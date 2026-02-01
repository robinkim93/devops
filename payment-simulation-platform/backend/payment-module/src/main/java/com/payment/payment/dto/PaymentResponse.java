package com.payment.payment.dto;

import com.payment.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private Payment.PaymentStatus status;
    private String orderId;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
