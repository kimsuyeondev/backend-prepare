package com.platform.stock.domain;

public enum OrderStatus {
    ORDERED,      // 주문 접수
    COOKING,      // 조리중
    DELIVERING,   // 배달중
    DELIVERED,    // 배달 완료
    CANCELLED     // 취소됨
}
