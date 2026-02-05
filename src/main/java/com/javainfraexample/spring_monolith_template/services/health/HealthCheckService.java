package com.javainfraexample.spring_monolith_template.services.health;

import org.springframework.stereotype.Service;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private static final String HEALTH_MESSAGE = "Service is healthy";
    private static final String HEALTH_STATUS = "OK";

    /**
     * Performs health check and returns the status payload.
     */
    public ApiResponseDto<String> check (){
        return ApiResponseDto.success(HEALTH_MESSAGE, HEALTH_STATUS);

    }
    
}
