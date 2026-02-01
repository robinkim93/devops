package com.payment.point.service;

import com.payment.point.dto.*;
import com.payment.point.entity.PointBalance;
import com.payment.point.entity.PointTransaction;
import com.payment.point.repository.PointBalanceRepository;
import com.payment.point.repository.PointTransactionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class PointService {
    
    private final PointBalanceRepository balanceRepository;
    private final PointTransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    public PointService(PointBalanceRepository balanceRepository,
                       PointTransactionRepository transactionRepository,
                       @Qualifier("pointRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
    }
    
    @Transactional
    public PointResponse addPoint(PointRequest request) {
        // Redis 분산 락으로 동시성 제어
        String lockKey = "point:lock:" + request.getUserId();
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            return PointResponse.builder()
                    .success(false)
                    .userId(request.getUserId())
                    .message("Another transaction is in progress")
                    .build();
        }
        
        try {
            PointBalance balance = balanceRepository.findByUserId(request.getUserId())
                    .orElse(PointBalance.builder()
                            .userId(request.getUserId())
                            .balance(BigDecimal.ZERO)
                            .build());
            
            BigDecimal newBalance = balance.getBalance().add(request.getAmount());
            balance.setBalance(newBalance);
            balance = balanceRepository.save(balance);
            
            // 트랜잭션 기록
            PointTransaction transaction = PointTransaction.builder()
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .type(PointTransaction.TransactionType.EARN)
                    .balanceAfter(newBalance)
                    .reason(request.getReason())
                    .build();
            transactionRepository.save(transaction);
            
            // 캐시 업데이트
            redisTemplate.opsForValue().set(
                    "point:balance:" + request.getUserId(),
                    newBalance.toString(),
                    1,
                    TimeUnit.HOURS
            );
            
            return PointResponse.builder()
                    .success(true)
                    .userId(request.getUserId())
                    .currentBalance(newBalance)
                    .message("Points added successfully")
                    .build();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    @Transactional
    public PointResponse usePoint(UsePointRequest request) {
        String lockKey = "point:lock:" + request.getUserId();
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            return PointResponse.builder()
                    .success(false)
                    .userId(request.getUserId())
                    .message("Another transaction is in progress")
                    .build();
        }
        
        try {
            PointBalance balance = balanceRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Point balance not found"));
            
            if (balance.getBalance().compareTo(request.getAmount()) < 0) {
                return PointResponse.builder()
                        .success(false)
                        .userId(request.getUserId())
                        .currentBalance(balance.getBalance())
                        .message("Insufficient points")
                        .build();
            }
            
            BigDecimal newBalance = balance.getBalance().subtract(request.getAmount());
            balance.setBalance(newBalance);
            balance = balanceRepository.save(balance);
            
            // 트랜잭션 기록
            PointTransaction transaction = PointTransaction.builder()
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .type(PointTransaction.TransactionType.USE)
                    .balanceAfter(newBalance)
                    .reason(request.getReason())
                    .build();
            transactionRepository.save(transaction);
            
            // 캐시 업데이트
            redisTemplate.opsForValue().set(
                    "point:balance:" + request.getUserId(),
                    newBalance.toString(),
                    1,
                    TimeUnit.HOURS
            );
            
            return PointResponse.builder()
                    .success(true)
                    .userId(request.getUserId())
                    .currentBalance(newBalance)
                    .message("Points used successfully")
                    .build();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    public BalanceResponse getBalance(Long userId) {
        // 캐시에서 먼저 조회
        String cachedBalance = redisTemplate.opsForValue().get("point:balance:" + userId);
        if (cachedBalance != null) {
            return BalanceResponse.builder()
                    .userId(userId)
                    .balance(new BigDecimal(cachedBalance))
                    .build();
        }
        
        // DB에서 조회
        PointBalance balance = balanceRepository.findByUserId(userId)
                .orElse(PointBalance.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build());
        
        // 캐시에 저장
        redisTemplate.opsForValue().set(
                "point:balance:" + userId,
                balance.getBalance().toString(),
                1,
                TimeUnit.HOURS
        );
        
        return BalanceResponse.builder()
                .userId(userId)
                .balance(balance.getBalance())
                .build();
    }
}
