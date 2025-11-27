package com.platform.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer price;

    private Integer stock;

    @Version
    private Long version; // Optimistic Lock용

    @Builder
    public Menu(String name, Integer price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /**
     * 재고 감소 (동시성 처리 필요)
     * @param quantity 주문 수량
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException(
                String.format("재고가 부족합니다. (요청: %d, 재고: %d)", quantity, this.stock)
            );
        }
        this.stock -= quantity;
    }

    /**
     * 재고 증가 (취소/환불시)
     */
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
