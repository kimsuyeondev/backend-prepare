package com.api.coupon.service;

import com.api.coupon.domain.Coupon;
import com.api.coupon.dto.CalculateDiscountRequest;
import com.api.coupon.dto.CalculateDiscountResponse;
import com.api.coupon.dto.CouponResponse;
import com.api.coupon.dto.IssueCouponRequest;
import com.api.coupon.repository.CouponRepository;
import com.api.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 쿠폰 서비스 테스트
 *
 * CouponService 구현 후 테스트하세요!
 * CouponServiceAnswer는 정답 확인용입니다.
 */
@SpringBootTest(classes = com.platform.BackendPrepareApplication.class)
class CouponServiceTest {

    @Autowired
    //private CouponServiceAnswer couponService; // 정답 코드로 테스트
     private CouponService couponService; // 본인 코드 테스트 시 주석 해제

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Test
    @Transactional
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        // when
        CouponResponse response = couponService.issueCoupon(
                new IssueCouponRequest(userId, couponId)
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(couponId);
        assertThat(response.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("동일 쿠폰 중복 발급 방지")
    void issueCoupon_Duplicate_Fail() {
        // given
        Long userId = 2L;
        Long couponId = 1L;

        couponService.issueCoupon(new IssueCouponRequest(userId, couponId));

        // when & then
        assertThatThrownBy(() ->
                couponService.issueCoupon(new IssueCouponRequest(userId, couponId))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 발급받은 쿠폰입니다");
    }

    @Test
    @DisplayName("선착순 쿠폰 동시성 테스트 - 100명만 발급")
    void issueCoupon_Concurrency_Test() throws InterruptedException {
        // given: 쿠폰 100개 (couponId = 5)
        Long couponId = 5L;
        int threadCount = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 1000명이 동시에 발급 요청
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executor.submit(() -> {
                try {
                    couponService.issueCoupon(new IssueCouponRequest(userId, couponId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then: 정확히 100명만 성공
        System.out.println("\n========================================");
        System.out.println("선착순 쿠폰 동시성 테스트");
        System.out.println("========================================");
        System.out.println("총 요청: 1000명");
        System.out.println("성공: " + successCount.get() + "명");
        System.out.println("실패: " + failCount.get() + "명");

        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        System.out.println("최종 발급 수량: " + coupon.getIssuedQuantity());
        System.out.println("========================================\n");

        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isEqualTo(900);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(100);
    }

    @Test
    @Transactional
    @DisplayName("할인 금액 계산 - 정액 쿠폰")
    void calculateDiscount_Fixed() {
        // given
        Long userId = 100L;
        Long couponId = 1L; // 3000원 할인 (최소 15000원)

        couponService.issueCoupon(new IssueCouponRequest(userId, couponId));

        // when: 20000원 주문
        CalculateDiscountRequest request = new CalculateDiscountRequest(
                userId, 20000, List.of(couponId)
        );
        CalculateDiscountResponse response = couponService.calculateDiscount(request);

        // then
        assertThat(response.getOriginalAmount()).isEqualTo(20000);
        assertThat(response.getTotalDiscount()).isEqualTo(3000);
        assertThat(response.getFinalAmount()).isEqualTo(17000);
        assertThat(response.getAppliedCoupons()).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("할인 금액 계산 - 정률 쿠폰")
    void calculateDiscount_Percentage() {
        // given
        Long userId = 101L;
        Long couponId = 3L; // 10% 할인 (최대 5000원, 최소 20000원)

        couponService.issueCoupon(new IssueCouponRequest(userId, couponId));

        // when: 50000원 주문
        CalculateDiscountRequest request = new CalculateDiscountRequest(
                userId, 50000, List.of(couponId)
        );
        CalculateDiscountResponse response = couponService.calculateDiscount(request);

        // then: 50000 * 10% = 5000원 (최대 5000원)
        assertThat(response.getOriginalAmount()).isEqualTo(50000);
        assertThat(response.getTotalDiscount()).isEqualTo(5000);
        assertThat(response.getFinalAmount()).isEqualTo(45000);
    }

    @Test
    @Transactional
    @DisplayName("할인 금액 계산 - 여러 쿠폰 조합")
    void calculateDiscount_Multiple_Coupons() {
        // given
        Long userId = 102L;

        // 3000원 할인 쿠폰 발급
        couponService.issueCoupon(new IssueCouponRequest(userId, 1L));

        // 10% 할인 쿠폰 발급 (최대 5000원)
        couponService.issueCoupon(new IssueCouponRequest(userId, 3L));

        // when: 50000원 주문에 두 쿠폰 사용
        CalculateDiscountRequest request = new CalculateDiscountRequest(
                userId, 50000, List.of(1L, 3L)
        );
        CalculateDiscountResponse response = couponService.calculateDiscount(request);

        // then: 5000 + 3000 = 8000원 할인
        assertThat(response.getOriginalAmount()).isEqualTo(50000);
        assertThat(response.getTotalDiscount()).isEqualTo(8000);
        assertThat(response.getFinalAmount()).isEqualTo(42000);
        assertThat(response.getAppliedCoupons()).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("보유하지 않은 쿠폰 사용 시 실패")
    void calculateDiscount_NotOwned_Fail() {
        // given
        Long userId = 103L;
        Long couponId = 1L;

        // when & then: 발급받지 않은 쿠폰 사용
        CalculateDiscountRequest request = new CalculateDiscountRequest(
                userId, 20000, List.of(couponId)
        );

        assertThatThrownBy(() -> couponService.calculateDiscount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("보유하지 않은 쿠폰입니다");
    }

    @Test
    @Transactional
    @DisplayName("최소 주문 금액 미달 시 실패")
    void calculateDiscount_MinAmount_Fail() {
        // given
        Long userId = 104L;
        Long couponId = 1L; // 최소 주문 15000원

        couponService.issueCoupon(new IssueCouponRequest(userId, couponId));

        // when & then: 10000원만 주문 (최소 금액 미달)
        CalculateDiscountRequest request = new CalculateDiscountRequest(
                userId, 10000, List.of(couponId)
        );

        assertThatThrownBy(() -> couponService.calculateDiscount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 주문 금액을 만족하지 않습니다");
    }
}
