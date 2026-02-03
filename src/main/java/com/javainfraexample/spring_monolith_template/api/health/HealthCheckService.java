package com.javainfraexample.spring_monolith_template.api.health;

import org.springframework.stereotype.Service;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;

import lombok.RequiredArgsConstructor;

/**
 * Service for health check operations. All health-related logic is kept in the api/health package.
 */
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private static final String HEALTH_MESSAGE = "Service is healthy";
    private static final String HEALTH_STATUS = "OK";

    /**
     * Performs health check and returns the status payload.
     */
    public ApiResponseDto<String> check() {
        return ApiResponseDto.success(HEALTH_MESSAGE, HEALTH_STATUS);
    }
}
