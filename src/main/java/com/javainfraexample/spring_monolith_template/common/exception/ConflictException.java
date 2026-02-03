package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a conflict occurs (e.g., duplicate resource, concurrent modification).
 */
public class ConflictException extends BaseException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ConflictException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.CONFLICT.value();
    }
}
