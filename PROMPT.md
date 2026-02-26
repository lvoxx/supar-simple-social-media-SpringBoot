# 🤖 Master Prompt — Social Media Microservice Backend (X/Twitter Clone)

> Copy toàn bộ nội dung dưới đây và paste vào Claude để sinh code dự án.

---

## SYSTEM CONTEXT

You are a **Senior Software Architect & Full-Stack Backend Engineer** specializing in:

- Reactive microservice architecture with Spring Boot (WebFlux/R2DBC) and FastAPI (async)
- Cloud-native, production-grade, HA/Low-latency distributed systems
- Event-driven architecture (CQRS/Event Sourcing via Axon Framework, Kafka)
- Kubernetes, Helm, ArgoCD, Jenkins CI/CD pipelines

Your task: **Generate the complete backend source code, configurations, Dockerfiles, Helm charts, CI/CD pipelines, and all documentation** for a social media platform modeled after X (Twitter). Every output must be **production-ready**, **fully reactive**, and **HA-ready**.

---

## PROJECT OVERVIEW

**Project Name:** `x-social-platform`  
**Goal:** Backend-only social media platform (no frontend clients yet).  
**Reference:** Twitter / X feature set.

---

## TECHNOLOGY STACK

### Spring Boot Services

| Component      | Version / Detail                                                               |
| -------------- | ------------------------------------------------------------------------------ |
| Spring Boot    | 4.0.2                                                                          |
| Java           | 21 (virtual threads + reactive)                                                |
| Build          | Maven (multi-module), packaged as extracted JAR via Dockerfile                 |
| Reactive Stack | Spring WebFlux, R2DBC, Project Reactor                                         |
| Auth           | Keycloak (OAuth2 + OIDC); JWT validated at K8S Gateway only                    |
| Messaging      | Apache Kafka (reactive via reactor-kafka)                                      |
| Event Sourcing | Axon Framework (CQRS/ES between services)                                      |
| Database       | PostgreSQL (R2DBC), Cassandra (reactive), Elasticsearch                        |
| Cache          | Redis + Redisson (reactive, annotation-driven `@Cacheable`, distributed locks) |
| Search         | Elasticsearch (reactive)                                                       |
| Metrics        | Micrometer + Zipkin (distributed tracing)                                      |
| Logging        | Structured JSON logging (Logback), ELK stack                                   |
| API Docs       | SpringDoc OpenAPI 3                                                            |
| Testing        | JUnit 5, Testcontainers, Mockito, WebTestClient                                |
| Rate Limiting  | Redis Token Bucket (Bucket4j + Redisson)                                       |

### FastAPI Services

| Component | Version / Detail                                      |
| --------- | ----------------------------------------------------- |
| FastAPI   | 0.133                                                 |
| Python    | 3.12                                                  |
| Async     | asyncio, httpx, aiokafka                              |
| AI/ML     | LangChain, sentence-transformers, scikit-learn, torch |
| RAG       | ChromaDB / Qdrant vector store                        |
| Database  | asyncpg (PostgreSQL), motor (MongoDB)                 |
| Cache     | aioredis                                              |
| Metrics   | prometheus-fastapi-instrumentator, opentelemetry-sdk  |
| API Docs  | FastAPI built-in (OpenAPI 3)                          |
| Testing   | pytest, pytest-asyncio, httpx                         |

---

## REPOSITORY STRUCTURE

Generate the following monorepo layout **exactly**:

```
x-social-platform/
├── spring-services/
│   ├── common/
│   │   └── common-core/               # Shared library: errors, messages, abstracts, enums
│   ├── starters/
│   │   ├── starter-kafka/             # Kafka reactive config + producer/consumer beans
│   │   ├── starter-redis/             # Redis + Redisson reactive config + rate limiter
│   │   ├── starter-elasticsearch/     # ES reactive client config
│   │   ├── starter-metrics/           # Micrometer + Zipkin + health actuator
│   │   ├── starter-postgres/          # R2DBC PostgreSQL config + Flyway migration
│   │   ├── starter-cassandra/         # Reactive Cassandra config
│   │   ├── starter-websocket/         # Reactive WebSocket config
│   │   └── starter-security/          # JWT claim extraction (no auth, claims forwarding)
│   └── services/
│       ├── user-service/
│       ├── media-service/
│       ├── post-service/
│       ├── comment-service/
│       ├── notification-service/
│       ├── search-service/
│       ├── group-service/
│       ├── private-message-service/
│       └── message-notification-service/
├── python-services/
│   ├── user-analysis-service/         # FastAPI: user behavior analysis
│   ├── post-guard-service/            # FastAPI: content moderation
│   ├── media-guard-service/           # FastAPI: media safety check
│   └── ai-dashboard-service/          # FastAPI: AI analytics dashboard
├── infrastructure/
│   ├── helm/
│   │   ├── charts/                    # Per-service Helm chart
│   │   └── umbrella/                  # Umbrella chart for full deployment
│   ├── k8s/
│   │   ├── namespace.yaml
│   │   ├── ingress/
│   │   └── keycloak/
│   ├── jenkins/
│   │   └── Jenkinsfile                # Per-service CI pipeline
│   └── argocd/
│       └── application.yaml           # ArgoCD Application manifests
└── docs/
    ├── README.md
    ├── ARCHITECTURE.md
    ├── CONVENTIONS.md
    ├── services/
    │   ├── user-service.md
    │   ├── media-service.md
    │   ├── post-service.md
    │   ├── comment-service.md
    │   ├── notification-service.md
    │   ├── search-service.md
    │   ├── group-service.md
    │   ├── private-message-service.md
    │   ├── message-notification-service.md
    │   ├── user-analysis-service.md
    │   ├── post-guard-service.md
    │   ├── media-guard-service.md
    │   └── ai-dashboard-service.md
    └── modules/
        ├── common-core.md
        └── starters.md
```

---

## GLOBAL RULES (APPLY TO ALL SERVICES)

### 1. Reactive First

- Spring services: **100% reactive** — use `Mono<T>`, `Flux<T>`, `R2DBC`, `reactor-kafka`, `reactive Redis`.
- FastAPI services: **100% async/await** — use `async def`, `aiokafka`, `asyncpg`, `aioredis`.
- **No blocking calls** anywhere in the hot path.

### 2. Authentication & Authorization

- Keycloak issues JWT (OAuth2/OIDC).
- **K8S Ingress/Gateway validates JWT**; services receive pre-authenticated requests.
- All services extract claims from request headers (`X-User-Id`, `X-User-Roles`, `X-Forwarded-For`) forwarded by the gateway.
- `starter-security` provides `ReactiveSecurityContextHolder` utilities and claim extraction filter beans (no auth logic, only context propagation).

### 3. Soft Delete Only

- **NEVER physically delete user-uploaded data** (posts, comments, media, messages, profiles).
- Use `deleted_at TIMESTAMPTZ`, `deleted_by UUID`, `is_deleted BOOLEAN DEFAULT FALSE`.
- System/operational data (logs, metrics, temp tokens) may be hard-deleted via scheduled jobs.

### 4. Caching Strategy

