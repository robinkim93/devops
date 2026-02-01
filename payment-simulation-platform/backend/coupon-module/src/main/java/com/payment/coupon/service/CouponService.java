package com.payment.coupon.service;

import com.payment.coupon.dto.CouponRequest;
import com.payment.coupon.dto.CouponResponse;
import com.payment.coupon.entity.Coupon;
import com.payment.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {
    
    private final CouponRepository couponRepository;
    
    @Transactional
    public CouponResponse issueCoupon(CouponRequest request) {
        // 쿠폰 타입에 따른 할인 정보 설정
        Integer discountAmount = null;
        Integer discountPercent = null;
        
        switch (request.getCouponType()) {
            case "PAYMENT_REWARD":
                discountAmount = 1000; // 1000원 할인
                break;
            case "WELCOME":
                discountPercent = 10; // 10% 할인
                break;
            case "BIRTHDAY":
                discountAmount = 5000; // 5000원 할인
                break;
            default:
                discountAmount = 1000;
        }
        
        Coupon coupon = Coupon.builder()
                .userId(request.getUserId())
                .couponType(request.getCouponType())
                .status(Coupon.CouponStatus.ACTIVE)
                .discountAmount(discountAmount)
                .discountPercent(discountPercent)
                .reason(request.getReason())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        
        coupon = couponRepository.save(coupon);
        
        return CouponResponse.builder()
                .success(true)
                .userId(coupon.getUserId())
                .couponId(coupon.getId())
                .couponType(coupon.getCouponType())
                .status(coupon.getStatus())
                .discountAmount(coupon.getDiscountAmount())
                .discountPercent(coupon.getDiscountPercent())
                .expiresAt(coupon.getExpiresAt())
                .message("Coupon issued successfully")
                .build();
    }
    
    public List<CouponResponse> getUserCoupons(Long userId) {
        return couponRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public List<CouponResponse> getActiveCoupons(Long userId) {
        return couponRepository.findByUserIdAndStatus(userId, Coupon.CouponStatus.ACTIVE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CouponResponse useCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        
        if (!coupon.getUserId().equals(userId)) {
            throw new RuntimeException("Coupon does not belong to user");
        }
        
        if (coupon.getStatus() != Coupon.CouponStatus.ACTIVE) {
            throw new RuntimeException("Coupon is not active");
        }
        
        if (coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            coupon.setStatus(Coupon.CouponStatus.EXPIRED);
            couponRepository.save(coupon);
            throw new RuntimeException("Coupon has expired");
        }
        
        coupon.setStatus(Coupon.CouponStatus.USED);
        coupon.setUsedAt(LocalDateTime.now());
        coupon = couponRepository.save(coupon);
        
        return toResponse(coupon);
    }
    
    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .success(true)
                .userId(coupon.getUserId())
                .couponId(coupon.getId())
                .couponType(coupon.getCouponType())
                .status(coupon.getStatus())
                .discountAmount(coupon.getDiscountAmount())
                .discountPercent(coupon.getDiscountPercent())
                .expiresAt(coupon.getExpiresAt())
                .message("Success")
                .build();
    }
}
