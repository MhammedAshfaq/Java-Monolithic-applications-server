package com.javainfraexample.spring_monolith_template.common.exception;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;
import com.javainfraexample.spring_monolith_template.common.dto.ErrorDetails;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * Handles all exceptions and returns standardized API responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles custom BaseException and its subclasses
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleBaseException(BaseException ex) {
        logger.error("BaseException occurred: {}", ex.getMessage(), ex);
        
        ErrorDetails errorDetails = ErrorDetails.of(
            ex.getErrorCode() != null ? ex.getErrorCode() : "ERROR",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(response);
    }
    
    /**
     * Handles ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(
            ex.getErrorCode() != null ? ex.getErrorCode() : "RESOURCE_NOT_FOUND",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }
    
    /**
     * Handles BadRequestException
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleBadRequestException(BadRequestException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(
            ex.getErrorCode() != null ? ex.getErrorCode() : "BAD_REQUEST",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handles UnauthorizedException
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("Unauthorized: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(
            ex.getErrorCode() != null ? ex.getErrorCode() : "UNAUTHORIZED",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(response);
    }
    
    /**
     * Handles ForbiddenException
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleForbiddenException(ForbiddenException ex) {
        logger.warn("Forbidden: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(
            ex.getErrorCode() != null ? ex.getErrorCode() : "FORBIDDEN",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(response);
    }
    
    /**
     * Handles ConstraintViolationException (for @Valid on path/query parameters)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.add(field + ": " + message);
            details.put(field, message);
        }
        
        ErrorDetails errorDetails = ErrorDetails.of(
            "VALIDATION_ERROR",
            "Validation failed",
            null,
            details
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(
            "Validation failed: " + String.join(", ", errors),
            errorDetails
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handles MethodArgumentNotValidException (for @Valid on request body)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        logger.warn("Method argument not valid: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String message = error.getDefaultMessage();
            errors.add(field + ": " + message);
            details.put(field, message);
        }
        
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            String message = error.getDefaultMessage();
            errors.add(message);
            details.put(error.getObjectName(), message);
        }
        
        ErrorDetails errorDetails = ErrorDetails.of(
            "VALIDATION_ERROR",
            "Validation failed",
            null,
            details
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(
            "Validation failed: " + String.join(", ", errors),
            errorDetails
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handles IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorDetails errorDetails = ErrorDetails.of(
            "ILLEGAL_ARGUMENT",
            ex.getMessage()
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(ex.getMessage(), errorDetails);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * Handles all other unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorDetails errorDetails = ErrorDetails.of(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support."
        );
        
        ApiResponseDto<Object> response = ApiResponseDto.error(
            "An unexpected error occurred",
            errorDetails
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
