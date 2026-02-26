# Conventions & Engineering Rules — X Social Platform

## 1. Project Structure Conventions

### Spring Boot Service Layout

```
<service-name>/
├── src/
│   ├── main/
│   │   ├── java/com/xsocial/<service>/
│   │   │   ├── config/          # Spring @Configuration beans
│   │   │   ├── domain/
│   │   │   │   ├── entity/      # R2DBC / Cassandra entities
│   │   │   │   ├── repository/  # ReactiveCrudRepository
│   │   │   │   ├── event/       # Domain event POJOs
│   │   │   │   └── command/     # Axon commands (if applicable)
│   │   │   ├── application/
│   │   │   │   ├── service/     # Business logic (interface + impl)
│   │   │   │   ├── dto/         # Request/Response DTOs
│   │   │   │   └── mapper/      # DTO ↔ Entity mappers (MapStruct)
│   │   │   ├── infrastructure/
│   │   │   │   ├── kafka/       # Producers, consumers, listeners
│   │   │   │   ├── redis/       # Cache config, Redisson beans
│   │   │   │   └── external/    # WebClient adapters to other services
│   │   │   ├── web/
│   │   │   │   ├── router/      # WebFlux RouterFunction (functional)
│   │   │   │   ├── handler/     # HandlerFunction implementations
│   │   │   │   └── filter/      # WebFilter (rate limit, logging)
│   │   │   └── <ServiceName>Application.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       └── db/migration/    # Flyway scripts: V1__init.sql
│   └── test/
│       ├── java/com/xsocial/<service>/
│       │   ├── unit/
│       │   ├── integration/
│       │   └── automation/
│       └── resources/
│           └── application-test.yaml
├── Dockerfile
└── pom.xml
```

### FastAPI Service Layout

```
<service-name>/
├── app/
│   ├── main.py              # FastAPI app, lifespan, router registration
│   ├── config.py            # Pydantic Settings from env vars
│   ├── dependencies.py      # DI: db session, redis, current user
│   ├── domain/
│   │   ├── models.py        # SQLAlchemy / Pydantic models
│   │   └── schemas.py       # Request/response Pydantic schemas
│   ├── api/
│   │   └── v1/
│   │       └── routes/      # APIRouter per resource
│   ├── services/            # Business logic
│   ├── infrastructure/
│   │   ├── database.py      # asyncpg / motor connection
│   │   ├── redis.py         # aioredis
│   │   ├── kafka.py         # aiokafka consumer/producer
│   │   └── ml/              # Model loaders, inference
│   └── core/
│       ├── exceptions.py
│       └── middleware.py    # Logging, tracing, rate limit middleware
├── tests/
│   ├── unit/
│   ├── integration/
│   └── automation/
├── Dockerfile
└── requirements.txt
```

---

## 2. Naming Conventions

### Java / Spring Boot

| Element         | Convention                          | Example                             |
| --------------- | ----------------------------------- | ----------------------------------- |
| Package         | `io.github.lvoxx.<service>.<layer>` | `io.github.lvoxx.user.web.handler`  |
| Class           | PascalCase                          | `UserProfileHandler`                |
| Interface       | PascalCase, no prefix               | `UserService` (not `IUserService`)  |
| Implementation  | `<Interface>Impl`                   | `UserServiceImpl`                   |
| DTO             | Suffix `Request` / `Response`       | `CreatePostRequest`, `PostResponse` |
| Kafka topic     | snake_case with dots                | `user.profile.updated`              |
| Redis key       | colon-delimited                     | `user:profile:{userId}`             |
| DB table        | snake_case, plural                  | `user_profiles`, `post_likes`       |
| DB column       | snake_case                          | `created_at`, `is_deleted`          |
| Environment var | SCREAMING_SNAKE                     | `DB_HOST`, `REDIS_PASSWORD`         |

### Python / FastAPI

