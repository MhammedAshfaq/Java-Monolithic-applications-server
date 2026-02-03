package com.javainfraexample.spring_monolith_template.common.exception;

import lombok.Getter;

/**
 * Base exception class for all custom exceptions in the application.
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    
    protected BaseException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }
    
    protected BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }
    
    protected BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    protected BaseException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    protected BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    /**
     * Get the HTTP status code for this exception
     */
    public abstract int getHttpStatus();
}
