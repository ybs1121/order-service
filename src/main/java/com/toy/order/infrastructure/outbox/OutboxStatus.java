package com.toy.order.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    /** Kafka 발행 중 예외 발생. 폴링 대상에서 제외되어 poison pill 현상을 방지한다. */
    FAILED
}
