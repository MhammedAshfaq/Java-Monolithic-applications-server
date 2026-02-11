# Database & Migrations

## Overview

| Component | Technology |
|-----------|------------|
| Database | PostgreSQL |
| ORM | JPA / Hibernate |
| Connection Pool | HikariCP |
| Migrations | Flyway |

## Configuration

### application.yaml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/java-spring-mololithic
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

    hikari:
      pool-name: HikariPool
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 20000

  jpa:
    hibernate:
      ddl-auto: validate    # Don't auto-create tables
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

---

## Migrations

### Naming Convention

```
{YYYYMMDDHHMMSS}__{action_name}.sql

Format: timestamp__action.sql (double underscore)
```

**Examples:**
```
20260205120000__create_users_table.sql
20260205120100__create_refresh_tokens_table.sql
20260205143500__add_phone_to_users.sql
20260206090000__create_products_table.sql
```

### Create New Migration

**Option 1: Use Script (Recommended)**

```bash
./scripts/create-migration.sh <action_name>
```

**Examples:**

```bash
# Create users table
./scripts/create-migration.sh create_users_table

# Add column
./scripts/create-migration.sh add_phone_to_users

# Create index
./scripts/create-migration.sh add_index_on_users_email

# Create new table
./scripts/create-migration.sh create_orders_table
```

**Option 2: Manual**

```bash
# Get timestamp
date +%Y%m%d%H%M%S
# Output: 20260205143022

# Create file
touch src/main/resources/db/migration/20260205143022__add_phone_to_users.sql
```

### Migration File Structure

```sql
-- 20260205143022__add_phone_to_users.sql
-- Description: Add phone column to users table

ALTER TABLE users ADD COLUMN phone VARCHAR(20);

CREATE INDEX idx_users_phone ON users(phone);
```

### Migration Location

```
src/main/resources/
└── db/
    └── migration/
        ├── 20260205120000__create_users_table.sql
        ├── 20260205120100__create_refresh_tokens_table.sql
        └── 20260205143022__add_phone_to_users.sql
```

### Run Migrations

Migrations run **automatically on application startup**.

```bash
# Start app (migrations run automatically)
./mvnw spring-boot:run
```

### Check Migration Status

```bash
# View applied migrations
psql -U postgres -d java-spring-mololithic -c "SELECT * FROM flyway_schema_history;"
```

### Rollback Migration

Flyway doesn't support automatic rollback. Create a new migration to undo changes:

```bash
./scripts/create-migration.sh rollback_add_phone_to_users
```

```sql
-- 20260205150000__rollback_add_phone_to_users.sql
DROP INDEX IF EXISTS idx_users_phone;
ALTER TABLE users DROP COLUMN IF EXISTS phone;
```

---

## Seed Data

Seed data is **separate from migrations** and runs manually when needed.

### Location

```
src/main/resources/db/
├── migration/          # Flyway migrations (auto-run on startup)
│   ├── 20260205120000__create_users_table.sql
│   └── 20260205120100__create_refresh_tokens_table.sql
└── seed/               # Seed data (manual run)
    ├── users.sql
    ├── refresh_tokens.sql
    └── all.sql
```

### Available Seeds

| File | Description |
|------|-------------|
| `users.sql` | Sample users with different roles |
| `refresh_tokens.sql` | Sample refresh tokens |
| `all.sql` | Runs all seeds in order |

### Run Seed Data

#### Option 1: Local Development (Java runs locally, PostgreSQL in Docker)

Requires `psql` on your machine. Install with: `brew install libpq && brew link --force libpq`

```bash
# List available seeds
./scripts/run-seed.sh

# Seed users only
./scripts/run-seed.sh users

# Seed refresh tokens
./scripts/run-seed.sh refresh_tokens

# Seed all data
./scripts/run-seed.sh all
```

#### Option 2: Docker Container (no psql needed on host)

When everything runs in Docker, exec into the PostgreSQL container directly:

```bash
# Seed all data (users + refresh tokens)
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/users.sql
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/refresh_tokens.sql

# Seed users only
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/users.sql

# Seed refresh tokens only
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/refresh_tokens.sql
```

**Alternative: Copy files into container first**

```bash
# Copy seed files into the container
docker cp src/main/resources/db/seed/. spring-postgres:/tmp/seed/

# Run seeds inside the container
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -f /tmp/seed/users.sql
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -f /tmp/seed/refresh_tokens.sql
```

#### Verify seed data

```bash
# Local psql
psql -U postgres -d java-spring-mololithic -c "SELECT id, email, role, status FROM users;"

# Or via Docker
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "SELECT id, email, role, status FROM users;"
```

### Test Credentials

