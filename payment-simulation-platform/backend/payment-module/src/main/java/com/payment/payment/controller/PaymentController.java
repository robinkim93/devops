package com.payment.payment.controller;

import com.payment.auth.dto.ValidateTokenResponse;
import com.payment.auth.service.AuthService;
import com.payment.payment.dto.*;
import com.payment.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final AuthService authService;
    
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody PaymentRequest request) {
        
        String token = authorization.replace("Bearer ", "");
        ValidateTokenResponse validation = authService.validateToken(token);
        
        if (!validation.isValid()) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            PaymentResponse response = paymentService.processPayment(
                    validation.getUserId(),
                    request
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable("id") Long id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user")
    public ResponseEntity<List<PaymentResponse>> getUserPayments(
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        ValidateTokenResponse validation = authService.validateToken(token);
        
        if (!validation.isValid()) {
            return ResponseEntity.status(401).build();
        }
        
        List<PaymentResponse> payments = paymentService.getUserPayments(validation.getUserId());
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running");
    }
}
