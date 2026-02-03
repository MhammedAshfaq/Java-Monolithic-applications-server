package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a bad request is made (e.g., invalid input, validation errors).
 */
public class BadRequestException extends BaseException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public BadRequestException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
