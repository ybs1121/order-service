package com.toy.order.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PENDING 상태의 Outbox 이벤트를 주기적으로 폴링하여 OutboxEventPublisher에 위임한다.
 *
 * 스케줄러 자체는 @Transactional을 갖지 않는다.
 * 각 이벤트의 Kafka 발행과 상태 업데이트는 OutboxEventPublisher(REQUIRES_NEW)가
 * 이벤트별 독립 트랜잭션으로 처리하므로, 특정 이벤트의 실패가 다른 이벤트에 영향을 주지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        List<OutboxEvent> pending = outboxJpaRepository
                .findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            // 이벤트마다 독립 트랜잭션으로 발행 → 실패해도 나머지 이벤트 처리 계속
            outboxEventPublisher.publishOne(event.getId());
        }
    }
}
