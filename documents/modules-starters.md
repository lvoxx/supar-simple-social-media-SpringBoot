# Starters & Common-Core — Module Reference

## common-core

**Location:** `spring-services/common/common-core`  
**Purpose:** Shared library for all Spring Boot services. Zero business logic — only infrastructure concerns.

### Package Structure

```
com.xsocial.common.core/
├── exception/
│   ├── BusinessException           # Base runtime exception with error code
│   ├── ResourceNotFoundException   # 404
│   ├── ConflictException           # 409
│   ├── ForbiddenException          # 403
│   ├── ValidationException         # 422
│   └── ExternalServiceException    # 502/503 upstream failures
├── handler/
│   ├── GlobalErrorWebExceptionHandler  # Reactive WebExceptionHandler
│   └── ErrorResponse               # {code, message, details, timestamp}
├── message/
│   ├── MessageKeys                 # Constants: USER_NOT_FOUND, POST_DELETED, etc.
│   └── AppMessageSource            # MessageSource wrapper
├── model/
│   ├── AuditableEntity             # createdAt, updatedAt, createdBy, updatedBy
│   ├── SoftDeletableEntity         # deletedAt, deletedBy, isDeleted
│   ├── PageResponse<T>             # {items, nextCursor, hasMore, total}
│   └── ApiResponse<T>              # {success, data, error, meta}
├── enums/
│   ├── UserRole                    # USER, MODERATOR, ADMIN, SYSTEM
│   ├── ContentStatus               # ACTIVE, HIDDEN, FLAGGED, DELETED
│   ├── MediaType                   # IMAGE, VIDEO, AUDIO, DOCUMENT
│   └── NotificationType            # LIKE, COMMENT, FOLLOW, MENTION, REPOST, SYSTEM
├── security/
│   ├── UserPrincipal               # record: userId, roles, ip
│   └── CurrentUser                 # @CurrentUser parameter annotation
├── util/
│   ├── UlidGenerator               # ULID-based UUID generation
│   ├── SlugUtil                    # username → URL slug
│   └── ReactiveContextUtil         # Extract UserPrincipal from Reactor Context
└── validation/
    └── ReactiveValidator           # Reactive bean validation (R2DBC-safe)
```

### Usage in other services

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.xsocial</groupId>
    <artifactId>common-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

```java
// Controller usage
@GetMapping("/users/{id}")
public Mono<ApiResponse<UserResponse>> getUser(
    @PathVariable String id,
    @CurrentUser UserPrincipal principal) {
    return userService.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("USER_NOT_FOUND", id)))
        .map(ApiResponse::success);
}
```

---

## starter-kafka

**Location:** `spring-services/starters/starter-kafka`  
**Auto-configured beans:** `ReactiveKafkaProducerTemplate`, DLT config, retry policy.

### application.yaml config

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
        spring.json.add.type.headers: false
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "com.xsocial.*"
    listener:
      ack-mode: MANUAL_IMMEDIATE
      concurrency: 3

xsocial:
  kafka:
    retry:
      max-attempts: 3
      backoff-multiplier: 2.0
      initial-interval-ms: 200
    dlt:
      enabled: true
```

### Usage

```java
@Service
@RequiredArgsConstructor
public class PostEventPublisher {
    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;

    public Mono<Void> publishPostCreated(PostCreatedEvent event) {
        return kafkaTemplate.send("post.created", event.getPostId(), event)
            .doOnSuccess(r -> log.info("Published post.created: {}", event.getPostId()))
            .then();
    }
}
```

---

## starter-redis

**Location:** `spring-services/starters/starter-redis`  
**Auto-configured beans:** `ReactiveRedisTemplate`, `RedissonClient`, `RateLimiterService`, `CacheManager`.

### application.yaml config

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
          min-idle: 2
  cache:
    type: redis
    redis:
      time-to-live: 300s
      cache-null-values: false

redisson:
  single-server-config:
    address: redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    connection-pool-size: 16

xsocial:
  rate-limit:
    enabled: true
    default-capacity: 100
    default-refill-tokens: 100
    default-refill-period: 1m
```

### Usage

