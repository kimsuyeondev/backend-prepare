package com.api.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CalculateDiscountRequest {
    private Long userId;
    private Integer orderAmount;
    private List<Long> couponIds; // 사용할 쿠폰 ID 목록
}
