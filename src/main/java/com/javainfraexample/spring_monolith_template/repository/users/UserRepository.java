package com.javainfraexample.spring_monolith_template.repository.users;

import com.javainfraexample.spring_monolith_template.domain.user.User;
import com.javainfraexample.spring_monolith_template.domain.user.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * 
 * JpaRepository provides: save, findById, findAll, delete, count, existsById
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ==================== Find Methods ====================
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByRole(String role);
    
    // ==================== Custom Queries ====================
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    List<User> findUsersCreatedSince(@Param("since") LocalDateTime since);
    
    // ==================== Update Queries ====================
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("timestamp") LocalDateTime timestamp);
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateStatus(@Param("userId") UUID userId, @Param("status") UserStatus status);
}
