package com.api.coupon.service;

import com.api.coupon.domain.Coupon;
import com.api.coupon.domain.UserCoupon;
import com.api.coupon.dto.CalculateDiscountRequest;
import com.api.coupon.dto.CalculateDiscountResponse;
import com.api.coupon.dto.CouponResponse;
import com.api.coupon.dto.IssueCouponRequest;
import com.api.coupon.repository.CouponRepository;
import com.api.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 쿠폰 서비스 정답 코드
 */
@Service
@RequiredArgsConstructor
public class CouponServiceAnswer {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 발급 API 구현 (정답)
     */
    @Transactional
    public CouponResponse issueCoupon(IssueCouponRequest request) {
        // 1. Pessimistic Lock으로 쿠폰 조회 (동시성 제어)
        Coupon coupon = couponRepository.findByIdWithPessmisticLock(request.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다"));

        // 2. 발급 가능 여부 확인 (기간, 수량)
        if (!coupon.canIssue()) {
            throw new IllegalStateException("쿠폰 발급이 불가능합니다");
        }

        // 3. 중복 발급 방지 - 이미 발급받은 쿠폰인지 확인
        boolean alreadyIssued = userCouponRepository
                .findByUserIdAndCouponIdAndUsed(request.getUserId(), request.getCouponId(), false)
                .isPresent();

        if (alreadyIssued) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다");
        }

        // 4. 쿠폰 발급 - issuedQuantity 증가
        coupon.issue();
        couponRepository.save(coupon);

        // 5. UserCoupon 생성 및 저장
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(request.getUserId())
                .couponId(request.getCouponId())
                .issuedAt(LocalDateTime.now())
                .used(false)
                .build();

        userCouponRepository.save(userCoupon);

        return CouponResponse.from(coupon);
    }

    /**
     * 할인 금액 계산 API 구현 (정답)
     */
    @Transactional(readOnly = true)
    public CalculateDiscountResponse calculateDiscount(CalculateDiscountRequest request) {
        List<CalculateDiscountResponse.AppliedCoupon> appliedCoupons = new ArrayList<>();
        int totalDiscount = 0;

        // 각 쿠폰에 대해 할인 금액 계산
        for (Long couponId : request.getCouponIds()) {
            // 1. 사용자가 보유한 쿠폰인지 확인
            UserCoupon userCoupon = userCouponRepository
                    .findByUserIdAndCouponIdAndUsed(request.getUserId(), couponId, false)
                    .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 쿠폰입니다: " + couponId));

            // 2. 이미 사용한 쿠폰인지 확인
            if (userCoupon.getUsed()) {
                throw new IllegalStateException("이미 사용한 쿠폰입니다: " + couponId);
            }

            // 3. 쿠폰 정보 조회
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다: " + couponId));

            // 4. 쿠폰 유효기간 확인
            if (!coupon.isValid()) {
                throw new IllegalStateException("쿠폰 사용 기간이 아닙니다: " + coupon.getName());
            }

            // 5. 최소 주문 금액 확인
            if (request.getOrderAmount() < coupon.getMinOrderAmount()) {
                throw new IllegalArgumentException(
                        "최소 주문 금액을 만족하지 않습니다. 필요: " + coupon.getMinOrderAmount() + "원");
            }

            // 6. 할인 금액 계산
            int discount = coupon.calculateDiscount(request.getOrderAmount());
            totalDiscount += discount;

            // 7. 적용된 쿠폰 정보 저장
            appliedCoupons.add(CalculateDiscountResponse.AppliedCoupon.builder()
                    .couponId(couponId)
                    .couponName(coupon.getName())
                    .discountAmount(discount)
                    .build());
        }

        // 8. 총 할인 금액이 주문 금액을 초과하지 않도록 조정
        if (totalDiscount > request.getOrderAmount()) {
            totalDiscount = request.getOrderAmount();
        }

        // 9. 최종 결제 금액 계산 (음수 방지)
        int finalAmount = Math.max(0, request.getOrderAmount() - totalDiscount);

        return CalculateDiscountResponse.builder()
                .originalAmount(request.getOrderAmount())
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .appliedCoupons(appliedCoupons)
                .build();
    }

    /**
     * 사용 가능한 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CouponResponse> getAvailableCoupons() {
        return couponRepository.findAll().stream()
                .filter(Coupon::canIssue)
                .map(CouponResponse::from)
                .toList();
    }

    /**
     * 사용자가 보유한 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CouponResponse> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndUsed(userId, false);

        List<CouponResponse> result = new ArrayList<>();
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
            if (coupon.isValid()) {
                result.add(CouponResponse.from(coupon));
            }
        }
        return result;
    }
}