- Every read-heavy endpoint uses `@Cacheable` (Redis) with TTL.
- Write-through / cache-aside pattern for critical entities.
- `@CacheEvict` on mutations.
- Distributed `RLock` (Redisson) for update/write operations on shared resources.

### 5. Rate Limiting

- All public/semi-public endpoints protected by **Bucket4j + Redisson** token bucket per `userId` (authenticated) or `IP` (anonymous).
- Configure via `application.yaml` `rate-limit` section in each service.
- Return `429 Too Many Requests` with `Retry-After` header.

### 6. Observability

Every service MUST include:

- **Structured JSON logs** with `traceId`, `spanId`, `userId`, `service` fields.
- **Micrometer metrics** exposed at `/actuator/metrics` (Prometheus scrape endpoint `/actuator/prometheus`).
- **Zipkin/OpenTelemetry** distributed tracing.
- **Health endpoints**: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.
- **OpenAPI 3 docs**: `/v3/api-docs`, Swagger UI at `/swagger-ui.html` (disabled in prod via profile).

### 7. Testing Requirements

Every service MUST have:

```
src/test/
├── unit/           # Pure unit tests (no I/O) — JUnit5 + Mockito / pytest + unittest.mock
├── integration/    # Integration tests with Testcontainers (postgres, redis, kafka, etc.)
└── automation/     # End-to-end API automation tests (WebTestClient / httpx TestClient)
```

- Minimum **80% line coverage** enforced by Maven Surefire/Failsafe + JaCoCo / pytest-cov.
- Integration tests use `@Testcontainers` with real Docker images.
- Automation tests run against a local compose stack.

### 8. Docker & Packaging

**Spring Boot (extracted JAR pattern):**

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN mvn -ntp package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**FastAPI (Python):**

```dockerfile
FROM python:3.12-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /install /usr/local
COPY . .
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

### 9. Configuration Convention

- Prefer **YAML** configuration over Java/Python code beans.
- Use **Spring profiles**: `default`, `dev`, `staging`, `prod`.
- Secrets via Kubernetes Secrets (referenced in Helm `values.yaml`).
- Helm `values.yaml` defines all environment-specific overrides.

### 10. Axon Framework Usage

- Use Axon for **behavioral state changes propagated across services** (e.g., user preference changes, feature flag updates, aggregated domain events).
- Do **NOT** use Axon for request/response data fetch — use Kafka consumer reads or reactive HTTP clients for that.
- Each Axon event must be versioned (`@Revision`).

---

## STARTERS SPECIFICATION

Generate each starter as a Spring Boot auto-configuration library with `spring.factories` / `@AutoConfiguration`.

### `starter-kafka`

```yaml
# application.yaml convention
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    listener:
      ack-mode: MANUAL_IMMEDIATE
```

Provide: `ReactiveKafkaProducerTemplate<String, Object>` bean, `ReactiveKafkaConsumerTemplate` factory, dead-letter topic config, retry policy (exponential backoff).

### `starter-redis`

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
redisson:
  single-server-config:
    address: redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}
```

Provide: `ReactiveRedisTemplate<String, Object>` bean, `RedissonClient` bean, `RateLimiterService` using Bucket4j + Redisson, `@EnableCaching` with `RedisCacheManager`.

### `starter-elasticsearch`

```yaml
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:}
```

Provide: `ReactiveElasticsearchClient` bean, index lifecycle management utility.

### `starter-metrics`

Configure: Micrometer Zipkin exporter, Prometheus registry, custom `@Timed` aspects, `X-Request-Id` MDC propagation filter, JVM / HTTP / DB metrics.

### `starter-postgres`

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    user: ${DB_USER}
    password: ${DB_PASSWORD}
    locations: classpath:db/migration
```

Provide: `ConnectionFactory` bean, `R2dbcEntityTemplate`, Flyway migration runner.

### `starter-cassandra`

```yaml
spring:
  cassandra:
    contact-points: ${CASSANDRA_CONTACT_POINTS:localhost}
    port: ${CASSANDRA_PORT:9042}
    keyspace-name: ${CASSANDRA_KEYSPACE}
    local-datacenter: datacenter1
    schema-action: CREATE_IF_NOT_EXISTS
```

### `starter-websocket`

Reactive WebSocket config with `ReactorNettyWebSocketClient`, STOMP over WebSocket via RSocket or standard WebSocket endpoint factory.

### `starter-security`

Extract JWT claims from gateway-forwarded headers. Provide `UserPrincipal` record and `@CurrentUser` annotation for controllers. No `spring-security` auth filter chain — gateway already handled it.

---

## COMMON-CORE SPECIFICATION

Package: `com.xsocial.common.core`

Generate:

```
common-core/src/main/java/com/xsocial/common/core/
├── exception/
│   ├── BusinessException.java          # Base checked business exception
│   ├── ResourceNotFoundException.java
│   ├── ConflictException.java
│   ├── ForbiddenException.java
│   ├── ValidationException.java
│   └── ExternalServiceException.java
├── handler/
│   ├── GlobalErrorWebExceptionHandler.java  # Reactive error handler
│   └── ErrorResponse.java                   # Standard error envelope
├── message/
│   ├── MessageKeys.java                # Constants: MSG_001=..., WARN_001=...
│   └── MessageSource.java              # i18n message resolver
├── model/
│   ├── AuditableEntity.java            # createdAt, updatedAt, createdBy, updatedBy
│   ├── SoftDeletableEntity.java        # deletedAt, deletedBy, isDeleted
│   ├── PageResponse.java               # Generic paginated response wrapper
│   └── ApiResponse.java                # Standard API envelope {success, data, error, meta}
├── enums/
│   ├── UserRole.java
│   ├── ContentStatus.java              # ACTIVE, HIDDEN, FLAGGED, DELETED
│   ├── MediaType.java
│   ├── NotificationType.java
│   ├── GroupMemberRole.java            # OWNER, ADMIN, MODERATOR, MEMBER
│   ├── GroupVisibility.java            # PUBLIC, PRIVATE, INVITE_ONLY
│   ├── MessageStatus.java              # SENT, DELIVERED, READ, FAILED
│   └── ConversationType.java           # DIRECT, GROUP_CHAT, GROUP_CHANNEL
├── util/
│   ├── UlidGenerator.java              # ULID-based ID generation
│   ├── SlugUtil.java
│   └── ReactiveContextUtil.java        # Extract userId from Reactor context
└── validation/
    └── ReactiveValidator.java          # Reactive bean validation helper
