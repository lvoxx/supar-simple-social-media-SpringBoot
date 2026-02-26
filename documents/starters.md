# Starters — Module Reference

**Location:** `spring-services/starters/`  
**Type:** Spring Boot Auto-configuration Libraries  

Tất cả starters được đăng ký qua `spring.factories` / `@AutoConfiguration`. Services chỉ cần add dependency là được auto-configure.

---

## starter-postgres

**Path:** `spring-services/starters/starter-postgres`  
**Cung cấp:** R2DBC ConnectionFactory · R2dbcEntityTemplate · Flyway migration runner  
**Dùng bởi:** user-service · media-service · post-service · group-service  

### application.yaml

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
    out-of-order: false
```

### Beans auto-configured

```java
@Bean ConnectionFactory connectionFactory()           // R2DBC pool
@Bean R2dbcEntityTemplate r2dbcEntityTemplate()       // reactive template
@Bean ReactiveAuditorAware<UUID> auditorAware()       // createdBy / updatedBy từ security context
@Bean Flyway flyway()                                  // migration runner (blocking, chỉ run khi start)
```

---

## starter-cassandra

**Path:** `spring-services/starters/starter-cassandra`  
**Cung cấp:** Reactive CqlSession · ReactiveCassandraTemplate · keyspace init  
**Dùng bởi:** comment-service · notification-service · private-message-service · message-notification-service  

### application.yaml

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
      serial-consistency: LOCAL_SERIAL
      page-size: 20
    connection:
      connect-timeout: 5s
      init-query-timeout: 10s
    pool:
      local:
        size: 1
```

### Beans auto-configured

```java
@Bean CqlSession cqlSession()
@Bean ReactiveCassandraTemplate reactiveCassandraTemplate()
@Bean CassandraConverter cassandraConverter()
```

---

## starter-redis

**Path:** `spring-services/starters/starter-redis`  
**Cung cấp:** ReactiveRedisTemplate · RedissonClient · RateLimiterService · RedisCacheManager  
**Dùng bởi:** Tất cả Spring Boot services  

### application.yaml

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
          time-between-eviction-runs: 60s
  cache:
    type: redis
    redis:
      time-to-live: 300s
      cache-null-values: false
      key-prefix: "xsocial:"

redisson:
  single-server-config:
    address: redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    connection-pool-size: 16
    connection-minimum-idle-size: 4
    idle-connection-timeout: 10000
    connect-timeout: 3000

xsocial:
  rate-limit:
    enabled: true
    default-capacity: 100
    default-refill-tokens: 100
    default-refill-period: 1m
```

### Beans auto-configured

```java
@Bean ReactiveRedisTemplate<String, Object> reactiveRedisTemplate()
@Bean RedissonClient redissonClient()
@Bean RedisCacheManager cacheManager()
@Bean RateLimiterService rateLimiterService()    // Bucket4j + Redisson
```

### Cách dùng caching

```java
// Annotation-based (ưu tiên)
@Cacheable(value = "user:profile", key = "#userId")
public Mono<UserResponse> findById(String userId) { ... }

@CacheEvict(value = "user:profile", key = "#userId")
public Mono<UserResponse> updateProfile(String userId, ...) { ... }

@CachePut(value = "user:profile", key = "#result.id()")
public Mono<UserResponse> createUser(CreateUserRequest req) { ... }
```

### Cách dùng distributed lock

```java
@Autowired RedissonClient redissonClient;

RLock lock = redissonClient.getLock("lock:follow:" + userId + ":" + targetId);
Mono.fromCallable(() -> lock.tryLock(500, 5000, TimeUnit.MILLISECONDS))
    .flatMap(acquired -> acquired
        ? performFollow().doFinally(s -> lock.unlock())
        : Mono.error(new ConflictException("OPERATION_IN_PROGRESS")));
```

### Cách dùng rate limiter

```java
@WebFilter
@RequiredArgsConstructor
public class RateLimitFilter implements WebFilter {
    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String key = resolveKey(exchange);  // userId hoặc IP
        String endpoint = exchange.getRequest().getPath().value();
        
        return rateLimiterService.tryConsume(key, endpoint)
            .flatMap(allowed -> allowed
                ? chain.filter(exchange)
                : buildRateLimitResponse(exchange));
    }
}
```

---

## starter-kafka

**Path:** `spring-services/starters/starter-kafka`  
**Cung cấp:** ReactiveKafkaProducerTemplate · DLT config · retry policy  
**Dùng bởi:** Tất cả services có Kafka integration  

### application.yaml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
        spring.json.add.type.headers: false
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.xsocial.*"
        isolation.level: read_committed
    listener:
      ack-mode: MANUAL_IMMEDIATE
      concurrency: 3

xsocial:
  kafka:
    retry:
      max-attempts: 3
      initial-interval-ms: 200
      backoff-multiplier: 2.0
    dlt:
      enabled: true
      suffix: .DLT
```

### Event Envelope

```java
// Tất cả Kafka events phải wrap trong KafkaEventEnvelope
public record KafkaEventEnvelope<T>(
    String eventId,           // ULID
    String eventType,         // "post.created"
    String version,           // "1"
    Instant timestamp,
    String producerService,
    String correlationId,
    T payload
) {}
```

### Cách dùng producer

