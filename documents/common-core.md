# common-core

**Type:** Spring Boot Library (không phải service)  
**Module path:** `spring-services/common/common-core`  
**Packaging:** JAR được import bởi tất cả Spring Boot services  

---

## Mục đích

Thư viện dùng chung cho tất cả Spring Boot services. Chứa: error handling, response wrappers, base entities, enums, validation utilities, và security context helpers. **Không chứa business logic.**

---

## Dependency

```xml
<!-- pom.xml của mỗi service -->
<dependency>
    <groupId>com.xsocial</groupId>
    <artifactId>common-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## Package Structure

```
com.xsocial.common.core/
├── exception/
├── handler/
├── message/
├── model/
├── enums/
├── security/
├── util/
└── validation/
```

---

## exception/

### `BusinessException` (base)
```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Object[] args;
    
    public BusinessException(String errorCode, Object... args) { ... }
}
```

### Các exception con

| Class | HTTP Status | Dùng khi |
|-------|-----------|---------|
| `ResourceNotFoundException` | 404 | Entity không tồn tại |
| `ConflictException` | 409 | Trạng thái đã tồn tại (đã follow, đã like) |
| `ForbiddenException` | 403 | Không có quyền thực hiện |
| `ValidationException` | 422 | Input không hợp lệ |
| `ExternalServiceException` | 502/503 | Service bên ngoài lỗi |
| `RateLimitExceededException` | 429 | Vượt rate limit |

```java
// Sử dụng
throw new ResourceNotFoundException("USER_NOT_FOUND", userId);
throw new ConflictException("ALREADY_FOLLOWING", targetId);
throw new ForbiddenException("NOT_GROUP_MEMBER", groupId);
```

---

## handler/

### `GlobalErrorWebExceptionHandler`

Reactive `WebExceptionHandler` — bắt tất cả exception và chuyển về `ApiResponse` chuẩn.

```java
@Component
@Order(-2)   // trước DefaultErrorWebExceptionHandler của Spring
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ErrorResponse error = mapException(ex);
        return writeResponse(exchange, error);
    }
}
```

### `ErrorResponse`
```java
public record ErrorResponse(
    String code,
    String message,
    List<String> details,
    Instant timestamp
) {}
```

---

## message/

### `MessageKeys` — error code constants
```java
public final class MessageKeys {
    // Users
    public static final String USER_NOT_FOUND        = "USER_NOT_FOUND";
    public static final String USERNAME_TAKEN        = "USERNAME_TAKEN";
    public static final String ALREADY_FOLLOWING     = "ALREADY_FOLLOWING";
    public static final String NOT_FOLLOWING         = "NOT_FOLLOWING";
    
    // Posts
    public static final String POST_NOT_FOUND        = "POST_NOT_FOUND";
    public static final String POST_CONTENT_REJECTED = "POST_CONTENT_REJECTED";
    public static final String POST_ALREADY_LIKED    = "POST_ALREADY_LIKED";
    
    // Media
    public static final String MEDIA_NOT_FOUND       = "MEDIA_NOT_FOUND";
    public static final String MEDIA_PROCESSING      = "MEDIA_STILL_PROCESSING";
    public static final String MEDIA_REJECTED        = "MEDIA_CONTENT_REJECTED";
    public static final String FILE_TOO_LARGE        = "FILE_SIZE_EXCEEDS_LIMIT";
    public static final String INVALID_FILE_TYPE     = "INVALID_FILE_TYPE";
    
    // Groups
    public static final String GROUP_NOT_FOUND       = "GROUP_NOT_FOUND";
    public static final String ALREADY_MEMBER        = "ALREADY_GROUP_MEMBER";
    public static final String NOT_MEMBER            = "NOT_GROUP_MEMBER";
    public static final String INSUFFICIENT_ROLE     = "INSUFFICIENT_GROUP_ROLE";
    public static final String OWNER_CANNOT_LEAVE    = "OWNER_CANNOT_LEAVE_GROUP";
    public static final String MAX_PINS_REACHED      = "MAX_PINNED_POSTS_REACHED";
    
    // Messages
    public static final String CONV_NOT_FOUND        = "CONVERSATION_NOT_FOUND";
    public static final String MSG_NOT_FOUND         = "MESSAGE_NOT_FOUND";
    public static final String MSG_EDIT_EXPIRED      = "MESSAGE_EDIT_TIME_EXPIRED";
    public static final String DM_NOT_ALLOWED        = "DIRECT_MESSAGE_NOT_ALLOWED";
    
    // Rate limit
    public static final String RATE_LIMIT_EXCEEDED   = "RATE_LIMIT_EXCEEDED";
    
