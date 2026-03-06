# user-service

**Type:** Spring Boot · Port `8081`  
**Primary DB:** PostgreSQL (R2DBC) — schema `x_social_users`  
**Cache:** Redis (cache/lock only)  
**Starters:** `postgres-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter`

---

## Responsibilities

User identity, profiles, social graph (follow / unfollow), account settings, verification flow, Keycloak synchronisation.

---

## DB init

A K8S `Job` runs Flyway CLI before the Pod starts. The service has no migration config.  
Scripts: `infrastructure/k8s/db-init/user-service/sql/`

```
V1__init_users.sql
V2__add_followers.sql
V3__add_verifications.sql
V4__add_account_history.sql
V5__add_follow_requests.sql
```

---

## Schema

No `REFERENCES` / `FOREIGN KEY`. No cross-table joins in hot paths.

```sql
-- users
id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid()
keycloak_id           UUID        UNIQUE NOT NULL
username              VARCHAR(50) UNIQUE NOT NULL
display_name          VARCHAR(100)
bio                   TEXT
avatar_url            TEXT
background_url        TEXT
website_url           TEXT
location              VARCHAR(100)
birth_date            DATE
is_verified           BOOLEAN     NOT NULL DEFAULT FALSE
is_private            BOOLEAN     NOT NULL DEFAULT FALSE
role                  VARCHAR(20) NOT NULL DEFAULT 'USER'
follower_count        INT         NOT NULL DEFAULT 0
following_count       INT         NOT NULL DEFAULT 0
post_count            INT         NOT NULL DEFAULT 0
status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
theme_settings        JSONB
notification_settings JSONB
account_settings      JSONB
created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
updated_at            TIMESTAMPTZ
created_by            UUID
updated_by            UUID
is_deleted            BOOLEAN     NOT NULL DEFAULT FALSE
deleted_at            TIMESTAMPTZ
deleted_by            UUID

-- followers  (no FK — app-enforced)
follower_id           UUID        NOT NULL
following_id          UUID        NOT NULL
created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
PRIMARY KEY (follower_id, following_id)

-- follow_requests
id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid()
requester_id          UUID        NOT NULL
target_id             UUID        NOT NULL
status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()

-- account_history  (append-only, no update)
id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid()
user_id               UUID        NOT NULL
action                VARCHAR(50) NOT NULL
detail                JSONB
ip                    INET
created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()

-- verifications
id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid()
user_id               UUID        NOT NULL
type                  VARCHAR(30)
status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
document_url          TEXT
reviewed_by           UUID
created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
updated_at            TIMESTAMPTZ
```

---

## Kafka

### Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `user.profile.updated` | Any profile field updated | search-svc, user-analysis-svc |
| `user.avatar.changed` | Avatar changed | post-svc (auto-post), search-svc |
| `user.background.changed` | Background changed | post-svc (auto-post) |
| `user.followed` | Follow relationship created | notification-svc, user-analysis-svc |
| `user.unfollowed` | Follow removed | notification-svc |
| `user.verified` | Admin approves verification | notification-svc |

### Consumed

*(None — inbound only via HTTP)*

---

## Axon

| Command | Broadcast to |
|---------|-------------|
| `UpdateUserPreferencesCommand` | `UserPreferencesUpdatedEvent` → notification-svc, post-svc, user-analysis-svc |

---

## API

```
GET    /api/v1/users/{username}
GET    /api/v1/users/me
PUT    /api/v1/users/me
PUT    /api/v1/users/me/avatar              → calls media-service
PUT    /api/v1/users/me/background          → calls media-service
PUT    /api/v1/users/me/settings
GET    /api/v1/users/me/history
POST   /api/v1/users/me/verify
GET    /api/v1/users/{userId}/followers
GET    /api/v1/users/{userId}/following
POST   /api/v1/users/{userId}/follow
DELETE /api/v1/users/{userId}/follow
GET    /api/v1/users/me/follow-requests
PUT    /api/v1/users/me/follow-requests/{reqId}
GET    /api/v1/users/search?q=
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `user:profile:{userId}` | 5 min |
| `user:profile:username:{username}` | 5 min |
| `user:followers:count:{userId}` | 1 min |
| `user:following:count:{userId}` | 1 min |
| `user:is-following:{a}:{b}` | 5 min |
| `user:settings:{userId}` | 10 min |

---

## Rate limits

| Endpoint | Limit |
|----------|-------|
| `POST /follow` | 50/hour per userId |
| `PUT /me` | 10/min per userId |
| `POST /me/verify` | 3/day per userId |

---

## Tests

- **Unit:** `UserServiceImplTest`, `FollowServiceTest`, `KeycloakAdminClientTest`
- **Integration (Testcontainers):** PostgreSQL + Redis + Kafka
- **Automation (WebTestClient):** register → update → follow → unfollow → verify history
