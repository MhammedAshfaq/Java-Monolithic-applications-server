package com.javainfraexample.spring_monolith_template.api.auth.dto;

/**
 * Login response DTO with JWT token.
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    public LoginResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
    
    public LoginResponse(String accessToken, long expiresIn) {
        this(accessToken, null, "Bearer", expiresIn);
    }
}