```java
@Service
@RequiredArgsConstructor
public class PostEventPublisher {
    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;

    public Mono<Void> publishPostCreated(Post post) {
        var event = KafkaEventEnvelope.of("post.created", "1", PostCreatedPayload.from(post));
        return kafkaTemplate
            .send("post.created", post.getId().toString(), event)
            .doOnSuccess(r -> log.info("Published post.created: {}", post.getId()))
            .doOnError(e -> log.error("Failed to publish post.created", e))
            .then();
    }
}
```

### Cách dùng consumer

```java
@Component
@RequiredArgsConstructor
public class PostEventConsumer {
    private final ReactiveKafkaConsumerTemplate<String, KafkaEventEnvelope<PostCreatedPayload>> consumer;

    @PostConstruct
    public void startConsuming() {
        consumer.receiveAutoAck()
            .doOnNext(record -> log.info("Received: {}", record.key()))
            .flatMap(record -> processEvent(record.value())
                .doOnError(e -> log.error("Error processing: {}", record.key(), e)))
            .subscribe();
    }
}
```

---

## starter-elasticsearch

**Path:** `spring-services/starters/starter-elasticsearch`  
**Cung cấp:** ReactiveElasticsearchClient · index lifecycle utilities  
**Dùng bởi:** search-service  

### application.yaml

```yaml
spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:}
    connection-timeout: 5s
    socket-timeout: 30s
```

### Beans auto-configured

```java
@Bean ReactiveElasticsearchClient reactiveElasticsearchClient()
@Bean ElasticsearchIndexManager indexManager()    // tiện ích create/delete/alias
```

---

## starter-metrics

**Path:** `spring-services/starters/starter-metrics`  
**Cung cấp:** Zipkin exporter · Prometheus registry · custom MDC filter · JVM/HTTP/DB metrics  
**Dùng bởi:** Tất cả services  

### application.yaml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
      show-components: when_authorized
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:0.1}   # 10% prod, 100% dev
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
  metrics:
    tags:
      service: ${spring.application.name}
      env: ${SPRING_PROFILES_ACTIVE:dev}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99

logging:
  pattern:
    console: >-
      {"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}","level":"%level",
       "service":"${spring.application.name}","traceId":"%X{traceId}",
       "spanId":"%X{spanId}","userId":"%X{userId}","message":"%msg"}%n
```

### MDC Filter (tự động áp dụng)

Mỗi request tự động thêm vào MDC (và log):

```
traceId    ← từ Zipkin
spanId     ← từ Zipkin
userId     ← từ header X-User-Id
requestId  ← tạo mới (ULID)
```

---

## starter-websocket

**Path:** `spring-services/starters/starter-websocket`  
**Cung cấp:** WebSocketHandlerAdapter · URL mapping · CORS config  
**Dùng bởi:** notification-service · private-message-service  

### application.yaml

```yaml
xsocial:
  websocket:
    allowed-origins: ${WS_ALLOWED_ORIGINS:*}
    heartbeat-interval: 25s
    connection-timeout: 60s
    max-text-message-size: 65536
    max-binary-message-size: 65536
```

### Cách dùng

```java
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler implements WebSocketHandler {
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        UserPrincipal user = extractUser(session);
        
        return session.send(
            notificationFlux(user.userId())
                .map(session::textMessage)
        ).and(
            session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(msg -> handleClientMessage(user, msg))
        );
    }
}
```

---

## starter-security

**Path:** `spring-services/starters/starter-security`  
**Cung cấp:** JWT claim extraction filter · UserPrincipal builder · @CurrentUser resolver  
**Dùng bởi:** Tất cả services  

### application.yaml

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

### Luồng hoạt động

```
K8S Gateway đã validate JWT → inject headers vào request:
  X-User-Id:    "01HXZ..."
  X-User-Roles: "USER,MODERATOR"
  X-Forwarded-For: "1.2.3.4"

starter-security WebFilter:
  1. Đọc headers
  2. Build UserPrincipal record
  3. Lưu vào Reactor Context
  4. Lưu vào MDC (cho logging)

Service controller:
  @GetMapping("/me")
  Mono<ApiResponse<UserResponse>> getMe(@CurrentUser UserPrincipal user) {
      // user.userId(), user.roles(), user.ip()
  }
  
  // Hoặc dùng ReactiveContextUtil:
  return ReactiveContextUtil.getCurrentUserId()
      .flatMap(userId -> userService.findById(userId));
```

### Beans auto-configured

```java
@Bean ClaimExtractionWebFilter claimFilter()       // WebFilter, order = -100
@Bean CurrentUserArgumentResolver currentUserResolver()
@Bean UserContextPropagationOperator contextOperator()  // propagate tới @Async
```

---

## Tóm tắt dependency theo service

| Service | postgres | cassandra | redis | kafka | elasticsearch | metrics | websocket | security |
|---------|:--------:|:---------:|:-----:|:-----:|:-------------:|:-------:|:---------:|:--------:|
| user-service | ✅ | | ✅ | ✅ | | ✅ | | ✅ |
| media-service | ✅ | | ✅ | ✅ | | ✅ | | ✅ |
| post-service | ✅ | | ✅ | ✅ | | ✅ | | ✅ |
| comment-service | | ✅ | ✅ | ✅ | | ✅ | | ✅ |
| notification-service | | ✅ | ✅ | ✅ | | ✅ | ✅ | ✅ |
| search-service | | | ✅ | ✅ | ✅ | ✅ | | ✅ |
| group-service | ✅ | | ✅ | ✅ | | ✅ | | ✅ |
| private-message-service | | ✅ | ✅ | ✅ | | ✅ | ✅ | ✅ |
| message-notification-service | | ✅ | ✅ | ✅ | | ✅ | | ✅ |
