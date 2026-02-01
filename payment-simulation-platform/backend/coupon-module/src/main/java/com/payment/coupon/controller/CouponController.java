package com.payment.coupon.controller;

import com.payment.coupon.dto.CouponRequest;
import com.payment.coupon.dto.CouponResponse;
import com.payment.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;
    
    @PostMapping("/issue")
    public ResponseEntity<CouponResponse> issueCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponService.issueCoupon(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CouponResponse>> getUserCoupons(@PathVariable("userId") Long userId) {
        List<CouponResponse> coupons = couponService.getUserCoupons(userId);
        return ResponseEntity.ok(coupons);
    }
    
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<CouponResponse>> getActiveCoupons(@PathVariable("userId") Long userId) {
        List<CouponResponse> coupons = couponService.getActiveCoupons(userId);
        return ResponseEntity.ok(coupons);
    }
    
    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponResponse> useCoupon(
            @PathVariable("couponId") Long couponId,
            @RequestParam("userId") Long userId) {
        CouponResponse response = couponService.useCoupon(couponId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Coupon Service is running");
    }
}
