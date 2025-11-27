package com.api.coupon.domain;


import com.api.coupon.dto.CalculateDiscountResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResult {
    private Integer originalAmount;     // 원래 주문 금액
    private Integer totalDiscount;      // 총 할인 금액
    private Integer finalAmount;        // 최종 결제 금액
    private List<CouponDiscountInfo> appliedCoupons; // 적용된 쿠폰 목록


}
