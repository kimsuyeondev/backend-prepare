package com.api.coupon.service;

import com.api.coupon.domain.Coupon;
import com.api.coupon.domain.Coupons;
import com.api.coupon.domain.DiscountResult;
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
 * 쿠폰 서비스
 * <p>
 * TODO: 아래 메서드들을 구현하세요!
 * <p>
 * 요구사항:
 * 1. 쿠폰 발급 API - 동시성 처리 필요
 * 2. 할인 금액 계산 API - 복잡한 비즈니스 로직
 * 3. 중복 쿠폰 적용 방지
 * 4. 쿠폰 조합 시 최대 할인 금액 제한
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponQueryService couponQueryService;

    /**
     * TODO 1: 쿠폰 발급 API 구현
     * <p>
     * 요구사항:
     * - 선착순 쿠폰이므로 동시성 처리 필요 (Pessimistic Lock 사용)
     * - 발급 가능 여부 검증 (기간, 수량)
     * - 사용자당 동일 쿠폰 중복 발급 방지
     * - 발급 후 issuedQuantity 증가
     * <p>
     * 예외 처리:
     * - 쿠폰이 존재하지 않으면 IllegalArgumentException
     * - 발급 불가능하면 IllegalStateException
     * - 이미 발급받은 쿠폰이면 IllegalStateException
     */
    @Transactional
    public CouponResponse issueCoupon(IssueCouponRequest request) {

        //쿠폰 조회
        Coupon coupon = couponRepository.findByIdWithPessmisticLock(request.getCouponId()).orElseThrow(
                () -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

        //쿠폰 발급 여부
        if (!coupon.canIssue()) {
            throw new IllegalStateException("쿠폰 발급이 불가합니다");
        }

        //중복 발급 여부
        boolean isAlreadyIssued =
                userCouponRepository.findByUserIdAndCouponIdAndUsed(request.getUserId(), coupon.getId(), false).isPresent();

        //쿠폰 발급
        if (isAlreadyIssued) {
            throw new IllegalStateException("이미 발급받은 쿠폰입니다");
        }

        coupon.issue();
        //     couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .couponId(coupon.getId())
                .userId(request.getUserId())
                .issuedAt(LocalDateTime.now())
                .used(false)
                .build();

        userCouponRepository.save(userCoupon);

        return CouponResponse.from(coupon);
    }

    /**
     * TODO 2: 할인 금액 계산 API 구현
     * <p>
     * 요구사항:
     * - 여러 쿠폰을 동시에 사용할 수 있음
     * - 쿠폰별로 할인 금액 계산 (정액/정률)
     * - 총 할인 금액은 주문 금액을 초과할 수 없음
     * - 최종 결제 금액은 0원 이상이어야 함
     * <p>
     * 비즈니스 로직:
     * 1. 사용자가 보유한 쿠폰인지 확인
     * 2. 쿠폰이 사용 가능한지 확인 (기간, 사용 여부)
     * 3. 최소 주문 금액 조건 확인
     * 4. 할인 금액 계산 (할인율이 높은 순서로 적용)
     * 5. 총 할인 금액이 주문 금액을 초과하지 않도록 조정
     * <p>
     * 예외 처리:
     * - 쿠폰을 보유하지 않았으면 IllegalArgumentException
     * - 이미 사용한 쿠폰이면 IllegalStateException
     * - 쿠폰 유효기간이 아니면 IllegalStateException
     */
    @Transactional(readOnly = true)
    public CalculateDiscountResponse calculateDiscount(CalculateDiscountRequest request) {
        // 쿠폰 조회, validate
        List<Coupon> couponList = couponQueryService.getUserOwnedCoupons(request.getUserId(), request.getCouponIds());

        // 할인 계산
        Coupons coupons = new Coupons(couponList);
        DiscountResult discountResult = coupons.calculateDiscount(request.getOrderAmount());

        return CalculateDiscountResponse.of(request.getOrderAmount(), discountResult);

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
