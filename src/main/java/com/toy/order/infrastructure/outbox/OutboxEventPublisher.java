package com.toy.order.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 이벤트 1건을 독립 트랜잭션으로 Kafka에 발행한다.
 *
 * [문제 1 - 부분 롤백]
 * 스케줄러가 @Transactional 하나로 10건을 처리하면, 7번째 실패 시 DB는 롤백되지만
 * Kafka에는 이미 6건이 발행된 상태가 된다. 다음 폴링에서 6건이 중복 발행된다.
 * → REQUIRES_NEW로 이벤트마다 트랜잭션을 분리하여, 성공한 이벤트는 즉시 커밋한다.
 *
 * [문제 2 - Poison Pill]
 * 특정 이벤트가 매번 실패하면 createdAt 순으로 항상 상위에 포함되어
 * 뒤에 있는 정상 이벤트가 영원히 발행되지 않는다.
 * → 실패한 이벤트를 FAILED로 마킹하여 이후 폴링 대상에서 제외한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxJpaRepository outboxJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 이벤트 ID로 DB에서 재조회하여 Kafka에 발행한다.
     * REQUIRES_NEW: 호출자(스케줄러)의 트랜잭션과 완전히 분리된 독립 트랜잭션으로 실행된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(String outboxEventId) {
        // findById도 try 안에 포함: 조회 실패 시에도 FAILED 마킹이 가능해야 한다.
        // try 밖에 두면 예외가 스케줄러까지 전파되고 이벤트는 PENDING으로 남아
        // 다음 폴링에서 계속 같은 오류를 반복하는 무한 루프가 발생한다.
        OutboxEvent event = null;
        try {
            event = outboxJpaRepository.findById(outboxEventId)
                    .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + outboxEventId));

            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
            event.markPublished();
            log.debug("Outbox event published: id={}, topic={}", event.getId(), event.getTopic());
        } catch (Exception e) {
            if (event != null) {
                // FAILED 마킹 후 트랜잭션 커밋 → 해당 이벤트는 다음 폴링에서 제외됨 (poison pill 방지)
                event.markFailed();
                log.error("Failed to publish outbox event: id={}, topic={}. Marked as FAILED.",
                        event.getId(), event.getTopic(), e);
            } else {
                // 조회 자체가 실패한 경우 (DB 장애 등) - 마킹 불가, 로그만 남김
                log.error("Failed to load outbox event: id={}. Will retry on next poll.", outboxEventId, e);
            }
        }
    }
}
