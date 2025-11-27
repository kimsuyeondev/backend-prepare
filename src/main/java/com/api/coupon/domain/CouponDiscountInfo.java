package com.api.coupon.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDiscountInfo {
    private Long couponId;
    private String couponName;
    private Integer discountAmount;
}
