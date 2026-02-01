package com.payment.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Coupon type is required")
    private String couponType;
    
    private String reason;
}
