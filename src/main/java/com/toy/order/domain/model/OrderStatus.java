package com.toy.order.domain.model;

public enum OrderStatus {
    PENDING,       // 주문 대기 (재고 확인 중)
    CONFIRMED,     // 주문 확정 (재고 차감 완료)
    CANCELLED      // 주문 취소
}
