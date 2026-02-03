package com.javainfraexample.spring_monolith_template.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Error details for API error responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {
    
    private String code;
    
    private String message;
    
    private String field;
    
    private Map<String, Object> details;
    
    /**
     * Creates error details with code and message
     */
    public static ErrorDetails of(String code, String message) {
        return ErrorDetails.builder()
            .code(code)
            .message(message)
            .build();
    }
    
    /**
     * Creates error details with code, message, and field
     */
    public static ErrorDetails of(String code, String message, String field) {
        return ErrorDetails.builder()
            .code(code)
            .message(message)
            .field(field)
            .build();
    }
    
    /**
     * Creates error details with all fields
     */
    public static ErrorDetails of(String code, String message, String field, Map<String, Object> details) {
        return ErrorDetails.builder()
            .code(code)
            .message(message)
            .field(field)
            .details(details)
            .build();
    }
}
