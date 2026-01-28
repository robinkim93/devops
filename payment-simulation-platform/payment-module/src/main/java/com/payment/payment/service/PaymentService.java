package com.payment.payment.service;

import com.payment.coupon.dto.CouponRequest;
import com.payment.coupon.dto.CouponResponse;
import com.payment.coupon.service.CouponService;
import com.payment.point.dto.PointRequest;
import com.payment.point.dto.PointResponse;
import com.payment.point.dto.UsePointRequest;
import com.payment.point.service.PointService;
import com.payment.payment.dto.*;
import com.payment.payment.entity.Payment;
import com.payment.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final CouponService couponService;
    
    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        BigDecimal originalAmount = request.getAmount();
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = originalAmount;

        // 1. 쿠폰 적용
        if (request.getCouponId() != null) {
            CouponResponse coupon = couponService.useCoupon(request.getCouponId(), userId);
            if (coupon.isSuccess()) {
                if (coupon.getDiscountAmount() != null) {
                    discountAmount = discountAmount.add(new BigDecimal(coupon.getDiscountAmount()));
                } else if (coupon.getDiscountPercent() != null) {
                    BigDecimal percentDiscount = originalAmount.multiply(new BigDecimal(coupon.getDiscountPercent())).divide(new BigDecimal("100"));
                    discountAmount = discountAmount.add(percentDiscount);
                }
            }
        }

        // 2. 포인트 사용 적용
        if (request.getUsePoint() != null && request.getUsePoint().compareTo(BigDecimal.ZERO) > 0) {
            UsePointRequest usePointRequest = UsePointRequest.builder()
                    .userId(userId)
                    .amount(request.getUsePoint())
                    .reason("결제 사용: " + request.getOrderId())
                    .build();
            
            PointResponse pointResponse = pointService.usePoint(usePointRequest);
            if (!pointResponse.isSuccess()) {
                throw new RuntimeException("Point usage failed: " + pointResponse.getMessage());
            }
            discountAmount = discountAmount.add(request.getUsePoint());
        }

        finalAmount = finalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 결제 생성
        Payment payment = Payment.builder()
                .userId(userId)
                .amount(finalAmount)
                .orderId(request.getOrderId())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD")
                .status(Payment.PaymentStatus.PENDING)
                .build();
        
        payment = paymentRepository.save(payment);
        
        try {
            // 결제 성공 처리
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            payment = paymentRepository.saveAndFlush(payment);
            
            // 포인트 적립 (실제 결제 금액의 1%)
            BigDecimal earnPointAmount = finalAmount.multiply(new BigDecimal("0.01"));
            if (earnPointAmount.compareTo(BigDecimal.ZERO) > 0) {
                PointRequest pointRequest = PointRequest.builder()
                        .userId(userId)
                        .amount(earnPointAmount)
                        .reason("결제 적립: " + request.getOrderId())
                        .build();
                
                try {
                    pointService.addPoint(pointRequest);
                } catch (Exception pe) {
                    System.err.println("Point accumulation failed: " + pe.getMessage());
                }
            }
            
            // 쿠폰 발급 (실제 결제 금액이 10000원 이상인 경우)
            if (finalAmount.compareTo(new BigDecimal("10000")) >= 0) {
                CouponRequest couponRequest = CouponRequest.builder()
                        .userId(userId)
                        .couponType("PAYMENT_REWARD")
                        .reason("결제 보상: " + request.getOrderId())
                        .build();
                
                try {
                    couponService.issueCoupon(couponRequest);
                } catch (Exception ce) {
                    System.err.println("Coupon issuance failed: " + ce.getMessage());
                }
            }
            
            return toResponse(payment);
        } catch (Exception e) {
            // 결제 실패 처리
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw e;
        }
    }
    
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toResponse(payment);
    }
    
    public java.util.List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }
    
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .orderId(payment.getOrderId())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
}
