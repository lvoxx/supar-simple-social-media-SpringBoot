# user-service

**Type:** Spring Boot  
**Port:** `8081`  
**Module path:** `spring-services/services/user-service`  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (Redisson)  
**Messaging:** Apache Kafka  
**Events:** Axon Framework  

---

## Trách nhiệm

Quản lý toàn bộ danh tính người dùng, hồ sơ cá nhân, đồ thị mạng xã hội (follow/unfollow), cài đặt tài khoản, và đồng bộ với Keycloak.

---

## Tính năng chính

| Tính năng | Mô tả |
|-----------|-------|
| Đăng ký & đồng bộ Keycloak | Tạo user representation trên Keycloak Admin REST API đồng thời với local DB |
| Hồ sơ cá nhân | Avatar, background, bio, liên kết, vị trí, ngày sinh, trạng thái xác minh |
| Đồ thị xã hội | Follow / unfollow với bộ đếm atomic qua Redis distributed lock |
| Tài khoản riêng tư | Gửi follow request, OWNER phải duyệt trước khi kết nối |
| Cài đặt tài khoản | Notification preferences (per-service), theme, quyền riêng tư |
| Lịch sử tài khoản | Ghi log mọi hành động: đăng nhập, đổi mật khẩu, cập nhật hồ sơ (kèm IP) |
| Xác minh danh tính | Submit + review flow; admin phê duyệt badge xác minh |
| Auto-post | Khi đổi avatar/background, tùy chọn tự tạo post qua Kafka → post-service |

---

## Starters sử dụng

`starter-postgres` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events

### Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `user.profile.updated` | Bất kỳ cập nhật hồ sơ | search-svc, user-analysis-svc |
| `user.avatar.changed` | Đổi ảnh đại diện | post-svc (auto-post), search-svc |
| `user.background.changed` | Đổi ảnh nền | post-svc (auto-post), search-svc |
| `user.followed` | Người dùng A follow B | notification-svc, user-analysis-svc |
| `user.unfollowed` | Hủy follow | notification-svc |
| `user.verified` | Admin xác minh tài khoản | notification-svc |

### Consumed

| Topic | Hành động |
|-------|-----------|
| *(không có — inbound only via HTTP)* | — |

---

## Axon Commands & Events

| Command | Mô tả |
|---------|-------|
| `UpdateUserPreferencesCommand` | Broadcast thay đổi preference tới tất cả services |

| Event | Consumers |
|-------|-----------|
| `UserPreferencesUpdatedEvent` | notification-svc, post-svc, user-analysis-svc |

---

## Database Schema (Flyway)

```
V1__init_users.sql
V2__add_followers.sql
V3__add_verifications.sql
V4__add_account_history.sql
V5__add_follow_requests.sql
```

```sql
-- users
id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid()
keycloak_id         UUID        UNIQUE NOT NULL
username            VARCHAR(50) UNIQUE NOT NULL
display_name        VARCHAR(100)
bio                 TEXT
avatar_url          TEXT
background_url      TEXT
website_url         TEXT
location            VARCHAR(100)
birth_date          DATE
is_verified         BOOLEAN     DEFAULT FALSE
is_private          BOOLEAN     DEFAULT FALSE
role                VARCHAR(20) DEFAULT 'USER'         -- USER | MODERATOR | ADMIN
follower_count      INT         DEFAULT 0
following_count     INT         DEFAULT 0
post_count          INT         DEFAULT 0
status              VARCHAR(20) DEFAULT 'ACTIVE'        -- ACTIVE | SUSPENDED | DEACTIVATED
theme_settings      JSONB
notification_settings JSONB                            -- per-service prefs
account_settings    JSONB
created_at          TIMESTAMPTZ DEFAULT NOW()
updated_at          TIMESTAMPTZ
deleted_at          TIMESTAMPTZ
is_deleted          BOOLEAN     DEFAULT FALSE

-- followers
follower_id         UUID REFERENCES users(id)
following_id        UUID REFERENCES users(id)
created_at          TIMESTAMPTZ DEFAULT NOW()
PRIMARY KEY (follower_id, following_id)

-- follow_requests  (tài khoản private)
id                  UUID PRIMARY KEY
requester_id        UUID REFERENCES users(id)
target_id           UUID REFERENCES users(id)
status              VARCHAR(20) DEFAULT 'PENDING'       -- PENDING | APPROVED | REJECTED
created_at          TIMESTAMPTZ

-- account_history
id                  UUID PRIMARY KEY
user_id             UUID REFERENCES users(id)
action              VARCHAR(50)
detail              JSONB
ip                  INET
created_at          TIMESTAMPTZ

-- verifications
id                  UUID PRIMARY KEY
user_id             UUID REFERENCES users(id)
type                VARCHAR(30)
status              VARCHAR(20)
document_url        TEXT
reviewed_by         UUID
created_at          TIMESTAMPTZ
updated_at          TIMESTAMPTZ
```

