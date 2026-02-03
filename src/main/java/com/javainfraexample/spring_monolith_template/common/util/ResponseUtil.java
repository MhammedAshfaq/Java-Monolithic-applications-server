package com.javainfraexample.spring_monolith_template.common.util;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for creating standardized API responses.
 */
public class ResponseUtil {
    
    /**
     * Creates a successful ResponseEntity with data
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> success(T data) {
        System.out.println("ResponseUtil.success(T data): " + data);
        return ResponseEntity.ok(ApiResponseDto.success(data));
    }
    
    /**
     * Creates a successful ResponseEntity with message and data
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> success(String message, T data) {
        System.out.println("ResponseUtil.success(String message, T data): " + message + " " + data);
        return ResponseEntity.ok(ApiResponseDto.success(message, data));
    }
    
    /**
     * Creates a successful ResponseEntity with only message
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> success(String message) {
        return ResponseEntity.ok(ApiResponseDto.success(message));
    }
    
    /**
     * Creates a successful ResponseEntity with custom HTTP status
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> success(HttpStatus status, T data) {
        return ResponseEntity.status(status).body(ApiResponseDto.success(data));
    }
    
    /**
     * Creates a successful ResponseEntity with custom HTTP status and message
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> success(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponseDto.success(message, data));
    }
    
    /**
     * Creates a created (201) ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success("Resource created successfully", data));
    }
    
    /**
     * Creates a created (201) ResponseEntity with custom message
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(message, data));
    }
    
    /**
     * Creates a no content (204) ResponseEntity
     */
    public static <T> ResponseEntity<ApiResponseDto<T>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
