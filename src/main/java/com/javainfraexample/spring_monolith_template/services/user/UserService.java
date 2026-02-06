package com.javainfraexample.spring_monolith_template.services.user;

import com.javainfraexample.spring_monolith_template.api.user.dto.UpdateUserRequest;
import com.javainfraexample.spring_monolith_template.api.user.dto.UserResponse;
import com.javainfraexample.spring_monolith_template.common.dto.ApiResponseDto;
import com.javainfraexample.spring_monolith_template.common.exception.ConflictException;
import com.javainfraexample.spring_monolith_template.common.exception.ResourceNotFoundException;
import com.javainfraexample.spring_monolith_template.common.redis.RedisCacheService;
import com.javainfraexample.spring_monolith_template.common.redis.RedisKey;
import com.javainfraexample.spring_monolith_template.domain.user.User;
import com.javainfraexample.spring_monolith_template.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * User management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RedisCacheService cacheService;

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Find user by ID with Redis caching.
     */
    public ApiResponseDto<UserResponse> findById(UUID id) {
        String cacheKey = RedisKey.USER.key(id.toString());

        Optional<UserResponse> cachedUser = cacheService.getObject(cacheKey, UserResponse.class);
        log.debug("Cached user: {}", cachedUser);
        if (cachedUser.isPresent()) {
            log.debug("Cache HIT for user: {}", id);
            return ApiResponseDto.success("User retrieved successfully", cachedUser.get());
        }
        log.debug("Cache MISS for user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        UserResponse response = UserResponse.from(user);
        cacheService.setObject(cacheKey, response, USER_CACHE_TTL);
        return ApiResponseDto.success("User retrieved successfully", response);
    }

    /**
     * Update user.
     */
    @Transactional
    public ApiResponseDto<UserResponse> update(UUID id, UpdateUserRequest request) {
        log.debug("Updating user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check email uniqueness if changing email
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already exists: " + request.email());
            }
            user.setEmail(request.email());
        }

        // Update fields if provided
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        User savedUser = userRepository.save(user);
        log.info("User updated: {}", id);

        // Invalidate cache
        cacheService.delete(RedisKey.USER.key(id.toString()));

        return ApiResponseDto.success("User updated successfully", UserResponse.from(savedUser));
    }

    /**
     * Delete user by ID.
     */
    @Transactional
    public ApiResponseDto<Void> delete(UUID id) {
        log.debug("Deleting user: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted: {}", id);

        // Invalidate cache
        cacheService.delete(RedisKey.USER.key(id.toString()));

        return ApiResponseDto.<Void>success("User deleted successfully", null);
    }

    /**
     * Find user by email.
     */
    public ApiResponseDto<UserResponse> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return ApiResponseDto.success("User retrieved successfully", UserResponse.from(user));
    }
}
