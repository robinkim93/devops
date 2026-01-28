package com.payment.point.controller;

import com.payment.point.dto.*;
import com.payment.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
    
    private final PointService pointService;
    
    @PostMapping("/add")
    public ResponseEntity<PointResponse> addPoint(@Valid @RequestBody PointRequest request) {
        PointResponse response = pointService.addPoint(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/use")
    public ResponseEntity<PointResponse> usePoint(@Valid @RequestBody UsePointRequest request) {
        PointResponse response = pointService.usePoint(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/balance/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable("userId") Long userId) {
        BalanceResponse response = pointService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Point Service is running");
    }
}
