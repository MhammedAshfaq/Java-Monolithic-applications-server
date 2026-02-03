package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ResourceNotFoundException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.NOT_FOUND.value();
    }
}
