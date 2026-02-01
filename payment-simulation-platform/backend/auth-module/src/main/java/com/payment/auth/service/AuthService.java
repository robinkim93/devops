package com.payment.auth.service;

import com.payment.auth.dto.*;
import com.payment.auth.entity.User;
import com.payment.auth.exception.AuthException;
import com.payment.auth.repository.UserRepository;
import com.payment.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    
    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil,
                      @Qualifier("authRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("Email already exists", HttpStatus.BAD_REQUEST);
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        
        user = userRepository.save(user);
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        // Redis에 토큰 저장 (1시간)
        redisTemplate.opsForValue().set(
                "token:" + user.getId(),
                token,
                1,
                TimeUnit.HOURS
        );
        
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        
        // Redis에 토큰 저장 (1시간)
        redisTemplate.opsForValue().set(
                "token:" + user.getId(),
                token,
                1,
                TimeUnit.HOURS
        );
        
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
    
    public ValidateTokenResponse validateToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                return ValidateTokenResponse.builder()
                        .valid(false)
                        .build();
            }
            
            Long userId = jwtUtil.getUserIdFromToken(token);
            Claims claims = jwtUtil.parseToken(token);
            String email = claims.get("email", String.class);
            
            // Redis에서 토큰 확인
            String storedToken = redisTemplate.opsForValue().get("token:" + userId);
            if (storedToken == null || !storedToken.equals(token)) {
                return ValidateTokenResponse.builder()
                        .valid(false)
                        .build();
            }
            
            return ValidateTokenResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .build();
        } catch (Exception e) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .build();
        }
    }
    
    public void logout(Long userId) {
        redisTemplate.delete("token:" + userId);
    }
}
