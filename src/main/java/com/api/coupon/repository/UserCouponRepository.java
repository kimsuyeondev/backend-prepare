package com.api.coupon.repository;

import com.api.coupon.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 사용자의 특정 쿠폰 조회
     */
    Optional<UserCoupon> findByUserIdAndCouponIdAndUsed(Long userId, Long couponId, Boolean used);

    /**
     * 사용자의 모든 쿠폰 조회
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 사용자의 사용 가능한 쿠폰 조회
     */
    List<UserCoupon> findByUserIdAndUsed(Long userId, Boolean used);
}
