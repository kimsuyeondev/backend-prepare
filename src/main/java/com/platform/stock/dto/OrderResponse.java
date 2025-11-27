package com.platform.stock.dto;

import com.platform.stock.domain.Order;
import com.platform.stock.domain.OrderItem;
import com.platform.stock.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private Long userId;
    private OrderStatus status;
    private Integer totalPrice;
    private List<OrderItemResponse> items;
    private LocalDateTime orderedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .status(order.getStatus())
            .totalPrice(order.getTotalPrice())
            .items(order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList()))
            .orderedAt(order.getOrderedAt())
            .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long menuId;
        private String menuName;
        private Integer quantity;
        private Integer price;
        private Integer totalPrice;

        public static OrderItemResponse from(OrderItem item) {
            return OrderItemResponse.builder()
                .menuId(item.getMenuId())
                .menuName(item.getMenuName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .build();
        }
    }
}