```

---

## SERVICE SPECIFICATIONS

---

### SERVICE 1: `user-service` (Spring Boot)

**Port:** 8081  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (`@Cacheable` on profile reads, followers/following counts)  
**Events Published (Kafka):** `user.profile.updated`, `user.avatar.changed`, `user.background.changed`, `user.followed`, `user.unfollowed`, `user.verified`  
**Events Consumed (Kafka):** none  
**Axon Commands/Events:** `UpdateUserPreferencesCommand`, `UserPreferencesUpdatedEvent`

**Keycloak Integration:**

- On user registration: call Keycloak Admin API to create user representation.
- On password change / email update: sync to Keycloak via Admin REST client (reactive WebClient).
- Store `keycloakUserId` (UUID) in local `users` table.

**Database Schema (Flyway):**

```sql
-- users
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
keycloak_id UUID UNIQUE NOT NULL,
username VARCHAR(50) UNIQUE NOT NULL,
display_name VARCHAR(100),
bio TEXT,
avatar_url TEXT,
background_url TEXT,
website_url TEXT,
location VARCHAR(100),
birth_date DATE,
is_verified BOOLEAN DEFAULT FALSE,
is_private BOOLEAN DEFAULT FALSE,
role VARCHAR(20) DEFAULT 'USER',  -- USER, MODERATOR, ADMIN
follower_count INT DEFAULT 0,
following_count INT DEFAULT 0,
post_count INT DEFAULT 0,
status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, DEACTIVATED
theme_settings JSONB,
notification_settings JSONB,  -- per-service notification prefs
account_settings JSONB,
created_at TIMESTAMPTZ DEFAULT NOW(),
updated_at TIMESTAMPTZ,
deleted_at TIMESTAMPTZ,
is_deleted BOOLEAN DEFAULT FALSE

-- followers
follower_id UUID REFERENCES users(id),
following_id UUID REFERENCES users(id),
created_at TIMESTAMPTZ DEFAULT NOW(),
PRIMARY KEY (follower_id, following_id)

-- account_history
id UUID, user_id UUID, action VARCHAR(50), detail JSONB, ip INET, created_at TIMESTAMPTZ

-- verifications
id UUID, user_id UUID, type VARCHAR(30), status VARCHAR(20), document_url TEXT, reviewed_by UUID, created_at, updated_at
```

**API Endpoints:**

```
GET    /api/v1/users/{username}           # Public profile
GET    /api/v1/users/{userId}/followers   # Paginated followers
GET    /api/v1/users/{userId}/following   # Paginated following
POST   /api/v1/users/{userId}/follow      # Follow user
DELETE /api/v1/users/{userId}/follow      # Unfollow user
GET    /api/v1/users/me                   # Own profile (JWT claims)
PUT    /api/v1/users/me                   # Update profile
PUT    /api/v1/users/me/avatar            # Update avatar → calls media-service
PUT    /api/v1/users/me/background        # Update background → calls media-service
PUT    /api/v1/users/me/settings          # Update account/notification/theme settings
GET    /api/v1/users/me/history           # Account activity history
POST   /api/v1/users/me/verify            # Submit verification request
GET    /api/v1/users/search?q=            # Username/display-name search (delegates to search-service)
```

**Business Rules:**

- When avatar/background changes: publish `user.avatar.changed` / `user.background.changed` to Kafka. `post-service` consumes and optionally auto-creates a post (only if `notification_settings.post_on_avatar_change == true`).
- Follow/unfollow: update counters atomically using `UPDATE users SET follower_count = follower_count + 1`.
- Private accounts: follower requests go through approval flow.
- Rate limit: follow = 50/hour, profile update = 10/min.

---

### SERVICE 2: `media-service` (Spring Boot)

**Port:** 8082  
**Database:** PostgreSQL (R2DBC) — stores media metadata & URLs  
**External:** Cloudinary (async via WebClient)  
**Cache:** Redis (media URL cache by `mediaId`, TTL 24h)  
**Events Published (Kafka):** `media.upload.completed`, `media.upload.failed`  
**Events Consumed (Kafka):** none

**Media Guard Rules (inline service, not separate):**

- Reject: executables, archives disguising as images, oversized files.
- Image: resize/compress using `libvips` (via ProcessBuilder or native binding) before Cloudinary upload. Max 2MB for avatars, 8MB for post images.
- Video: accept up to 512MB raw, transcode to H.264/AAC via FFmpeg subprocess. Return processing status; notify caller via Kafka `media.upload.completed`.
- NSFW detection: call `media-guard-service` (FastAPI) via WebClient before finalizing upload.

**Async Upload Flow:**

1. Client/service uploads file → `media-service` validates & saves metadata as `PROCESSING`.
2. Compression/transcoding runs asynchronously (Reactor `publishOn(Schedulers.boundedElastic())`).
3. Uploads to Cloudinary.
4. Calls `media-guard-service` for content safety check.
5. Updates metadata to `READY` / `REJECTED`.
6. Publishes `media.upload.completed` or `media.upload.failed` on Kafka.
7. Callers (`post-service`, `user-service`) consume this event to finalize their workflow.

**Database Schema:**

```sql
-- media_assets
id UUID PK, owner_id UUID, owner_type VARCHAR(20),   -- USER, POST, COMMENT, MESSAGE
original_filename TEXT, content_type VARCHAR(100), file_size_bytes BIGINT,
cloudinary_public_id TEXT, cloudinary_url TEXT, cdn_url TEXT,
width INT, height INT, duration_seconds INT,
status VARCHAR(20),   -- PROCESSING, READY, REJECTED, DELETED
rejection_reason TEXT,
is_deleted BOOLEAN DEFAULT FALSE, deleted_at TIMESTAMPTZ,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
```

**API Endpoints:**

```
POST   /api/v1/media/upload              # Multipart upload (returns mediaId + status)
GET    /api/v1/media/{mediaId}           # Get media metadata & URL
GET    /api/v1/media/{mediaId}/status    # Polling endpoint for async uploads
DELETE /api/v1/media/{mediaId}           # Soft delete (owner only)
```

---

### SERVICE 3: `post-service` (Spring Boot)

**Port:** 8083  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (post by ID TTL 5min, trending posts TTL 1min, feed cache per user TTL 30s)  
**Events Published (Kafka):** `post.created`, `post.updated`, `post.deleted`, `post.liked`, `post.reposted`, `post.bookmarked`, `post.reported`  
**Events Consumed (Kafka):** `media.upload.completed`, `media.upload.failed`, `user.avatar.changed`, `user.background.changed`

**Post Guard Flow:**
Before publishing any user-created post:

1. Extract text content → call `post-guard-service` (FastAPI) via WebClient.
2. If media attached → await `media.upload.completed` Kafka event (with timeout 5 min).
3. Guard returns: `APPROVED`, `FLAGGED` (hold for review), `REJECTED` (return 422 to user).

**Database Schema:**

```sql
-- posts
id UUID PK, author_id UUID, content TEXT (max 280 chars),
reply_to_id UUID REFERENCES posts(id),  -- for threads
repost_of_id UUID REFERENCES posts(id), -- repost reference
post_type VARCHAR(20),  -- ORIGINAL, REPLY, REPOST, QUOTE, AUTO (system posts)
status VARCHAR(20),     -- DRAFT, PENDING_MEDIA, PUBLISHED, HIDDEN, DELETED
visibility VARCHAR(20), -- PUBLIC, FOLLOWERS_ONLY, MENTIONED_ONLY
like_count INT DEFAULT 0, repost_count INT DEFAULT 0, reply_count INT DEFAULT 0,
bookmark_count INT DEFAULT 0, view_count INT DEFAULT 0,
is_edited BOOLEAN DEFAULT FALSE, edited_at TIMESTAMPTZ,
is_pinned BOOLEAN DEFAULT FALSE,
is_deleted BOOLEAN DEFAULT FALSE, deleted_at TIMESTAMPTZ, deleted_by UUID,
created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ

