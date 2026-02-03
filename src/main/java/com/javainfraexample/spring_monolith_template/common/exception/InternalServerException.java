package com.javainfraexample.spring_monolith_template.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for internal server errors.
 */
public class InternalServerException extends BaseException {
    
    public InternalServerException(String message) {
        super(message);
    }
    
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InternalServerException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public InternalServerException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    @Override
    public int getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
