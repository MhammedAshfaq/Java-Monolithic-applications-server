package com.javainfraexample.spring_monolith_template.api.user.dto;

import com.javainfraexample.spring_monolith_template.domain.user.User;
import com.javainfraexample.spring_monolith_template.domain.user.UserRole;
import com.javainfraexample.spring_monolith_template.domain.user.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User response DTO - excludes sensitive fields like password.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {
    /**
     * Convert User entity to UserResponse DTO.
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}
