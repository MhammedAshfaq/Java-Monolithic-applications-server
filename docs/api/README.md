# API Development

## Overview

| Component | Purpose |
|-----------|---------|
| Controllers | HTTP endpoints, request handling |
| DTOs | Request/Response data structures |
| Validation | Input validation with annotations |
| Swagger | API documentation |

---

## Folder Structure

```
api/{feature}/
├── {Feature}Controller.java
└── dto/
    ├── {Action}Request.java
    ├── {Action}Response.java
    └── ...
```

**Example:**
```
api/auth/
├── AuthController.java
└── dto/
    ├── LoginRequest.java
    ├── LoginResponse.java
    ├── RegisterRequest.java
    └── RefreshTokenRequest.java
```

---

## Controller Example

```java
package com.example.api.auth;

import com.example.api.auth.dto.*;
import com.example.common.dto.ApiResponseDto;
import com.example.common.ratelimit.RateLimit;
import com.example.common.ratelimit.RateLimitType;
import com.example.services.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Authenticate user and return JWT")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Too many attempts")
    @RateLimit(type = RateLimitType.STRICT, key = "login")
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Register", description = "Create new account")
    @ApiResponse(responseCode = "201", description = "Registration successful")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Email exists")
    @RateLimit(type = RateLimitType.STRICT, key = "register")
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
}
```

---

## Request DTOs

Use Java `record` for immutable, concise DTOs:

```java
package com.example.api.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password
    
) {}
```

### Validation Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@NotBlank` | Not null, not empty | `@NotBlank String name` |
| `@NotNull` | Not null | `@NotNull Long id` |
| `@Email` | Valid email | `@Email String email` |
| `@Size` | Length range | `@Size(min=2, max=100)` |
| `@Min` / `@Max` | Number range | `@Min(1) Integer quantity` |
| `@Pattern` | Regex match | `@Pattern(regexp="[A-Z]+")` |
| `@Past` / `@Future` | Date validation | `@Past LocalDate birthDate` |

---

## Response DTOs

```java
package com.example.api.auth.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    // Convenience constructors
    public LoginResponse(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
    
    public LoginResponse(String accessToken, long expiresIn) {
        this(accessToken, null, "Bearer", expiresIn);
    }
}
```

---

## Standard API Response

All endpoints should return `ApiResponseDto<T>`:

```java
public record ApiResponseDto<T>(
    LocalDateTime timestamp,
    boolean success,
    String message,
    T data,
    ErrorDetails error
) {
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(LocalDateTime.now(), true, message, data, null);
    }
    
    public static <T> ApiResponseDto<T> error(String message, ErrorDetails error) {
        return new ApiResponseDto<>(LocalDateTime.now(), false, message, null, error);
    }
}
```

**Success Response:**
```json
{
    "timestamp": "2026-02-05T12:00:00",
    "success": true,
    "message": "Login successful",
    "data": {
        "accessToken": "eyJhbG...",
        "tokenType": "Bearer",
        "expiresIn": 3600
    },
    "error": null
}
```

**Error Response:**
```json
{
    "timestamp": "2026-02-05T12:00:00",
    "success": false,
    "message": "Validation failed",
    "data": null,
    "error": {
        "code": "VALIDATION_ERROR",
        "message": "Email is required"
    }
}
```

---

## Swagger Documentation

### Access Swagger UI

```
http://localhost:8082/apidocs
```

### Annotations

| Annotation | Purpose |
|------------|---------|
| `@Tag` | Group endpoints |
| `@Operation` | Describe endpoint |
| `@ApiResponse` | Document response |
| `@Parameter` | Describe parameter |
| `@Schema` | Describe model |

### Example

```java
@Tag(name = "Products", description = "Product management endpoints")
@RestController
@RequestMapping("/products")
public class ProductController {

    @Operation(
        summary = "Get product by ID",
        description = "Returns a single product"
    )
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponse>> getProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
```

---

## URL Patterns

| Pattern | Example | Description |
|---------|---------|-------------|
| `GET /resources` | `GET /products` | List all |
| `GET /resources/{id}` | `GET /products/1` | Get one |
| `POST /resources` | `POST /products` | Create |
| `PUT /resources/{id}` | `PUT /products/1` | Update |
| `DELETE /resources/{id}` | `DELETE /products/1` | Delete |
| `GET /resources/{id}/sub` | `GET /users/1/orders` | Sub-resource |

---

## HTTP Status Codes

| Code | When to Use |
|------|-------------|
| `200 OK` | Successful GET, PUT |
| `201 Created` | Successful POST |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Not authenticated |
| `403 Forbidden` | Not authorized |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Duplicate resource |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Server error |

---

## Rate Limiting

Apply rate limits to endpoints:

```java
// Strict: 5 requests/minute (login, register)
@RateLimit(type = RateLimitType.STRICT)
@PostMapping("/login")
public ResponseEntity<?> login(...) {}

// Short-term: 100 requests/minute (normal endpoints)
@RateLimit(type = RateLimitType.SHORT_TERM)
@GetMapping("/profile")
public ResponseEntity<?> getProfile(...) {}

// Custom key (separate counter)
@RateLimit(type = RateLimitType.STRICT, key = "password-reset")
@PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(...) {}
```

See [Rate Limiting](../rate-limit/README.md) for full documentation.

---

## Creating a New Endpoint

### 1. Create DTOs

```bash
mkdir -p src/main/java/.../api/product/dto
```

```java
// CreateProductRequest.java
public record CreateProductRequest(
    @NotBlank String name,
    @NotNull @Min(0) BigDecimal price,
    String description
) {}

// ProductResponse.java
public record ProductResponse(
    Long id,
    String name,
    BigDecimal price,
    String description,
    LocalDateTime createdAt
) {}
```

### 2. Create Controller

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(productService.create(request));
    }
}
```

### 3. Update Service

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ApiResponseDto<ProductResponse> create(CreateProductRequest request) {
        Product product = Product.builder()
            .name(request.name())
            .price(request.price())
            .description(request.description())
            .build();
        
        product = productRepository.save(product);
        
        return ApiResponseDto.success(
            "Product created",
            mapToResponse(product)
        );
    }
}
```
