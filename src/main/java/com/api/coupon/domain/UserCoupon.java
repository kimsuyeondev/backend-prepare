package com.api.coupon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 보유한 쿠폰
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private LocalDateTime issuedAt; // 발급 시간

    @Column
    private LocalDateTime usedAt; // 사용 시간

    @Column(nullable = false)
    private Boolean used; // 사용 여부

    /**
     * 쿠폰 사용
     */
    public void use() {
        if (used) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 사용 가능한 쿠폰인지 확인
     */
    public boolean canUse() {
        return !used;
    }
}