---

## API Endpoints

```
# Hồ sơ công khai
GET    /api/v1/users/{username}

# Hồ sơ cá nhân (cần auth)
GET    /api/v1/users/me
PUT    /api/v1/users/me
PUT    /api/v1/users/me/avatar            → gọi media-service
PUT    /api/v1/users/me/background        → gọi media-service
PUT    /api/v1/users/me/settings
GET    /api/v1/users/me/history

# Xác minh
POST   /api/v1/users/me/verify

# Mạng xã hội
GET    /api/v1/users/{userId}/followers
GET    /api/v1/users/{userId}/following
POST   /api/v1/users/{userId}/follow
DELETE /api/v1/users/{userId}/follow

# Follow requests (tài khoản private)
GET    /api/v1/users/me/follow-requests
PUT    /api/v1/users/me/follow-requests/{reqId}   # {action: APPROVE|REJECT}

# Tìm kiếm (delegate sang search-service)
GET    /api/v1/users/search?q=
```

---

## Luồng tích hợp Keycloak

```
Đăng ký:
  1. POST /api/v1/auth/register
  2. user-service → Keycloak Admin API: POST /admin/realms/{realm}/users
  3. Lưu keycloakId vào bảng users local
  4. Trả về profile

Đổi mật khẩu / email:
  1. user-service → Keycloak Admin API: PUT /admin/realms/{realm}/users/{id}
  2. Cập nhật local nếu cần

Xóa tài khoản (soft):
  1. Set is_deleted = true, deleted_at = NOW() trong local DB
  2. Keycloak: disable user (không xóa để giữ audit trail)
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `user:profile:{userId}` | 5 phút | Hồ sơ theo ID |
| `user:profile:username:{username}` | 5 phút | Hồ sơ theo username |
| `user:followers:count:{userId}` | 1 phút | Số lượng follower |
| `user:following:count:{userId}` | 1 phút | Số lượng following |
| `user:is-following:{userId}:{targetId}` | 5 phút | Kiểm tra quan hệ follow |
| `user:settings:{userId}` | 10 phút | Cài đặt tài khoản |

---

## Rate Limits

| Endpoint | Giới hạn |
|----------|---------|
| `POST /follow` | 50 req/giờ per userId |
| `PUT /me` | 10 req/phút per userId |
| `POST /me/verify` | 3 req/ngày per userId |

---

## Cấu trúc source

```
user-service/src/main/java/com/xsocial/user/
├── config/
│   ├── KeycloakClientConfig.java
│   └── UserServiceConfig.java
├── domain/
│   ├── entity/User.java, Follower.java, FollowRequest.java
│   ├── repository/UserRepository.java, FollowerRepository.java
│   └── event/UserProfileUpdatedEvent.java, UserFollowedEvent.java
├── application/
│   ├── service/UserService.java, FollowService.java, VerificationService.java
│   ├── dto/UserResponse.java, UpdateProfileRequest.java, FollowResponse.java
│   └── mapper/UserMapper.java
├── infrastructure/
│   ├── kafka/UserEventPublisher.java
│   ├── redis/UserCacheService.java
│   └── external/KeycloakAdminClient.java, MediaServiceClient.java
├── web/
│   ├── router/UserRouter.java
│   ├── handler/UserHandler.java, FollowHandler.java
│   └── filter/RateLimitFilter.java
└── UserServiceApplication.java
```

---

## Tests

### Unit Tests
- `UserServiceImplTest` — mock repository, kiểm tra business rules
- `FollowServiceTest` — mock Redis lock, counter logic
- `KeycloakAdminClientTest` — mock WebClient responses

### Integration Tests (Testcontainers)
- `UserRepositoryIT` — PostgreSQL container
- `FollowServiceIT` — PostgreSQL + Redis container
- `UserKafkaPublisherIT` — Kafka container

### Automation Tests (WebTestClient)
- `UserApiAutomationTest` — register → update profile → follow → unfollow → verify history

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN mvn -ntp package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

---

## Observability

- **Metrics:** `/actuator/prometheus` — custom counters: `user.registrations.total`, `user.follows.total`, `user.profile.updates.total`
- **Tracing:** Zipkin — span per Keycloak API call, span per Kafka publish
- **Logging:** Structured JSON — fields: `userId`, `action`, `keycloakId`, `traceId`
- **Health:** `/actuator/health/liveness` + `/actuator/health/readiness` (checks DB + Redis + Keycloak reachability)
- **OpenAPI:** `/v3/api-docs` — tắt Swagger UI ở profile `prod`
