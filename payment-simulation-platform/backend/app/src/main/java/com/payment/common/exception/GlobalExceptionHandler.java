package com.payment.common.exception;

import com.payment.auth.exception.AuthException;
import com.payment.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException e, HttpServletRequest request) {
        log.error("AuthException: {}", e.getMessage());
        return createErrorResponse(e.getStatus(), e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation Error: {}", message);
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Runtime Exception: ", e);
        // 비즈니스 로직에서 던지는 일반적인 RuntimeException 처리
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e.getMessage() != null && e.getMessage().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        }
        return createErrorResponse(status, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception: ", e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
