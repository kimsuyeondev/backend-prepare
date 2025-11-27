package com.platform.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderedAt;

    @Builder
    public Order(Long userId, List<OrderItem> items) {
        this.userId = userId;
        this.items = items;
        this.totalPrice = calculateTotalPrice();
        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();

        // 양방향 연관관계 설정
        items.forEach(item -> item.setOrder(this));
    }

    private Integer calculateTotalPrice() {
        return items.stream()
            .mapToInt(OrderItem::getTotalPrice)
            .sum();
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        item.setOrder(this);
        this.totalPrice = calculateTotalPrice();
    }
}
