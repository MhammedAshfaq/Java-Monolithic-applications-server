package com.javainfraexample.spring_monolith_template.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token request DTO.
 */
public record RefreshTokenRequest(
    
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