```java
// Caching with annotations (starter enables @EnableCaching)
@Service
public class UserServiceImpl implements UserService {

    @Cacheable(value = "user:profile", key = "#userId", unless = "#result == null")
    public Mono<UserResponse> findById(String userId) { ... }

    @CacheEvict(value = "user:profile", key = "#userId")
    public Mono<UserResponse> updateProfile(String userId, UpdateProfileRequest req) { ... }
}

// Distributed lock (Redisson)
@Service
@RequiredArgsConstructor
public class FollowService {
    private final RedissonClient redissonClient;

    public Mono<Void> follow(String followerId, String targetId) {
        String lockKey = "lock:follow:" + followerId + ":" + targetId;
        RLock lock = redissonClient.getLock(lockKey);
        return Mono.fromCallable(() -> lock.tryLock(500, 5000, TimeUnit.MILLISECONDS))
            .flatMap(acquired -> acquired
                ? performFollow(followerId, targetId).doFinally(s -> lock.unlock())
                : Mono.error(new ConflictException("FOLLOW_IN_PROGRESS", targetId)));
    }
}

// Rate limiting
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements WebFilter {
    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        return rateLimiterService.tryConsume(userId, exchange.getRequest().getPath().value())
            .flatMap(allowed -> allowed
                ? chain.filter(exchange)
                : Mono.error(new RateLimitExceededException()));
    }
}
```

---

## starter-elasticsearch

**Location:** `spring-services/starters/starter-elasticsearch`  
**Auto-configured beans:** `ReactiveElasticsearchClient`, index lifecycle utilities.

### application.yaml config

```yaml
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:}
    connection-timeout: 5s
    socket-timeout: 30s
```

---

## starter-metrics

**Location:** `spring-services/starters/starter-metrics`  
**Auto-configured:** Zipkin exporter, Prometheus, JVM metrics, HTTP metrics, DB metrics, custom MDC filter.

### application.yaml config

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:0.1}
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

---

## starter-postgres

**Location:** `spring-services/starters/starter-postgres`  
**Auto-configured:** R2DBC `ConnectionFactory`, `R2dbcEntityTemplate`, Flyway runner.

### application.yaml config

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
      max-acquire-time: 5s
      validation-query: SELECT 1
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    user: ${DB_USER}
    password: ${DB_PASSWORD}
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

---

## starter-cassandra

**Location:** `spring-services/starters/starter-cassandra`  
**Auto-configured:** Reactive `CqlSession`, `ReactiveCassandraTemplate`, keyspace init.

### application.yaml config

```yaml
spring:
  cassandra:
    contact-points: ${CASSANDRA_CONTACT_POINTS:localhost}
    port: ${CASSANDRA_PORT:9042}
    keyspace-name: ${CASSANDRA_KEYSPACE:x_social}
    local-datacenter: datacenter1
    username: ${CASSANDRA_USER:cassandra}
    password: ${CASSANDRA_PASSWORD:cassandra}
    schema-action: CREATE_IF_NOT_EXISTS
    request:
      timeout: 10s
      consistency: LOCAL_QUORUM
      page-size: 20
    connection:
      connect-timeout: 5s
      init-query-timeout: 10s
```

---

## starter-websocket

**Location:** `spring-services/starters/starter-websocket`  
**Auto-configured:** `WebSocketHandlerAdapter`, `SimpleUrlHandlerMapping`, CORS config for WS.

### application.yaml config

```yaml
xsocial:
  websocket:
    allowed-origins: ${WS_ALLOWED_ORIGINS:*}
    heartbeat-interval: 25s
    connection-timeout: 60s
```

---

## starter-security

**Location:** `spring-services/starters/starter-security`  
**Purpose:** Extract JWT claims from gateway-forwarded headers. NO auth validation (handled by K8S gateway).  
**Auto-configured:** `UserPrincipalFilter` (WebFilter), `ReactiveSecurityContextHolder` setup, `@CurrentUser` resolver.

### What it does

```
Incoming request headers (injected by K8S gateway):
  X-User-Id: 01HXZ...
  X-User-Roles: USER,MODERATOR
  X-Forwarded-For: 1.2.3.4

starter-security filter:
  1. Reads these headers
  2. Builds UserPrincipal record
  3. Stores in Reactor Context

Service controller:
  @GetMapping("/me")
  Mono<Response> getMe(@CurrentUser UserPrincipal user) {
      // user.userId(), user.roles(), user.ip()
  }
```

### application.yaml config

```yaml
xsocial:
  security:
    user-id-header: X-User-Id
    roles-header: X-User-Roles
    ip-header: X-Forwarded-For
    anonymous-paths:
      - /actuator/**
      - /v3/api-docs/**
      - /swagger-ui/**
```
