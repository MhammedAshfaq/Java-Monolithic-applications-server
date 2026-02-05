package com.javainfraexample.spring_monolith_template.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javainfraexample.spring_monolith_template.api.auth.dto.LoginRequest;
import com.javainfraexample.spring_monolith_template.api.auth.dto.LoginResponse;
import com.javainfraexample.spring_monolith_template.api.auth.dto.RefreshTokenRequest;
import com.javainfraexample.spring_monolith_template.api.auth.dto.RegisterRequest;
import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;
import com.javainfraexample.spring_monolith_template.common.ratelimit.RateLimit;
import com.javainfraexample.spring_monolith_template.common.ratelimit.RateLimitType;
import com.javainfraexample.spring_monolith_template.services.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Authenticate user and return JWT tokens")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Too many login attempts")
    @RateLimit(type = RateLimitType.STRICT, key = "login")
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Register", description = "Create a new user account")
    @ApiResponse(responseCode = "201", description = "Registration successful")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @RateLimit(type = RateLimitType.STRICT, key = "register")
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed")
    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    @RateLimit(type = RateLimitType.SHORT_TERM)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
