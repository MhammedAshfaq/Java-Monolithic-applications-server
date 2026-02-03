package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the user does not have permission to access a resource.
 */
public class ForbiddenException extends BaseException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ForbiddenException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.FORBIDDEN.value();
    }
}
