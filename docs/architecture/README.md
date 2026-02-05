# Project Architecture

## Folder Structure

```
spring-monolith-template/
├── src/
│   ├── main/
│   │   ├── java/.../
│   │   │   ├── api/                    # REST Controllers + DTOs
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── LoginRequest.java
│   │   │   │   │       ├── LoginResponse.java
│   │   │   │   │       ├── RegisterRequest.java
│   │   │   │   │       └── RefreshTokenRequest.java
│   │   │   │   ├── dev/
│   │   │   │   │   └── DevDashboardController.java
│   │   │   │   └── health/
│   │   │   │       └── HealthCheckController.java
│   │   │   │
│   │   │   ├── services/               # Business Logic
│   │   │   │   ├── auth/
│   │   │   │   │   └── AuthService.java
│   │   │   │   └── health/
│   │   │   │       └── HealthCheckService.java
│   │   │   │
│   │   │   ├── domain/                 # Entities + Enums
│   │   │   │   └── user/
│   │   │   │       ├── User.java
│   │   │   │       ├── UserRole.java
│   │   │   │       └── UserStatus.java
│   │   │   │
│   │   │   ├── repository/             # Data Access (JPA)
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   ├── common/                 # Shared Components
│   │   │   │   ├── dto/
│   │   │   │   │   └── ApiResponseDto.java
│   │   │   │   ├── exception/
│   │   │   │   └── ratelimit/
│   │   │   │       ├── RateLimitType.java
│   │   │   │       ├── RateLimitConfig.java
│   │   │   │       ├── RateLimiterService.java
│   │   │   │       ├── RateLimitFilter.java
│   │   │   │       ├── RateLimit.java
│   │   │   │       └── RateLimitAspect.java
│   │   │   │
│   │   │   ├── config/                 # Configuration
│   │   │   │   ├── redis/
│   │   │   │   │   └── RedisConfig.java
│   │   │   │   ├── security/
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   └── swagger/
│   │   │   │       └── SwaggerConfig.java
│   │   │   │
│   │   │   └── SpringMonolithTemplateApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       └── db/
│   │           └── migration/          # Flyway Migrations
│   │               ├── 20260205120000__create_users_table.sql
│   │               └── 20260205120100__create_refresh_tokens_table.sql
│   │
│   └── test/                           # Tests
│
├── scripts/
│   └── create-migration.sh             # Migration generator
│
├── docs/                               # Documentation
│
└── pom.xml
```

## Layer Responsibilities

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Request                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  RateLimitFilter                                                 │
│  - IP-based rate limiting                                        │
│  - Returns 429 if exceeded                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  SecurityConfig                                                  │
│  - Authentication                                                │
│  - Authorization                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  API Layer (Controllers)                     api/                │
│  - Request validation (@Valid)                                   │
│  - Request/Response DTOs                                         │
│  - HTTP status codes                                             │
│  - Swagger documentation                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Service Layer                               services/           │
│  - Business logic                                                │
│  - Transaction management                                        │
│  - Orchestration                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Repository Layer                            repository/         │
│  - Data access (JPA)                                             │
│  - Custom queries                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Domain Layer                                domain/             │
│  - Entities                                                      │
│  - Enums                                                         │
│  - Value Objects                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Database (PostgreSQL) + Redis                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Naming Conventions

### Files

| Type | Convention | Example |
|------|------------|---------|
| Controller | `{Feature}Controller.java` | `AuthController.java` |
| Service | `{Feature}Service.java` | `AuthService.java` |
| Repository | `{Entity}Repository.java` | `UserRepository.java` |
| Entity | `{Name}.java` | `User.java` |
| Request DTO | `{Action}Request.java` | `LoginRequest.java` |
| Response DTO | `{Action}Response.java` | `LoginResponse.java` |
| Migration | `{timestamp}__{action}.sql` | `20260205120000__create_users_table.sql` |

### Packages

| Package | Purpose |
|---------|---------|
| `api.{feature}` | Controllers for a feature |
| `api.{feature}.dto` | DTOs for that feature |
| `services.{feature}` | Business logic |
| `domain.{entity}` | Entity + related enums |
| `repository` | All repositories |
| `common.dto` | Shared DTOs |
| `common.exception` | Custom exceptions |
| `config.{type}` | Configuration classes |

## Adding a New Feature

Example: Adding a `Product` feature.

### 1. Create Entity

```
domain/product/
├── Product.java
├── ProductStatus.java
└── ProductCategory.java
```

### 2. Create Repository

```
repository/
└── ProductRepository.java
```

### 3. Create Migration

```bash
./scripts/create-migration.sh create_products_table
```

### 4. Create Service

```
services/product/
└── ProductService.java
```

### 5. Create Controller + DTOs

```
api/product/
├── ProductController.java
└── dto/
    ├── CreateProductRequest.java
    ├── UpdateProductRequest.java
    └── ProductResponse.java
```

## Configuration Files

| File | Purpose |
|------|---------|
| `application.yaml` | Common settings (all profiles) |
| `application-dev.yaml` | Development overrides |
| `application-prod.yaml` | Production overrides |

## Profiles

| Profile | Usage | Dev Tools |
|---------|-------|-----------|
| `dev` | Local development | Enabled |
| `prod` | Production | Disabled |

```bash
# Run with dev profile (default)
./mvnw spring-boot:run

# Run with prod profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```
