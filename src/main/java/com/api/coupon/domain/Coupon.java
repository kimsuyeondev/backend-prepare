package com.api.coupon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 쿠폰 엔티티
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(nullable = false)
    private Integer discountValue; // 할인 금액 or 할인율

    @Column(nullable = false)
    private Integer minOrderAmount; // 최소 주문 금액

    @Column
    private Integer maxDiscountAmount; // 최대 할인 금액 (정률 쿠폰용)

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer totalQuantity; // 총 발급 가능 수량

    @Column(nullable = false)
    private Integer issuedQuantity; // 이미 발급된 수량

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    public boolean canIssue() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate)
                && now.isBefore(endDate)
                && issuedQuantity < totalQuantity;
    }

    /**
     * 쿠폰 발급
     */
    public void issue() {
        if (!canIssue()) {
            throw new IllegalStateException("쿠폰 발급이 불가능합니다.");
        }
        this.issuedQuantity++;
    }

    /**
     * 할인 금액 계산
     */
    public int calculateDiscount(int orderAmount) {
        if (type == CouponType.FIXED) {
            // 정액 할인
            return Math.min(discountValue, orderAmount);
        } else {
            // 정률 할인
            int discount = (int) (orderAmount * discountValue / 100.0);
            if (maxDiscountAmount != null) {
                discount = Math.min(discount, maxDiscountAmount);
            }
            return discount;
        }
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startDate) && now.isBefore(endDate);
    }

    public boolean isMinOrderAmount(int orderAmount) {
        return orderAmount >= minOrderAmount;
    }

    public void validateForDiscount(Integer orderAmount) {
        if(!isValid()) {
            throw new IllegalStateException("쿠폰이 사용 불가합니다.");
        }
        if (!isMinOrderAmount(orderAmount)) {
            throw new IllegalArgumentException("최소 주문 금액을 만족하지 않습니다");
        }
    }
}
