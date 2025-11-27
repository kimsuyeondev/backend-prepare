package com.api.coupon.service;

import com.api.coupon.domain.Coupon;
import com.api.coupon.domain.Coupons;
import com.api.coupon.dto.CouponResponse;
import com.api.coupon.repository.CouponRepository;
import com.api.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 쿠폰 조회 전용 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService {


    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;


    public List<Coupon> getUserOwnedCoupons(Long userId, List<Long> couponIds) {
        //사용자 보유 쿠폰 여부
        List<Coupon> coupons = new ArrayList<>();
        for (long couponId : couponIds) {
            userCouponRepository.findByUserIdAndCouponIdAndUsed(userId, couponId, false).
                    orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다"));

            //쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

            coupons.add(coupon);
        }

        return coupons;
    }

}
