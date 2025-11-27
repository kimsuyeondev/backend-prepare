package com.api.coupon.dto;

import com.api.coupon.domain.CouponDiscountInfo;
import com.api.coupon.domain.DiscountResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateDiscountResponse {
    private Integer originalAmount;     // 원래 주문 금액
    private Integer totalDiscount;      // 총 할인 금액
    private Integer finalAmount;        // 최종 결제 금액
    private List<AppliedCoupon> appliedCoupons; // 적용된 쿠폰 목록


    public static CalculateDiscountResponse of(int orderAmount, DiscountResult discountResult) {
        List<AppliedCoupon> appliedCouponList = discountResult.getAppliedCoupons().stream()
                .map(AppliedCoupon::from)
                .toList();

        return CalculateDiscountResponse.builder()
                .finalAmount(discountResult.getFinalAmount())
                .totalDiscount(discountResult.getTotalDiscount())
                .appliedCoupons(appliedCouponList)
                .originalAmount(orderAmount).build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedCoupon {
        private Long couponId;
        private String couponName;
        private Integer discountAmount;

        public static AppliedCoupon from(CouponDiscountInfo couponDiscountInfo) {
            return AppliedCoupon.builder()
                    .couponId(couponDiscountInfo.getCouponId())
                    .couponName(couponDiscountInfo.getCouponName())
                    .discountAmount(couponDiscountInfo.getDiscountAmount())
                    .build();
        }
    }


}
