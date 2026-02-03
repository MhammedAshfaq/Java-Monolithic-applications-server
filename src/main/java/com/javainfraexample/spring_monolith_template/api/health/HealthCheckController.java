package com.javainfraexample.spring_monolith_template.api.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @Operation(summary = "Health check endpoint", description = "Returns the health status of the application. Returns 'OK' if the service is running.")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping()
    public ResponseEntity<ApiResponseDto<String>> healthCheck() {
        return ResponseEntity.ok(healthCheckService.check());
    }
}