| Element         | Convention      | Example                       |
| --------------- | --------------- | ----------------------------- |
| Module          | snake_case      | `user_analysis.py`            |
| Class           | PascalCase      | `UserBehaviorAnalyzer`        |
| Function/method | snake_case      | `analyze_user_behavior`       |
| Pydantic model  | PascalCase      | `PostGuardRequest`            |
| Route path      | kebab-case      | `/api/v1/guard/post-check`    |
| Env var         | SCREAMING_SNAKE | `MODEL_PATH`, `KAFKA_BROKERS` |

---

## 3. API Design Rules

### Response Envelope

All endpoints return `ApiResponse<T>` from `common-core`:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "requestId": "01HXZ...",
    "timestamp": "2026-01-01T00:00:00Z",
    "version": "v1"
  }
}
```

Error response:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User with id '...' does not exist",
    "details": []
  },
  "meta": { ... }
}
```

### Pagination (Cursor-based for Cassandra, Offset for PostgreSQL)

```json
{
  "data": {
    "items": [...],
    "nextCursor": "01HXZ...",
    "hasMore": true,
    "total": null
  }
}
```

### Versioning

All APIs are prefixed `/api/v1/`. Breaking changes require new version `/api/v2/`.

### HTTP Status Codes

| Scenario              | Code |
| --------------------- | ---- |
| Success GET           | 200  |
| Created               | 201  |
| Accepted (async)      | 202  |
| No Content            | 204  |
| Bad Request           | 400  |
| Unauthorized          | 401  |
| Forbidden             | 403  |
| Not Found             | 404  |
| Conflict              | 409  |
| Unprocessable Entity  | 422  |
| Too Many Requests     | 429  |
| Internal Server Error | 500  |
| Service Unavailable   | 503  |

---

## 4. Reactive Programming Rules

**Spring Boot (Project Reactor):**

- **NEVER** call `.block()` in any service/handler/consumer.
- **NEVER** use `Thread.sleep()` — use `Mono.delay()`.
- Use `publishOn(Schedulers.boundedElastic())` only for blocking I/O (file I/O, subprocess calls). Always return to reactive scheduler after.
- Propagate `ReactorContext` (userId, traceId) via `Context` and `contextWrite()`.
- Error handling: use `.onErrorResume()`, `.onErrorMap()`, never swallow errors silently.
- Prefer `switchIfEmpty(Mono.error(...))` over null checks.

**FastAPI (asyncio):**

- All route handlers must be `async def`.
- Never call synchronous blocking code in async context — use `asyncio.to_thread()` if necessary.
- Use `async with` for database sessions, Redis connections.
- Background tasks (model inference, Kafka publish) use `BackgroundTasks` or `asyncio.create_task()`.

---

## 5. Data Rules

### Soft Delete

Every entity with user-uploaded data MUST have:

```sql
is_deleted     BOOLEAN DEFAULT FALSE NOT NULL,
deleted_at     TIMESTAMPTZ,
deleted_by     UUID
```

All repository queries MUST add `WHERE is_deleted = FALSE` by default.  
Implement a `SoftDeletableRepository` base interface in `common-core`.

### Auditing

Every entity MUST extend `AuditableEntity`:

```java
created_at   TIMESTAMPTZ DEFAULT NOW() NOT NULL
updated_at   TIMESTAMPTZ
created_by   UUID   -- keycloakUserId
updated_by   UUID
```

Automatically populated via R2DBC `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` with `ReactiveAuditorAware`.

### IDs

- Use **ULID** (Universally Unique Lexicographically Sortable Identifier) for all primary keys: time-sortable, UUID-compatible, no hotspot risk.
- Store as `UUID` in PostgreSQL, `TEXT` in Cassandra.
- Generate via `UlidGenerator` from `common-core`.

---

## 6. Testing Standards

### Coverage Requirements

| Type              | Minimum                                      |
| ----------------- | -------------------------------------------- |
| Unit tests        | 80% line coverage                            |
| Integration tests | All repository methods, all Kafka consumers  |
| Automation tests  | All API endpoints (happy path + error cases) |

### Testcontainers Setup

