package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication is required or authentication fails.
 */
public class UnauthorizedException extends BaseException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public UnauthorizedException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