    // Generic
    public static final String FORBIDDEN             = "ACCESS_FORBIDDEN";
    public static final String INTERNAL_ERROR        = "INTERNAL_SERVER_ERROR";
    public static final String EXTERNAL_SERVICE_DOWN = "EXTERNAL_SERVICE_UNAVAILABLE";
}
```

---

## model/

### `ApiResponse<T>` — Standard response envelope
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorResponse error,
    ApiMeta meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ApiMeta.now());
    }
    
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error, ApiMeta.now());
    }
}

public record ApiMeta(String requestId, Instant timestamp, String version) {
    public static ApiMeta now() {
        return new ApiMeta(UlidGenerator.generate(), Instant.now(), "v1");
    }
}
```

### `PageResponse<T>` — Paginated response
```java
public record PageResponse<T>(
    List<T> items,
    String nextCursor,      // null nếu hết dữ liệu
    boolean hasMore,
    Long total              // null cho Cassandra (không đếm được)
) {}
```

### `AuditableEntity` — Base cho tất cả entities
```java
@Data
public abstract class AuditableEntity {
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    @CreatedBy
    private UUID createdBy;
    
    @LastModifiedBy
    private UUID updatedBy;
}
```

### `SoftDeletableEntity` — Kế thừa AuditableEntity
```java
@Data
public abstract class SoftDeletableEntity extends AuditableEntity {
    private Boolean isDeleted = false;
    private Instant deletedAt;
    private UUID deletedBy;
}
```

---

## enums/

```java
public enum UserRole {
    USER, MODERATOR, ADMIN, SYSTEM
}

public enum ContentStatus {
    ACTIVE,
    HIDDEN,         // ẩn bởi moderator
    FLAGGED,        // chờ review
    PENDING_REVIEW, // đang review
    DELETED         // soft deleted
}

public enum MediaType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, STICKER
}

public enum NotificationType {
    LIKE, COMMENT, FOLLOW, MENTION, REPOST,
    SYSTEM, GROUP_JOIN, GROUP_ROLE_CHANGED,
    MESSAGE_REACTION, CONVERSATION_CREATED
}

public enum GroupMemberRole {
    OWNER, ADMIN, MODERATOR, MEMBER
}

public enum GroupVisibility {
    PUBLIC, PRIVATE, INVITE_ONLY
}

public enum MessageStatus {
    SENT, DELIVERED, READ, FAILED, DELETED
}

public enum ConversationType {
    DIRECT, GROUP_CHAT, GROUP_CHANNEL
}
```

---

## security/

### `UserPrincipal` — Parsed JWT claims
```java
public record UserPrincipal(
    UUID userId,
    String username,
    Set<UserRole> roles,
    String ip
) {
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }
    
    public boolean isAdmin() {
        return roles.contains(UserRole.ADMIN);
    }
    
    public boolean isModerator() {
        return roles.contains(UserRole.ADMIN) 
            || roles.contains(UserRole.MODERATOR);
    }
}
```

### `@CurrentUser` — Annotation cho controller parameters
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {}

// Sử dụng trong handler
public Mono<ApiResponse<UserResponse>> getMe(@CurrentUser UserPrincipal user) {
    return userService.findById(user.userId()).map(ApiResponse::success);
}
```

---

## util/

### `UlidGenerator`
```java
public final class UlidGenerator {
    // Tạo ULID (Universally Unique Lexicographically Sortable Identifier)
    // Time-sortable, UUID-compatible, không có hotspot
    public static String generate() { ... }      // "01HXZ7K5P3..."
    public static UUID generateAsUUID() { ... }  // UUID from ULID bits
}
```

### `ReactiveContextUtil`
```java
public final class ReactiveContextUtil {
    // Extract UserPrincipal từ Reactor Context (propagated bởi starter-security)
    public static Mono<UserPrincipal> getCurrentUser() {
        return Mono.deferContextual(ctx -> 
            Mono.justOrEmpty(ctx.getOrEmpty(UserPrincipal.class)));
    }
    
    public static Mono<UUID> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::userId);
    }
}
```

### `SlugUtil`
```java
public final class SlugUtil {
    // "Hello World!" → "hello-world"
    public static String toSlug(String text) { ... }
    // Validate slug format
    public static boolean isValidSlug(String slug) { ... }
}
```

---

## validation/

### `ReactiveValidator`
```java
@Component
public class ReactiveValidator {
    private final Validator validator;
    
    // Validate và trả Mono.error(ValidationException) nếu có lỗi
    public <T> Mono<T> validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (violations.isEmpty()) return Mono.just(object);
        return Mono.error(new ValidationException(buildMessage(violations)));
    }
}
```

---

## Auto-configuration

`common-core` đăng ký auto-configuration qua:

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:
  com.xsocial.common.core.handler.GlobalErrorWebExceptionHandlerAutoConfig
  com.xsocial.common.core.validation.ReactiveValidatorAutoConfig
```

Services không cần cấu hình thêm — import dependency là dùng được ngay.