-- post_media (post ↔ media_asset join)
post_id UUID, media_id UUID, position INT, PRIMARY KEY (post_id, media_id)

-- post_likes
post_id UUID, user_id UUID, created_at TIMESTAMPTZ, PRIMARY KEY (post_id, user_id)

-- post_bookmarks
post_id UUID, user_id UUID, created_at TIMESTAMPTZ, PRIMARY KEY (post_id, user_id)

-- post_edits (audit trail of edits)
id UUID, post_id UUID, previous_content TEXT, edited_at TIMESTAMPTZ, edited_by UUID

-- post_reports
id UUID, post_id UUID, reporter_id UUID, reason VARCHAR(50), detail TEXT,
status VARCHAR(20) DEFAULT 'PENDING', created_at TIMESTAMPTZ
```

**API Endpoints:**

```
POST   /api/v1/posts                          # Create post
GET    /api/v1/posts/{postId}                 # Get post detail
PUT    /api/v1/posts/{postId}                 # Edit post (appends to post_edits)
DELETE /api/v1/posts/{postId}                 # Soft delete
GET    /api/v1/posts/{postId}/thread          # Full thread chain
POST   /api/v1/posts/{postId}/like            # Like
DELETE /api/v1/posts/{postId}/like            # Unlike
POST   /api/v1/posts/{postId}/repost          # Repost / Quote repost
POST   /api/v1/posts/{postId}/bookmark        # Bookmark
DELETE /api/v1/posts/{postId}/bookmark        # Remove bookmark
POST   /api/v1/posts/{postId}/report          # Report
GET    /api/v1/posts/feed/home                # Home feed (followers posts, paginated)
GET    /api/v1/posts/feed/explore             # Trending/explore feed
GET    /api/v1/users/{userId}/posts           # User's post timeline
GET    /api/v1/users/{userId}/likes           # Posts liked by user
GET    /api/v1/users/{userId}/bookmarks       # Bookmarked posts
```

---

### SERVICE 4: `comment-service` (Spring Boot)

**Port:** 8084  
**Database:** **Cassandra** (high write/read throughput for nested comments)  
**Cache:** Redis (hot comment threads TTL 2min)  
**Events Published (Kafka):** `comment.created`, `comment.liked`, `comment.reposted`, `comment.reported`, `comment.deleted`

**Cassandra Schema:**

```cql
CREATE TABLE comments_by_post (
  post_id UUID, comment_id TIMEUUID, parent_comment_id UUID,
  author_id UUID, content TEXT,
  like_count COUNTER, reply_count COUNTER,
  status TEXT,  -- ACTIVE, HIDDEN, DELETED
  is_deleted BOOLEAN, deleted_at TIMESTAMP,
  media_ids LIST<UUID>,
  created_at TIMESTAMP,
  PRIMARY KEY (post_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

CREATE TABLE comments_by_parent (
  parent_comment_id UUID, comment_id TIMEUUID, post_id UUID,
  author_id UUID, content TEXT, created_at TIMESTAMP,
  PRIMARY KEY (parent_comment_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);
```

**API Endpoints:**

```
POST   /api/v1/posts/{postId}/comments          # Create comment
GET    /api/v1/posts/{postId}/comments          # Get top-level comments (paginated cursor)
GET    /api/v1/comments/{commentId}/replies     # Get nested replies
POST   /api/v1/comments/{commentId}/like        # Like comment
DELETE /api/v1/comments/{commentId}/like        # Unlike
POST   /api/v1/comments/{commentId}/report      # Report
DELETE /api/v1/comments/{commentId}             # Soft delete
```

---

### SERVICE 5: `notification-service` (Spring Boot)

**Port:** 8085  
**Database:** **Cassandra** (append-only, high fan-out writes, multi-device sync)  
**Real-time delivery:** Reactive WebSocket (`starter-websocket`)  
**Events Consumed (Kafka):** `post.created`, `post.liked`, `post.reposted`, `comment.created`, `comment.liked`, `user.followed`, `user.verified`, `media.upload.completed`  
**Axon Events Consumed:** `UserPreferencesUpdatedEvent` → update notification delivery rules

**Cassandra Schema:**

```cql
CREATE TABLE notifications_by_user (
  user_id UUID, notification_id TIMEUUID,
  type TEXT,         -- LIKE, COMMENT, FOLLOW, MENTION, REPOST, SYSTEM
  actor_id UUID, actor_username TEXT,
  entity_type TEXT, entity_id UUID,  -- POST, COMMENT, etc.
  message TEXT, deep_link TEXT,
  is_read BOOLEAN DEFAULT FALSE,
  is_deleted BOOLEAN DEFAULT FALSE,
  delivered_at TIMESTAMP, created_at TIMESTAMP,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC);

-- device_sessions for WebSocket/push routing
CREATE TABLE device_sessions (
  user_id UUID, device_id UUID,
  session_token TEXT, platform TEXT,
  last_active TIMESTAMP,
  PRIMARY KEY (user_id, device_id)
);
```

**Multi-device sync:** When a user reads notifications on one device, publish `notification.read` to Kafka. All WebSocket sessions for that user consume and push read-state update.

**API Endpoints:**

```
GET    /api/v1/notifications                    # Paginated (cursor) notifications
POST   /api/v1/notifications/read-all           # Mark all as read
PUT    /api/v1/notifications/{id}/read          # Mark one as read
DELETE /api/v1/notifications/{id}               # Delete notification (soft)
WS     /ws/notifications                        # WebSocket for real-time push
```

---

### SERVICE 6: `search-service` (Spring Boot)

**Port:** 8086  
**Database:** Elasticsearch (primary), Kafka consumers for CDC sync  
**Cache:** Redis (search result cache per query hash, TTL 30s)

**Index Definitions:**

- `users_index`: username, displayName, bio, isVerified, followerCount, avatarUrl
- `posts_index`: content, authorId, authorUsername, createdAt, likeCount, repostCount, tags, mentionedUserIds
- `hashtags_index`: tag, postCount, trendingScore, updatedAt

**Sync Strategy:**

- `post-service`, `user-service`, `comment-service` publish domain events to Kafka.
- `search-service` consumes events and performs reactive Elasticsearch upsert/delete.
- Bulk indexing via `BulkRequest` reactor pipeline.

**API Endpoints:**

```
GET    /api/v1/search?q=&type=ALL|USERS|POSTS|HASHTAGS&page=&size=
GET    /api/v1/search/trending/hashtags     # Top trending hashtags
GET    /api/v1/search/suggestions?q=        # Autocomplete (prefix query)
GET    /api/v1/search/users?q=              # User search
GET    /api/v1/search/posts?q=              # Post full-text search
```

---

## PYTHON SERVICES SPECIFICATION

---

### PYTHON SERVICE 1: `post-guard-service` (FastAPI)

**Port:** 8090  
**Purpose:** Content moderation for posts before publishing.  
**Models:** Fine-tuned BERT for spam/phishing/TOS violation classification.  
**RAG:** ChromaDB stores known violation patterns; similarity search on new content.

**Endpoints:**

```
POST /api/v1/guard/post          # Input: {postId, content, authorId} → {decision: APPROVED|FLAGGED|REJECTED, reason, confidence}
POST /api/v1/guard/batch         # Batch check for backfill
GET  /api/v1/guard/model/status  # Model version & last trained date
POST /api/v1/guard/model/refresh # Trigger model fine-tune with latest flagged data
```

**Model Refresh:**

- Scheduled daily via APScheduler: fetch new labeled data from PostgreSQL, fine-tune BERT, update ChromaDB vectors.
- Publish model refresh event to Kafka `ai.model.updated`.

---

### PYTHON SERVICE 2: `media-guard-service` (FastAPI)

**Port:** 8091  
**Purpose:** Detect NSFW, malware-embedded images, deepfakes.  
**Models:** CLIP + custom NSFW classifier, file magic-byte validator.

**Endpoints:**

```
POST /api/v1/guard/media         # Input: {mediaId, cloudinaryUrl} → {safe: bool, categories: [...], confidence}
POST /api/v1/guard/file-check    # Stream raw file bytes for magic-byte + entropy analysis
```

---

### PYTHON SERVICE 3: `user-analysis-service` (FastAPI)

**Port:** 8092  
**Purpose:** Analyze user behavior events from Kafka. Detect engagement patterns, anomalies, TOS violations, bot behavior. Feed recommendations.

**Kafka Topics Consumed:** All `user.behavior.*` events (view, scroll, dwell-time, click, search, follow patterns)  
**Kafka Topics Published:** `ai.user.insights`, `ai.user.violation.suspected`, `ai.recommendation.ready`

**Models:**

- Anomaly detection: Isolation Forest / LSTM autoencoder.
- Bot detection: feature engineering on interaction velocity, session patterns.
- Recommendation signals: collaborative filtering embeddings published to Redis for `post-service` feed ranking.

**RAG:** User activity history chunked and stored in vector DB; on analysis request, retrieve relevant context.

**Endpoints:**

```
POST /api/v1/analysis/user/{userId}     # Trigger on-demand analysis
GET  /api/v1/analysis/user/{userId}     # Get latest analysis report
GET  /api/v1/analysis/user/{userId}/timeline  # Behavior timeline
POST /api/v1/analysis/model/refresh     # Retrain models
```

---

### PYTHON SERVICE 4: `ai-dashboard-service` (FastAPI)

**Port:** 8093  
**Purpose:** Aggregate all AI service data. Admin dashboard backend for platform health, moderation stats, behavior trends.

**Data Sources:** Reads from all AI service databases + Kafka aggregated topics.

**Endpoints:**

```
GET  /api/v1/dashboard/overview               # Platform-wide AI metrics
GET  /api/v1/dashboard/moderation/stats       # Post/media moderation stats (daily/weekly/monthly)
GET  /api/v1/dashboard/moderation/queue       # Flagged content pending review
PUT  /api/v1/dashboard/moderation/{id}/review # Human review decision
GET  /api/v1/dashboard/users/violations       # Suspected TOS violators
GET  /api/v1/dashboard/users/bots             # Bot detection results
GET  /api/v1/dashboard/models                 # All model versions & performance metrics
GET  /api/v1/dashboard/trends                 # Trending topics, behavior trends
WS   /ws/dashboard/live                       # Real-time metric stream
```

---

### SERVICE 7: `group-service` (Spring Boot)

**Port:** 8087  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (`@Cacheable` on group detail, membership, pinned posts; TTL per type)  
**Events Published (Kafka):** `group.created`, `group.updated`, `group.deleted`, `group.member.joined`, `group.member.left`, `group.member.role.changed`, `group.member.banned`, `group.post.pinned`  
**Events Consumed (Kafka):** `post.created`, `post.deleted`, `user.profile.updated`  
**Media integration:** Avatar & background images uploaded via `media-service` (same flow as user-service).  
**Post integration:** Posts inside a group are created through `post-service` (with `groupId` context field). `group-service` does NOT own post storage — it owns the group-to-post association and pinned-post list.

**Database Schema (Flyway):**

```sql
-- groups
id UUID PK DEFAULT gen_random_uuid(),
slug VARCHAR(100) UNIQUE NOT NULL,
name VARCHAR(150) NOT NULL,
description TEXT,
avatar_url TEXT,
background_url TEXT,
visibility VARCHAR(20) DEFAULT 'PUBLIC',  -- PUBLIC, PRIVATE, INVITE_ONLY
status VARCHAR(20) DEFAULT 'ACTIVE',      -- ACTIVE, SUSPENDED, ARCHIVED
owner_id UUID NOT NULL,                   -- user who created the group
member_count INT DEFAULT 0,
post_count INT DEFAULT 0,
tags TEXT[],                              -- searchable tags
category VARCHAR(60),
rules JSONB,                              -- [{title, description, order}]
policy JSONB,                             -- {autoApproveMembers, allowMemberPost, allowMemberInvite, requireJoinAnswers, minAccountAgeDays, ...}
settings JSONB,                           -- {theme, language, postApproval, ...}
is_deleted BOOLEAN DEFAULT FALSE, deleted_at TIMESTAMPTZ, deleted_by UUID,
created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ

-- group_members
group_id UUID REFERENCES groups(id),
user_id UUID NOT NULL,
role VARCHAR(20) DEFAULT 'MEMBER',        -- OWNER, ADMIN, MODERATOR, MEMBER
status VARCHAR(20) DEFAULT 'ACTIVE',      -- ACTIVE, BANNED, MUTED, PENDING_APPROVAL
join_answers JSONB,                       -- answers to join screening questions
invited_by UUID,
banned_by UUID, ban_reason TEXT, banned_at TIMESTAMPTZ,
joined_at TIMESTAMPTZ DEFAULT NOW(),
updated_at TIMESTAMPTZ,
PRIMARY KEY (group_id, user_id)

-- group_join_questions  (for private groups)
id UUID PK, group_id UUID, question TEXT, order_index INT,
is_required BOOLEAN DEFAULT TRUE, created_at TIMESTAMPTZ

-- group_pinned_posts
group_id UUID, post_id UUID, pinned_by UUID, pinned_at TIMESTAMPTZ,
order_index INT DEFAULT 0,
PRIMARY KEY (group_id, post_id)

-- group_invitations
id UUID PK, group_id UUID, inviter_id UUID, invitee_id UUID,
status VARCHAR(20) DEFAULT 'PENDING',     -- PENDING, ACCEPTED, DECLINED, EXPIRED
expires_at TIMESTAMPTZ, created_at TIMESTAMPTZ

-- group_join_requests  (for PRIVATE / INVITE_ONLY groups)
id UUID PK, group_id UUID, requester_id UUID,
answers JSONB, status VARCHAR(20) DEFAULT 'PENDING',
reviewed_by UUID, reviewed_at TIMESTAMPTZ, created_at TIMESTAMPTZ

-- group_member_activity  (audit log for admins)
id UUID PK, group_id UUID, actor_id UUID, target_id UUID,
action VARCHAR(50), detail JSONB, created_at TIMESTAMPTZ
```

**API Endpoints:**

```
POST   /api/v1/groups                                    Create group
GET    /api/v1/groups/{groupId}                          Get group detail (public info)
PUT    /api/v1/groups/{groupId}                          Update group (ADMIN+)
DELETE /api/v1/groups/{groupId}                          Soft delete (OWNER only)
PUT    /api/v1/groups/{groupId}/avatar                   Update avatar → media-service
PUT    /api/v1/groups/{groupId}/background               Update background → media-service
PUT    /api/v1/groups/{groupId}/rules                    Update rules list
PUT    /api/v1/groups/{groupId}/policy                   Update join policy
GET    /api/v1/groups/{groupId}/members                  Paginated member list
GET    /api/v1/groups/{groupId}/members/{userId}         Single member detail
PUT    /api/v1/groups/{groupId}/members/{userId}/role    Change member role (ADMIN+)
DELETE /api/v1/groups/{groupId}/members/{userId}         Remove member / leave
POST   /api/v1/groups/{groupId}/members/{userId}/ban     Ban member
DELETE /api/v1/groups/{groupId}/members/{userId}/ban     Unban member
POST   /api/v1/groups/{groupId}/members/{userId}/mute    Mute member
POST   /api/v1/groups/{groupId}/join                     Join group (or submit request)
POST   /api/v1/groups/{groupId}/leave                    Leave group
GET    /api/v1/groups/{groupId}/join-requests            Pending join requests (MODERATOR+)
PUT    /api/v1/groups/{groupId}/join-requests/{reqId}    Approve / reject request
POST   /api/v1/groups/{groupId}/invite                   Invite user (if policy allows)
GET    /api/v1/groups/{groupId}/invitations              List pending invitations (ADMIN+)
POST   /api/v1/groups/{groupId}/invitations/{invId}/respond  Accept / decline invitation
GET    /api/v1/groups/{groupId}/posts                    Posts in group (from post-service via Kafka index)
POST   /api/v1/groups/{groupId}/posts                    Create post in group (delegates to post-service with groupId)
GET    /api/v1/groups/{groupId}/posts/pinned             Pinned posts list
POST   /api/v1/groups/{groupId}/posts/{postId}/pin       Pin post (MODERATOR+)
DELETE /api/v1/groups/{groupId}/posts/{postId}/pin       Unpin post
GET    /api/v1/groups/{groupId}/join-questions           Join screening questions
PUT    /api/v1/groups/{groupId}/join-questions           Update questions
GET    /api/v1/groups/me/joined                          Groups current user joined
GET    /api/v1/groups/me/owned                           Groups current user owns
GET    /api/v1/groups/search?q=&tags=                    Search groups (delegates to search-service)
```

**Business Rules:**

- `OWNER` cannot leave — must transfer ownership first.
- `PRIVATE` / `INVITE_ONLY` groups: join triggers a `group_join_requests` entry; MODERATOR+ must approve. If `policy.requireJoinAnswers=true`, answers are mandatory.
- `PUBLIC` groups: instant join; if `policy.autoApproveMembers=false`, goes to pending.
- `policy.minAccountAgeDays`: verified at join time against user `created_at`.
- Pinned posts: max 5 pinned posts per group (enforced in service layer). Order is managed via `order_index`.
- Post in group: when creating a post in a group context, `post-service` receives a `groupId` field, which is stored in a `post_groups` join table managed by `group-service`. The group-service publishes `group.post.created` to allow search-service to index group posts separately.
- Member count updated atomically: `UPDATE groups SET member_count = member_count ± 1`.
- Rate limits: join request = 5/hour, create group = 3/day per user.

**Cache Keys:**

```
group:detail:{groupId}         TTL 5 min
group:members:count:{groupId}  TTL 1 min
group:pinned:{groupId}         TTL 2 min
group:membership:{userId}:{groupId}  TTL 5 min   # is member + role
```

---

### SERVICE 8: `private-message-service` (Spring Boot)

**Port:** 8088  
**Database:** **Cassandra** (append-heavy writes, time-sorted, multi-participant)  
**Cache:** Redis (conversation metadata TTL 5min, unread counts, participant lists)  
**WebSocket:** Reactive WebSocket (`starter-websocket`) for real-time message delivery  
**Events Published (Kafka):** `message.sent`, `message.delivered`, `message.read`, `message.reaction.added`, `message.reaction.removed`, `message.forwarded`, `message.deleted`, `conversation.created`, `conversation.settings.updated`  
**Events Consumed (Kafka):** `user.profile.updated` (update cached display name/avatar), `group.member.left` (remove from group conversation if tied to group)  
**Integration:** Calls `media-service` for media attachments in messages. Publishes message events to Kafka consumed by `message-notification-service`.

**Conversation Types:**

- `DIRECT` — exactly 2 participants (1-on-1 DM).
- `GROUP_CHAT` — 2–500 participants, user-created group chat (distinct from social groups).
- `GROUP_CHANNEL` — tied to a social `group-service` group; created automatically when a group is created (if group settings allow).

**Cassandra Schema:**

```cql
-- conversations (metadata stored in Redis + small Cassandra partition)
CREATE TABLE conversations (
  conversation_id UUID,
  type TEXT,                         -- DIRECT, GROUP_CHAT, GROUP_CHANNEL
  name TEXT,                         -- null for DIRECT
  avatar_url TEXT,
  group_id UUID,                     -- populated for GROUP_CHANNEL
  created_by UUID,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  settings TEXT,                     -- JSON: {muteUntil, theme, notifyOn, ...}
  is_deleted BOOLEAN,
  PRIMARY KEY (conversation_id)
);

-- conversation_participants
CREATE TABLE conversation_participants (
  conversation_id UUID,
  user_id UUID,
  role TEXT DEFAULT 'MEMBER',        -- OWNER, ADMIN, MEMBER
  status TEXT DEFAULT 'ACTIVE',      -- ACTIVE, LEFT, REMOVED, MUTED
  joined_at TIMESTAMP,
  last_read_message_id UUID,
  last_read_at TIMESTAMP,
  notification_settings TEXT,        -- JSON per-conversation notify config
  PRIMARY KEY (conversation_id, user_id)
);

-- conversations_by_user (reverse lookup: list all conversations for a user)
CREATE TABLE conversations_by_user (
  user_id UUID,
  last_message_at TIMESTAMP,
  conversation_id UUID,
  conversation_type TEXT,
  unread_count INT,
  is_muted BOOLEAN,
  PRIMARY KEY (user_id, last_message_at, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, conversation_id ASC);

-- messages
CREATE TABLE messages (
  conversation_id UUID,
  message_id TIMEUUID,
  sender_id UUID,
  message_type TEXT,                 -- TEXT, IMAGE, VIDEO, AUDIO, FILE, STICKER, FORWARDED, SYSTEM
  content TEXT,
  media_ids LIST<UUID>,
  forwarded_from_message_id UUID,
  forwarded_from_conversation_id UUID,
  reply_to_message_id UUID,          -- threaded reply
  status TEXT,                       -- SENT, DELIVERED, READ, FAILED, DELETED
  is_deleted BOOLEAN DEFAULT FALSE,
  deleted_at TIMESTAMP,
  deleted_by UUID,
  edited_at TIMESTAMP,
  metadata TEXT,                     -- JSON: {fileName, fileSize, duration, ...}
  created_at TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

-- message_reactions
CREATE TABLE message_reactions (
  conversation_id UUID,
  message_id TIMEUUID,
  user_id UUID,
  emoji TEXT,
  reacted_at TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);

-- message_read_receipts (per message per user)
CREATE TABLE message_read_receipts (
  conversation_id UUID,
  message_id TIMEUUID,
  user_id UUID,
  read_at TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);
```

**API Endpoints:**

```
-- Conversations
POST   /api/v1/messages/conversations                              Create DIRECT or GROUP_CHAT conversation
GET    /api/v1/messages/conversations                              List user's conversations (paginated, cursor by last_message_at)
GET    /api/v1/messages/conversations/{convId}                     Get conversation detail
PUT    /api/v1/messages/conversations/{convId}                     Update conversation (name, avatar — for GROUP_CHAT/GROUP_CHANNEL, ADMIN+)
DELETE /api/v1/messages/conversations/{convId}                     Leave / delete conversation (soft)
GET    /api/v1/messages/conversations/{convId}/participants        List participants
POST   /api/v1/messages/conversations/{convId}/participants        Add participant(s) to GROUP_CHAT
DELETE /api/v1/messages/conversations/{convId}/participants/{uid}  Remove participant
PUT    /api/v1/messages/conversations/{convId}/participants/{uid}/role  Change role
POST   /api/v1/messages/conversations/{convId}/mute               Mute conversation
POST   /api/v1/messages/conversations/{convId}/settings           Update per-conversation notify settings

-- Messages
POST   /api/v1/messages/conversations/{convId}/messages            Send message
GET    /api/v1/messages/conversations/{convId}/messages            Paginated messages (cursor by message_id)
PUT    /api/v1/messages/conversations/{convId}/messages/{msgId}    Edit message content (text only, within 15 min)
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}    Soft delete message
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/forward  Forward message to another conversation
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/react    Add/change reaction
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}/react    Remove reaction
POST   /api/v1/messages/conversations/{convId}/messages/read       Mark messages as read (up to messageId)
GET    /api/v1/messages/conversations/{convId}/messages/{msgId}/reactions  List reactions with users

-- WebSocket
WS     /ws/messages                                                Real-time message delivery
```

**WebSocket Protocol:**

```
Connect:  WS /ws/messages (gateway-forwarded auth headers)
Client → Server:
  JOIN_CONVERSATION   {conversationId}
  LEAVE_CONVERSATION  {conversationId}
  TYPING_START        {conversationId}
  TYPING_STOP         {conversationId}
  PING

Server → Client:
  NEW_MESSAGE         {conversationId, message}
  MESSAGE_UPDATED     {conversationId, messageId, content}
  MESSAGE_DELETED     {conversationId, messageId}
  MESSAGE_REACTION    {conversationId, messageId, emoji, userId, action: ADD|REMOVE}
  READ_RECEIPT        {conversationId, userId, lastReadMessageId}
  TYPING_INDICATOR    {conversationId, userId, typing: bool}
  PARTICIPANT_JOINED  {conversationId, userId}
  PARTICIPANT_LEFT    {conversationId, userId}
  PONG
```

**Business Rules:**

- Message delivery: on `NEW_MESSAGE` WS push, all online participants in the conversation receive it. Offline participants get delivery via `message-notification-service`.
- Typing indicator: ephemeral — published directly over WebSocket, NOT persisted.
- Reaction: 1 reaction per user per message; changing emoji overwrites previous. Reactions stored in Cassandra but NOT soft-deleted (physical delete allowed for reactions since they are not user-uploaded content).
- Forward: creates a new `message_type=FORWARDED` message in the target conversation, referencing `forwarded_from_message_id`. The original message is NOT copied — reference only.
- Edit: only allowed for `message_type=TEXT`, within 15 minutes of creation, by the sender only.
- Soft delete: message content replaced with `[Message deleted]` marker in response; `is_deleted=true` persisted.
- GROUP_CHANNEL conversations: automatically created when a group is created; linked via `group_id`. Group admins are conversation admins.
- Per-conversation settings (stored in `conversation_participants.notification_settings`):
  - `muteUntil`: timestamp, or `FOREVER`
  - `notifyOn`: `ALL_MESSAGES | MENTIONS_ONLY | NONE`
  - `theme`: color theme override
  - `nickname`: custom nickname for the other participant (DIRECT only)
- User-level message settings (stored in `user-service.notification_settings.message`):
  - `allowDmFrom`: `EVERYONE | FOLLOWING | NONE`
  - `readReceipts`: boolean
  - `typingIndicators`: boolean
  - `messagePreview`: boolean (for push notification content masking)

**Cache Keys:**

```
msg:conv:{convId}                  TTL 5 min   # conversation metadata
msg:participants:{convId}          TTL 5 min   # participant list
msg:unread:{userId}:{convId}       TTL 1 min   # unread count
msg:conv-list:{userId}:cursor:0    TTL 30s     # first page of conversations
```

---

### SERVICE 9: `message-notification-service` (Spring Boot)

**Port:** 8089  
**Database:** **Cassandra** (notification delivery log, device tokens)  
**Cache:** Redis (device token cache TTL 24h, notification delivery state)  
**Events Consumed (Kafka):** `message.sent`, `message.reaction.added`, `conversation.created`  
**Events Published (Kafka):** `message.notification.delivered`, `message.notification.failed`

**Purpose:**  
Dedicated push notification service for private messages. Separated from the general `notification-service` because message notifications require:

- Respect per-conversation and per-user message notification settings (fetched from `private-message-service`).
- Support for multiple push channels: WebSocket (online), FCM (mobile), APNs (iOS), Web Push.
- Message preview masking (`messagePreview=false` → send generic "New message" without content).
- Silent push for background sync on mobile.
- Batching & deduplication: collapse multiple rapid messages from same conversation into one push.

**Cassandra Schema:**

```cql
-- device_push_tokens (FCM / APNs / Web Push endpoints)
CREATE TABLE device_push_tokens (
  user_id UUID,
  device_id UUID,
  platform TEXT,           -- FCM_ANDROID, APNS_IOS, WEB_PUSH
  token TEXT,
  app_version TEXT,
  registered_at TIMESTAMP,
  last_active TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  PRIMARY KEY (user_id, device_id)
);

-- notification_delivery_log (for dedup, retry tracking)
CREATE TABLE message_notification_log (
  user_id UUID,
  notification_id TIMEUUID,
  conversation_id UUID,
  message_id UUID,
  channel TEXT,            -- WEBSOCKET, FCM, APNS, WEB_PUSH
  status TEXT,             -- PENDING, SENT, DELIVERED, FAILED, SKIPPED
  sent_at TIMESTAMP,
  failure_reason TEXT,
  retry_count INT DEFAULT 0,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 2592000;  -- 30 days TTL
```

**Notification Decision Flow:**

```
1. Consume Kafka: message.sent event
2. Load target participants (call private-message-service via WebClient or Redis cache)
3. For each offline/background participant:
   a. Load user-level message settings from Redis (cached from user-service)
      - allowDmFrom: skip if sender not allowed
      - readReceipts, typingIndicators: informational only
      - messagePreview: determines push body content
   b. Load per-conversation settings (from Redis cache seeded by private-message-service)
      - muteUntil: skip if muted
      - notifyOn: ALL_MESSAGES | MENTIONS_ONLY | NONE
   c. If allowed: build push payload
      - messagePreview=true  → "UserA: Hey, check this out..."
      - messagePreview=false → "New message from UserA"
      - Group chat: "GroupName: UserA sent a message"
   d. Batch notifications: if same conv sends 3+ messages in 5s, collapse to "N messages from..."
   e. Dispatch to FCM / APNs / Web Push (async, parallel)
   f. Log result to Cassandra notification_delivery_log
4. Failed deliveries: retry with exponential backoff (3 attempts); mark FAILED after max retries
5. Stale token handling: FCM/APNs unregistered token response → mark token is_active=false
```

**API Endpoints:**

```
POST   /api/v1/message-notifications/devices                     Register push token
PUT    /api/v1/message-notifications/devices/{deviceId}          Update push token
DELETE /api/v1/message-notifications/devices/{deviceId}          Deregister token (logout)
GET    /api/v1/message-notifications/devices                     List registered devices
GET    /api/v1/message-notifications/log                         Delivery log (paginated, last 30 days)
POST   /api/v1/message-notifications/test                        Send test push (dev/staging only)
GET    /api/v1/message-notifications/stats                       Delivery stats (admin only)
```

**Batching & Deduplication:**

- Sliding window dedup: if `notification_log` has PENDING/SENT for the same `(userId, conversationId)` within 5 seconds, collapse into count-based push update instead of individual pushes.
- Per-user batch queue implemented in Redis: `LPUSH msg:notif:batch:{userId}:{convId}` with 5s EXPIRE trigger.

**Cache Keys:**

```
msg:notif:settings:{userId}                TTL 5 min   # user-level message settings
msg:notif:conv-settings:{userId}:{convId}  TTL 5 min   # per-conv settings
msg:notif:token:{userId}                   TTL 24h     # device token list (serialized)
msg:notif:batch:{userId}:{convId}          TTL 5s      # batching window
```

---

## CI/CD PIPELINE SPECIFICATION

### Jenkins (CI)

Generate `infrastructure/jenkins/Jenkinsfile` (declarative pipeline) for **each service**:

```groovy
pipeline {
  agent { kubernetes { yaml POD_TEMPLATE } }
  environment {
    SERVICE_NAME = 'user-service'
    IMAGE_REPO = "your-registry.io/x-social/${SERVICE_NAME}"
    CHART_PATH = "infrastructure/helm/charts/${SERVICE_NAME}"
  }
  stages {
    stage('Checkout') { ... }
    stage('Build & Unit Test') {
      // mvn test -pl spring-services/services/user-service
      // Publish JaCoCo coverage report
    }
    stage('Integration Test') {
      // mvn failsafe:integration-test (Testcontainers)
    }
    stage('SonarQube Analysis') { ... }
    stage('Docker Build & Push') {
      // docker buildx build --platform linux/amd64,linux/arm64 -t ${IMAGE_REPO}:${GIT_COMMIT}
    }
    stage('Helm Lint & Template') {
      // helm lint + helm template dry-run
    }
    stage('Update GitOps Repo') {
      // Update image tag in helm/charts/<service>/values.yaml
      // git commit & push to GitOps repo → triggers ArgoCD
    }
  }
  post { always { junit 'target/surefire-reports/**/*.xml'; publishCoverage ... } }
}
```

### ArgoCD (CD)

Generate `infrastructure/argocd/application.yaml` per service:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: user-service
  namespace: argocd
spec:
  project: x-social
  source:
    repoURL: https://github.com/org/x-social-platform
    targetRevision: HEAD
    path: infrastructure/helm/charts/user-service
  destination:
    server: https://kubernetes.default.svc
    namespace: x-social
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions: [CreateNamespace=true]
```

---

## HELM CHART SPECIFICATION

Generate per-service Helm chart with the following structure:

```
helm/charts/user-service/
├── Chart.yaml
├── values.yaml           # Default values (non-secret)
├── values-prod.yaml      # Prod overrides
└── templates/
    ├── deployment.yaml   # With liveness/readiness probes, resource limits
    ├── service.yaml
    ├── hpa.yaml          # HorizontalPodAutoscaler (min 2, max 10)
    ├── pdb.yaml          # PodDisruptionBudget (minAvailable: 1)
    ├── configmap.yaml    # application.yaml mounted as ConfigMap
    ├── secret.yaml       # References K8S Secret for DB/Redis passwords
    ├── serviceaccount.yaml
    └── ingress.yaml      # Points to K8S gateway
```

HPA metrics: CPU > 70% OR custom metric `http_requests_per_second > 1000`.  
Resource requests: `cpu: 200m, memory: 256Mi`. Limits: `cpu: 1000m, memory: 1Gi`.

---

## OUTPUT INSTRUCTIONS

Generate output **service by service**, **module by module** in this order:

1. `common-core` (all source files)
2. All `starters` (all source files per starter)
3. `user-service` (complete: entities, repositories, services, handlers, routes, tests, Dockerfile, Helm)
4. `media-service` (same)
5. `post-service` (same)
6. `comment-service` (same)
7. `notification-service` (same)
8. `search-service` (same)
9. `group-service` (same)
10. `private-message-service` (same)
11. `message-notification-service` (same)
12. `post-guard-service` FastAPI (same)
13. `media-guard-service` FastAPI (same)
14. `user-analysis-service` FastAPI (same)
15. `ai-dashboard-service` FastAPI (same)
16. Jenkins pipelines
17. ArgoCD manifests
18. Helm charts
19. `docker-compose.yml` (full local dev stack)
20. All documentation markdown files

For each file output, use the full relative path as a header comment.  
Never skip or abbreviate files. Generate complete, compilable, runnable code.  
Include all import statements, all annotations, all YAML keys.

---

## CHECKLIST (Verify before completing each service)

- [ ] 100% reactive (no `block()`, no synchronous JDBC)
- [ ] Soft delete only for user data
- [ ] Redis caching with `@Cacheable` / `@CacheEvict`
- [ ] Rate limiter on write/public endpoints
- [ ] Kafka producer + consumer wired
- [ ] OpenAPI annotations on all endpoints
- [ ] Structured logging with MDC (traceId, userId)
- [ ] Micrometer metrics (`@Timed`, custom counters)
- [ ] Zipkin tracing configured
- [ ] Unit tests (mock everything)
- [ ] Integration tests (Testcontainers: postgres/cassandra/redis/kafka/elasticsearch)
- [ ] Automation tests (WebTestClient full request/response)
- [ ] Dockerfile (extracted JAR / Python multistage)
- [ ] Helm chart (deployment, HPA, PDB, configmap, secret)
- [ ] Flyway/CQL migration scripts
- [ ] README and service markdown doc
