package com.javainfraexample.spring_monolith_template.api.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;
import com.javainfraexample.spring_monolith_template.common.exception.BadRequestException;
import com.javainfraexample.spring_monolith_template.common.exception.ResourceNotFoundException;
import com.javainfraexample.spring_monolith_template.common.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Example controller demonstrating exception handling and response wrapping.
 * This controller can be removed or used as a reference for implementing other
 * controllers.
 */
@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
@Tag(name = "Example", description = "Example endpoints demonstrating exception handling")
public class ExampleController {

    @Operation(summary = "Get example resource", description = "Example endpoint that returns a resource. Throws ResourceNotFoundException if id is 'notfound'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<String>> getExample(
            @Parameter(description = "Resource ID") @PathVariable String id) {
        if ("notfound".equals(id)) {
            throw new ResourceNotFoundException("EXAMPLE_NOT_FOUND", "Example resource with id '%s' not found", id);
        }

        return ResponseUtil.success("Resource retrieved successfully", "Example data for id: " + id);
    }

    @Operation(summary = "Get example with validation", description = "Example endpoint that validates query parameter. Throws BadRequestException if 'error' parameter is provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/validate")
    public ResponseEntity<ApiResponseDto<String>> validateExample(
            @Parameter(description = "Query parameter") @RequestParam(required = false) String param) {
        if ("error".equals(param)) {
            throw new BadRequestException("VALIDATION_ERROR", "Invalid parameter value: %s", param);
        }

        return ResponseUtil.success("Validation passed", "Parameter value: " + param);
    }
}
