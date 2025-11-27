package com.platform.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다")
    @Valid
    private List<OrderItemRequest> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "메뉴 ID는 필수입니다")
        private Long menuId;

        @NotNull(message = "수량은 필수입니다")
        private Integer quantity;

        public void validate() {
            if (quantity <= 0) {
                throw new IllegalArgumentException("수량은 1개 이상이어야 합니다");
            }
        }
    }
}
