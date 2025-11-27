package com.api.coupon.domain;

import java.util.ArrayList;
import java.util.List;

public class Coupons {

    private final List<Coupon> coupons;

    public Coupons(List<Coupon> coupons) {
        this.coupons = List.copyOf(coupons);
    }

    public DiscountResult calculateDiscount(Integer orderAmount) {
        int totalDiscount = 0;
        List<CouponDiscountInfo> appliedCoupons = new ArrayList<>();

        for (Coupon coupon : coupons) {
            coupon.validateForDiscount(orderAmount);

            //최소 주문 금액 조건 및 할인 금액
            int discountAmount = coupon.calculateDiscount(orderAmount);
            totalDiscount += discountAmount;

            appliedCoupons.add(CouponDiscountInfo.builder()
                    .couponId(coupon.getId())
                    .discountAmount(discountAmount)
                    .couponName(coupon.getName())
                    .build());
        }

        // 총 할인 금액이 주문 금액을 초과하지 않도록 조정
        totalDiscount = Math.min(totalDiscount, orderAmount);  // 할인이 주문 금액 초과 방지
        int finalAmount = orderAmount - totalDiscount;


        return DiscountResult.builder()
                .totalDiscount(totalDiscount)
                .appliedCoupons(appliedCoupons)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount).build();
    }


}
