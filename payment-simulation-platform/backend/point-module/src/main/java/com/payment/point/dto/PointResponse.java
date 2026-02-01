package com.payment.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointResponse {
    private boolean success;
    private Long userId;
    private BigDecimal currentBalance;
    private String message;
}
