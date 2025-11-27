package com.api.coupon.dto;

import com.api.coupon.domain.Coupon;
import com.api.coupon.domain.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String name;
    private CouponType type;
    private Integer discountValue;
    private Integer minOrderAmount;
    private Integer maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer remainingQuantity;

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .remainingQuantity(coupon.getTotalQuantity() - coupon.getIssuedQuantity())
                .build();
    }
}
