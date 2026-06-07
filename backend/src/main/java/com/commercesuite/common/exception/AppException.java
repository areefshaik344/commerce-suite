package com.commercesuite.common.exception;

import com.commercesuite.common.api.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;
    public AppException(ErrorCode code, HttpStatus status, String message) {
        super(message); this.code = code; this.status = status;
    }
    public static AppException notFound(String m)            { return new AppException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, m); }
    public static AppException conflict(ErrorCode c, String m){ return new AppException(c, HttpStatus.CONFLICT, m); }
    public static AppException forbidden(String m)           { return new AppException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, m); }
    public static AppException unauthorized(ErrorCode c, String m){ return new AppException(c, HttpStatus.UNAUTHORIZED, m); }
    public static AppException badRequest(ErrorCode c, String m){ return new AppException(c, HttpStatus.BAD_REQUEST, m); }
}
