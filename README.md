# X Social Platform — Backend

> Production-ready reactive microservice backend modelled after X (Twitter).  
> Spring Boot 4.0.2 · FastAPI 0.133 · Kafka · Axon · Keycloak · Kubernetes

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Shared Modules](#shared-modules)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Database Initialisation](#database-initialisation)
- [CI/CD](#cicd)
- [Documentation Index](#documentation-index)

---

## Architecture

```
Internet
   │
   ▼
[Keycloak  OAuth2 / OIDC]   ← token issuer, user federation
   │ JWT
   ▼
[K8S Ingress + JWT Validation Gateway]
   │ injects: X-User-Id · X-User-Roles · X-Forwarded-For
   │
   ├──► user-service                  :8081  PostgreSQL
   ├──► media-service                 :8082  PostgreSQL
   ├──► post-service                  :8083  PostgreSQL
   ├──► comment-service               :8084  Cassandra
   ├──► notification-service          :8085  Cassandra
   ├──► search-service                :8086  Elasticsearch
   ├──► group-service                 :8087  PostgreSQL
   ├──► private-message-service       :8088  Cassandra
   ├──► message-notification-service  :8089  Cassandra
   ├──► post-guard-service            :8090  PostgreSQL   (FastAPI)
   ├──► media-guard-service           :8091  —            (FastAPI)
   ├──► user-analysis-service         :8092  PostgreSQL   (FastAPI)
   └──► ai-dashboard-service          :8093  PostgreSQL   (FastAPI)

Shared infrastructure
  Apache Kafka · Axon Server · Redis (cache/lock only) · Zipkin · ELK · ArgoCD
```

### Design principles

- **Reactive everywhere** — Spring WebFlux / R2DBC; fully `async/await` FastAPI.
- **One primary database per service** — PostgreSQL *or* Cassandra *or* Elasticsearch. Redis is a cache/lock layer only. Services that would naturally mix two engines are split into separate microservices.
- **No foreign-key constraints** — referential integrity is enforced at the application layer.
- **No table relations across services** — cross-service data fetched via Kafka events or WebClient; never via shared DB.
- **DB init via Kubernetes** — all schema migrations and index mappings are applied by K8S Jobs / InitContainers. Services never run migrations.
- **Soft-delete only** — user-generated data is never physically deleted.
- **JWT validated at the gateway** — services read claims from forwarded headers; zero auth logic in individual services.

---

## Services

### Spring Boot Services

| Service | Port | Primary DB | Description |
|---------|------|-----------|-------------|
| [user-service](./services/user-service.md) | 8081 | PostgreSQL | Profiles, followers, account settings, Keycloak sync |
| [media-service](./services/media-service.md) | 8082 | PostgreSQL | Upload, processing pipeline, Cloudinary CDN |
| [post-service](./services/post-service.md) | 8083 | PostgreSQL | Posts, likes, reposts, bookmarks, feeds |
| [comment-service](./services/comment-service.md) | 8084 | Cassandra | Nested comments, high-throughput writes |
| [notification-service](./services/notification-service.md) | 8085 | Cassandra | Real-time push, multi-device read sync |
| [search-service](./services/search-service.md) | 8086 | Elasticsearch | Full-text search, trending, autocomplete |
| [group-service](./services/group-service.md) | 8087 | PostgreSQL | Groups, membership, roles, join screening |
| [private-message-service](./services/private-message-service.md) | 8088 | Cassandra | DM & group chat, reactions, forwarding |
| [message-notification-service](./services/message-notification-service.md) | 8089 | Cassandra | Push notifications — FCM, APNs, Web Push |

### FastAPI AI Services

| Service | Port | Primary DB | Description |
|---------|------|-----------|-------------|
| [post-guard-service](./services/post-guard-service.md) | 8090 | PostgreSQL | Content moderation — BERT + RAG |
| [media-guard-service](./services/media-guard-service.md) | 8091 | — | NSFW, malware, deepfake detection |
| [user-analysis-service](./services/user-analysis-service.md) | 8092 | PostgreSQL | Behaviour analysis, bot detection, recommendations |
| [ai-dashboard-service](./services/ai-dashboard-service.md) | 8093 | PostgreSQL | Admin AI dashboard, moderation queue |

---

## Shared Modules

Located under `spring-services/`:

| Module | Path | Description |
|--------|------|-------------|
| [common-core](./modules/common-core.md) | `common/common-core` | Exceptions, ApiResponse, enums, ULID, security context |
| [postgres-starter](./modules/starters.md#postgres-starter) | `starters/postgres-starter` | R2DBC connection pool, `ReactiveAuditorAware` |
| [cassandra-starter](./modules/starters.md#cassandra-starter) | `starters/cassandra-starter` | Reactive Cassandra session |
| [redis-starter](./modules/starters.md#redis-starter) | `starters/redis-starter` | ReactiveRedisTemplate, Redisson, rate limiter |
| [kafka-starter](./modules/starters.md#kafka-starter) | `starters/kafka-starter` | Reactive Kafka producer/consumer, DLT |
| [elasticsearch-starter](./modules/starters.md#elasticsearch-starter) | `starters/elasticsearch-starter` | Reactive ES client |
| [metrics-starter](./modules/starters.md#metrics-starter) | `starters/metrics-starter` | Micrometer, Zipkin, MDC filter |
| [websocket-starter](./modules/starters.md#websocket-starter) | `starters/websocket-starter` | Reactive WebSocket adapter |
| [security-starter](./modules/starters.md#security-starter) | `starters/security-starter` | JWT claim extraction, `@CurrentUser` |

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Spring Boot services | Spring Boot 4.0.2, Java 21, Maven multi-module |
| FastAPI services | FastAPI 0.133, Python 3.12 |
| Reactive runtime | Spring WebFlux, Project Reactor, asyncio |
| Auth | Keycloak (OAuth2 + OIDC) — validation at K8S Ingress only |
| Event bus | Apache Kafka (reactor-kafka / aiokafka) |
| CQRS / ES | Axon Framework |
| Relational DB | PostgreSQL 16 (R2DBC) |
| Wide-column DB | Apache Cassandra 4 (reactive) |
| Search | Elasticsearch 8 (reactive) |
| Cache / lock | Redis 7 + Redisson |
| Media CDN | Cloudinary |
| Vector store | ChromaDB / Qdrant (AI services only) |
| Tracing | Zipkin + OpenTelemetry |
| Metrics | Micrometer + Prometheus |
| Logging | Logback JSON + ELK Stack |
| Containers | Docker (extracted JAR + Python multi-stage) |
| Orchestration | Kubernetes + Helm |
| CI | Jenkins (declarative pipelines) |
| CD | ArgoCD (GitOps) |

---

## Getting Started

### Prerequisites

Docker ≥ 4 · Java 21 · Python 3.12 · Maven ≥ 3.9 · Helm ≥ 3.14 · kubectl

### Start infrastructure locally

```bash
# All infrastructure: Kafka, PostgreSQL, Cassandra, Elasticsearch, Redis, Keycloak, Zipkin
docker compose -f docker-compose.infra.yml up -d

# Apply all schemas (runs the same init containers locally)
./scripts/init-db-local.sh
```

### Build & run services

```bash
cd spring-services && mvn clean package -DskipTests
docker compose -f docker-compose.services.yml up
```

### Run tests

```bash
mvn test                                        # unit tests
mvn failsafe:integration-test                   # integration tests (needs Docker)
cd python-services/post-guard-service && pytest tests/ -v --cov=app
```

---

## Database Initialisation

**Services never initialise their own schemas.**  
All DDL / CQL / index mappings are applied by Kubernetes resources before the service Pod starts.

| Engine | K8S resource | Tool | Scripts location |
|--------|-------------|------|-----------------|
| PostgreSQL | `Job` | Flyway CLI (`flyway/flyway:10`) | `infrastructure/k8s/db-init/<service>/sql/` |
| Cassandra | `InitContainer` | `cqlsh` (`cassandra:4.1`) | `infrastructure/k8s/db-init/<service>/cql/` |
| Elasticsearch | `Job` | `curl` (`curlimages/curl:8`) | `infrastructure/k8s/db-init/<service>/mappings/` |

The Spring Boot `application.yaml` for Cassandra services always sets `spring.cassandra.schema-action: NONE`.  
PostgreSQL services do not include any `flyway.*` or `spring.flyway.*` configuration.

---

## CI/CD

### Jenkins (CI) — `infrastructure/jenkins/`
Stages: `Checkout` → `Build & Unit Test` → `Integration Test` → `SonarQube` → `Docker Build & Push` → `Helm Lint` → `Update GitOps Repo`

### ArgoCD (CD) — `infrastructure/argocd/`
Watches `infrastructure/helm/charts/`. Automated sync with pruning and self-healing on image tag commit.

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Full system design, data flows, HA |
| [CONVENTIONS.md](./CONVENTIONS.md) | Coding rules, naming, Git workflow |
| [services/user-service.md](./services/user-service.md) | user-service |
| [services/media-service.md](./services/media-service.md) | media-service |
| [services/post-service.md](./services/post-service.md) | post-service |
| [services/comment-service.md](./services/comment-service.md) | comment-service |
| [services/notification-service.md](./services/notification-service.md) | notification-service |
| [services/search-service.md](./services/search-service.md) | search-service |
| [services/group-service.md](./services/group-service.md) | group-service |
| [services/private-message-service.md](./services/private-message-service.md) | private-message-service |
| [services/message-notification-service.md](./services/message-notification-service.md) | message-notification-service |
| [services/post-guard-service.md](./services/post-guard-service.md) | post-guard-service (FastAPI) |
| [services/media-guard-service.md](./services/media-guard-service.md) | media-guard-service (FastAPI) |
| [services/user-analysis-service.md](./services/user-analysis-service.md) | user-analysis-service (FastAPI) |
| [services/ai-dashboard-service.md](./services/ai-dashboard-service.md) | ai-dashboard-service (FastAPI) |
| [modules/common-core.md](./modules/common-core.md) | common-core shared library |
| [modules/starters.md](./modules/starters.md) | All starters reference |
