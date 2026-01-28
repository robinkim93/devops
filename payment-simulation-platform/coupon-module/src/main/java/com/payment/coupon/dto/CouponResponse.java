package com.payment.coupon.dto;

import com.payment.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private boolean success;
    private Long userId;
    private Long couponId;
    private String couponType;
    private Coupon.CouponStatus status;
    private Integer discountAmount;
    private Integer discountPercent;
    private LocalDateTime expiresAt;
    private String message;
}
