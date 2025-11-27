package com.platform.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long menuId;
    private String menuName;
    private Integer quantity;
    private Integer price;

    @Builder
    public OrderItem(Long menuId, String menuName, Integer quantity, Integer price) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.quantity = quantity;
        this.price = price;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getTotalPrice() {
        return this.price * this.quantity;
    }
}