| Email | Password | Role | Status |
|-------|----------|------|--------|
| admin@example.com | password123 | ADMIN | ACTIVE |
| john@example.com | password123 | USER | ACTIVE |
| jane@example.com | password123 | USER | ACTIVE |
| mod@example.com | password123 | MODERATOR | ACTIVE |
| inactive@example.com | password123 | USER | INACTIVE |

### Create New Seed File

1. Create a new SQL file in `src/main/resources/db/seed/`:

```sql
-- src/main/resources/db/seed/products.sql
-- Seed data for products table

INSERT INTO products (id, name, price, created_at) VALUES
    ('...', 'Product 1', 99.99, NOW()),
    ('...', 'Product 2', 149.99, NOW())
ON CONFLICT (id) DO NOTHING;
```

2. Update `all.sql` to include it:

```sql
-- src/main/resources/db/seed/all.sql
\i users.sql
\i refresh_tokens.sql
\i products.sql
```

3. Run the seed:

```bash
./scripts/run-seed.sh products
# or
./scripts/run-seed.sh all
```

### Generate Password Hash

For custom seed data with passwords:

```bash
./scripts/generate-password-hash.sh myPassword123
```

**Pre-generated hash for `password123`:**
```
$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6
```

### Environment Variables

The seed script uses these defaults (can be overridden):

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=java-spring-mololithic
DB_USER=postgres
DB_PASSWORD=postgres
```

**Example with custom database:**

```bash
DB_NAME=my_database ./scripts/run-seed.sh all
```

### Workflow

**Local development (Java runs locally):**

```bash
# 1. Start infrastructure
docker-compose up -d postgres redis pgadmin redis-commander

# 2. Start app (migrations run automatically on startup)
./mvnw spring-boot:run

# 3. Seed data (in another terminal)
./scripts/run-seed.sh all

# 4. Verify
psql -U postgres -d java-spring-mololithic -c "SELECT id, email, role, status FROM users;"
```

**Full Docker (everything in Docker):**

```bash
# 1. Start all services (migrations run automatically on app startup)
docker-compose up -d

# 2. Seed data (in another terminal)
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/users.sql
docker exec -i spring-postgres psql -U postgres -d java-spring-mololithic < src/main/resources/db/seed/refresh_tokens.sql

# 3. Verify
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "SELECT id, email, role, status FROM users;"
```

---

## Entities

### Location

```
domain/{entity}/
├── {Entity}.java       # JPA Entity
├── {Entity}Role.java   # Enum (if needed)
└── {Entity}Status.java # Enum (if needed)
```

### Example Entity

```java
package com.example.domain.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### JPA Annotations

| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks as JPA entity |
| `@Table(name = "...")` | Table name |
| `@Id` | Primary key |
| `@GeneratedValue` | Auto-increment |
| `@Column` | Column mapping |
| `@Enumerated` | Enum storage |
| `@OneToMany`, `@ManyToOne` | Relationships |

---

## Repositories

### Location

```
repository/
└── {Entity}Repository.java
```

### Example Repository

```java
package com.example.repository;

import com.example.domain.user.User;
import com.example.domain.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ========== Derived Queries ==========
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByNameContainingIgnoreCase(String name);

    // ========== Custom JPQL Queries ==========
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);

    // ========== Update Queries ==========
    
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") UserStatus status);
}
```

### Built-in Methods (JpaRepository)

| Method | Description |
|--------|-------------|
| `save(entity)` | Insert or update |
| `findById(id)` | Find by ID |
| `findAll()` | Get all |
| `deleteById(id)` | Delete by ID |
| `count()` | Count all |
| `existsById(id)` | Check exists |

### Query Methods

Spring Data derives queries from method names:

| Method Name | Generated Query |
|-------------|-----------------|
| `findByEmail` | `WHERE email = ?` |
| `findByNameAndStatus` | `WHERE name = ? AND status = ?` |
| `findByEmailContaining` | `WHERE email LIKE %?%` |
| `findByCreatedAtAfter` | `WHERE created_at > ?` |
| `countByStatus` | `SELECT COUNT(*) WHERE status = ?` |

---

## Usage in Service

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public User create(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        
        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build();
            
        return userRepository.save(user);
    }
    
    @Transactional
    public void updateStatus(UUID userId, UserStatus status) {
        userRepository.updateStatus(userId, status);
    }
}
```

---

## Best Practices

### Migrations

1. **Never modify existing migrations** - Create new ones
2. **One change per migration** - Easier to debug
3. **Test migrations locally** - Before deploying
4. **Use IF EXISTS / IF NOT EXISTS** - Idempotent migrations

### Entities

1. **Use Lombok** - Reduce boilerplate
2. **Use Builder pattern** - Clean object creation
3. **Set defaults with @Builder.Default** - Avoid nulls
4. **Use enums for status fields** - Type safety

### Repositories

1. **Use Optional for single results** - Null safety
2. **Use @Modifying for UPDATE/DELETE** - Required by Spring
3. **Name methods clearly** - Self-documenting
