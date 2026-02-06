package com.javainfraexample.spring_monolith_template.api.user.dto;

import com.javainfraexample.spring_monolith_template.domain.user.UserRole;
import com.javainfraexample.spring_monolith_template.domain.user.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user.
 * All fields are optional - only provided fields will be updated.
 */
public record UpdateUserRequest(
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Email(message = "Invalid email format")
        String email,

        UserRole role,

        UserStatus status
) {
}