Each service's `BaseIntegrationTest`:

```java
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Container static GenericContainer<?> redis = new GenericContainer<>("redis:7");
    @Container static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6"));
    // ... dynamic property sources
}
```

### Test Naming

```java
// Format: methodName_givenCondition_expectedBehavior
@Test void createPost_givenValidRequest_returnsCreatedPost() { ... }
@Test void createPost_givenContentTooLong_returns422() { ... }
@Test void createPost_givenBannedContent_returnsRejected() { ... }
```

---

## 7. Logging Standards

### Log Format (JSON)

Every log entry must include:

```json
{
  "timestamp": "2026-01-01T00:00:00.000Z",
  "level": "INFO",
  "service": "user-service",
  "traceId": "...",
  "spanId": "...",
  "userId": "...",
  "requestId": "...",
  "message": "...",
  "context": {}
}
```

### Log Levels

| Level | When to use                                                   |
| ----- | ------------------------------------------------------------- |
| DEBUG | Internal state, cache hits/misses, query params               |
| INFO  | Request received, response sent, Kafka event published        |
| WARN  | Retried operation succeeded, degraded mode activated          |
| ERROR | Exception caught, external service failed, data inconsistency |

Never log: passwords, tokens, PII in plain text.

---

## 8. Kafka Event Schema

All Kafka events must follow this envelope:

```json
{
  "eventId": "01HXZ...",
  "eventType": "post.created",
  "version": "1",
  "timestamp": "2026-01-01T00:00:00Z",
  "producerService": "post-service",
  "correlationId": "...",
  "payload": { ... }
}
```

- Events are **versioned** (`version` field, Axon `@Revision`).
- Breaking schema changes require a new event type (e.g., `post.created.v2`).
- Each consumer group has its own DLT: `<topic>.DLT`.

---

## 9. Git Workflow

```
main         — production-ready, protected, requires PR + 2 reviews
develop      — integration branch, all features merge here first
feature/*    — feature branches from develop
bugfix/*     — bug fix branches
hotfix/*     — emergency fixes from main
release/*    — release preparation branches
```

**Commit message format (Conventional Commits):**

```
feat(post-service): add bookmark endpoint
fix(user-service): correct follower count decrement on unfollow
chore(starter-kafka): upgrade kafka client to 3.7
test(comment-service): add testcontainer integration tests
docs(architecture): update kafka topic list
```

**PR Rules:**

- No direct pushes to `main` or `develop`.
- All PRs require passing Jenkins CI (unit + integration tests).
- SonarQube quality gate must pass.
- At least 1 approval required for `develop`, 2 for `main`.

---

## 10. Configuration Hierarchy

```
Priority (highest to lowest):
1. Environment variables (K8S ConfigMap / Secret)
2. application-{profile}.yaml
3. application.yaml (defaults)
4. Starter auto-configuration defaults

Profiles:
  dev      — local development, all infra via Docker Compose
  test     — Testcontainers, in-memory where possible
  staging  — K8S staging cluster, reduced replicas
  prod     — K8S production cluster, full HA
```

**Secret management:** All credentials (`DB_PASSWORD`, `REDIS_PASSWORD`, `KAFKA_SASL_PASSWORD`) are injected via K8S Secrets referenced in Helm `values.yaml`. Never hardcode secrets in code or commit them to git.

---

## 11. OpenAPI Documentation Rules

Every endpoint must have:

```java
@Operation(
    summary = "Create a new post",
    description = "Publishes a new post after content moderation and media processing.",
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponse(responseCode = "201", description = "Post created successfully")
@ApiResponse(responseCode = "202", description = "Post accepted, pending media processing")
@ApiResponse(responseCode = "422", description = "Content rejected by moderation")
@ApiResponse(responseCode = "429", description = "Rate limit exceeded")
```

OpenAPI spec auto-generated and available at `/v3/api-docs` (JSON) and `/v3/api-docs.yaml` (YAML).  
Swagger UI available in `dev`/`staging` profiles only (disabled in `prod`).
