package com.toy.order.infrastructure.idempotency;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka 이벤트 처리 이력 테이블.
 *
 * 멱등성 키: eventId (OutboxEvent의 UUID = 이벤트 발생 단위의 식별자)
 *
 * [aggregateId를 키로 쓰면 안 되는 이유]
 * aggregateId(orderId, productId 등)는 비즈니스 엔티티 식별자다.
 * 같은 엔티티에 대해 동일 토픽 이벤트가 여러 번 정상 발생할 수 있다.
 * (예: 같은 상품에 대한 재입고 이벤트가 여러 번 → 두 번째부터 영원히 처리 불가)
 * eventId를 키로 쓰면 "이벤트 발생 자체"가 중복인지를 정확히 판별할 수 있다.
 */
@Entity
@Table(
    name = "processed_events",
    uniqueConstraints = @UniqueConstraint(columnNames = {"eventId"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Outbox UUID. 이벤트 발생 1건을 고유하게 식별하는 멱등성 키. */
    @Column(nullable = false)
    private String eventId;

    /** 감사(audit) 목적으로 보관. 멱등성 판단에는 사용하지 않는다. */
    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
        this.processedAt = LocalDateTime.now();
    }
}
