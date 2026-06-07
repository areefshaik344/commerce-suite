package com.commercesuite.common.exception;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
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

    private static ResponseEntity<ApiResponse<ErrorBody>> build(HttpStatus status, ErrorCode code, String msg, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false,
                new ErrorBody(code.name(), msg, fields == null ? Map.of() : fields),
                msg, Instant.now()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleApp(AppException ex) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleCv(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleDup(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Data integrity violation", null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleBadCreds(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAuth(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Authentication required", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorBody>> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Internal server error", null);
    }
}
