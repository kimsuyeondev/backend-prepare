package com.api.coupon.controller;

import com.api.coupon.dto.CalculateDiscountRequest;
import com.api.coupon.dto.CalculateDiscountResponse;
import com.api.coupon.dto.CouponResponse;
import com.api.coupon.dto.IssueCouponRequest;
import com.api.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 쿠폰 발급
     */
    @PostMapping("/issue")
    public ResponseEntity<CouponResponse> issueCoupon(@RequestBody IssueCouponRequest request) {
        CouponResponse response = couponService.issueCoupon(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 할인 금액 계산
     */
    @PostMapping("/calculate")
    public ResponseEntity<CalculateDiscountResponse> calculateDiscount(
            @RequestBody CalculateDiscountRequest request) {
        CalculateDiscountResponse response = couponService.calculateDiscount(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 발급 가능한 쿠폰 목록 조회
     */
    @GetMapping("/available")
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons() {
        List<CouponResponse> coupons = couponService.getAvailableCoupons();
        return ResponseEntity.ok(coupons);
    }

    /**
     * 사용자가 보유한 쿠폰 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CouponResponse>> getUserCoupons(@PathVariable Long userId) {
        List<CouponResponse> coupons = couponService.getUserCoupons(userId);
        return ResponseEntity.ok(coupons);
    }
}
