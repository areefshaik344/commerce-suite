package com.commercesuite.common.exception;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(String code, String message, Map<String, String> fieldErrors) {}

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleApp(AppException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(ex.getMessage()) instanceof ApiResponse<?>
                        ? new ApiResponse<>(false, new ErrorBody(ex.getCode().name(), ex.getMessage(), Map.of()),
                                            ex.getMessage(), java.time.Instant.now())
                        : null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        return ResponseEntity.badRequest().body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.VALIDATION_FAILED.name(), "Validation failed", fields),
                "Validation failed", java.time.Instant.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleCv(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.VALIDATION_FAILED.name(), ex.getMessage(), Map.of()),
                "Validation failed", java.time.Instant.now()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleDup(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.CONFLICT.name(), "Data integrity violation", Map.of()),
                "Conflict", java.time.Instant.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleBadCreds(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.INVALID_CREDENTIALS.name(), "Invalid credentials", Map.of()),
                "Invalid credentials", java.time.Instant.now()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.UNAUTHENTICATED.name(), "Authentication required", Map.of()),
                "Unauthenticated", java.time.Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.FORBIDDEN.name(), "Access denied", Map.of()),
                "Forbidden", java.time.Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false,
                new ErrorBody(ErrorCode.INTERNAL_ERROR.name(), "Internal server error", Map.of()),
                "Internal error", java.time.Instant.now()));
    }
}
