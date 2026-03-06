# comment-service

**Type:** Spring Boot · Port `8084`  
**Primary DB:** Apache Cassandra — keyspace `x_social_comments`  
**Cache:** Redis  
**Starters:** `cassandra-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter`

---

## Responsibilities

High-throughput comment and nested-reply management. Max nesting depth = 3. Uses Cassandra `COUNTER` columns for atomic like/reply counts without distributed locks.

---

## Why Cassandra

- Append-heavy writes (millions/day), no updates in hot path.
- `TIMEUUID` clustering key → natural cursor pagination without `OFFSET`.
- `COUNTER` type for `like_count` / `reply_count` is lock-free and atomic.
- Simple query patterns (by `post_id`, by `parent_comment_id`) — no JOINs.

---

## DB init

K8S `InitContainer` runs `cqlsh` before Pod starts.  
Scripts: `infrastructure/k8s/db-init/comment-service/cql/`  
`spring.cassandra.schema-action: NONE`

---

## Schema (keyspace `x_social_comments`)

```cql
-- Top-level and all-depth comments, partitioned by post
CREATE TABLE comments_by_post (
  post_id           UUID,
  comment_id        TIMEUUID,
  parent_comment_id UUID,
  author_id         UUID,
  author_username   TEXT,       -- denormalised from user-service
  content           TEXT,
  depth             INT      DEFAULT 0,
  status            TEXT     DEFAULT 'ACTIVE',
  is_deleted        BOOLEAN  DEFAULT FALSE,
  deleted_at        TIMESTAMP,
  deleted_by        UUID,
  media_ids         LIST<UUID>,
  created_at        TIMESTAMP,
  updated_at        TIMESTAMP,
  PRIMARY KEY (post_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

-- Replies lookup by parent
CREATE TABLE comments_by_parent (
  parent_comment_id UUID,
  comment_id        TIMEUUID,
  post_id           UUID,
  author_id         UUID,
  content           TEXT,
  is_deleted        BOOLEAN  DEFAULT FALSE,
  created_at        TIMESTAMP,
  PRIMARY KEY (parent_comment_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

-- Like dedup (was this user's like already recorded?)
CREATE TABLE comment_likes (
  comment_id UUID,
  user_id    UUID,
  post_id    UUID,
  liked_at   TIMESTAMP,
  PRIMARY KEY (comment_id, user_id)
);

-- Atomic counters (separate table — Cassandra requirement)
CREATE TABLE comment_counters (
  comment_id  UUID PRIMARY KEY,
  like_count  COUNTER,
  reply_count COUNTER
);
```

---

## Kafka published

| Topic | Consumers |
|-------|-----------|
| `comment.created` | notification-svc, search-svc, user-analysis-svc |
| `comment.liked` | notification-svc |
| `comment.reported` | ai-dashboard-svc |
| `comment.deleted` | search-svc |

---

## API

```
POST   /api/v1/posts/{postId}/comments
GET    /api/v1/posts/{postId}/comments        # cursor by comment_id
GET    /api/v1/comments/{commentId}/replies
PUT    /api/v1/comments/{commentId}           # edit within 15 min
DELETE /api/v1/comments/{commentId}
POST   /api/v1/comments/{commentId}/like
DELETE /api/v1/comments/{commentId}/like
POST   /api/v1/comments/{commentId}/report
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `comment:detail:{commentId}` | 2 min |
| `comment:liked:{userId}:{commentId}` | 5 min |

---

## Tests

- **Unit:** `CommentServiceTest`, `CommentLikeServiceTest`
- **Integration:** Cassandra + Kafka containers (Testcontainers)
- **Automation:** create → nested reply → like → edit → delete → paginate

---
---

# notification-service

**Type:** Spring Boot · Port `8085`  
**Primary DB:** Apache Cassandra — keyspace `x_social_notifications`  
**Cache:** Redis (Pub/Sub for cross-pod WS routing)  
**Starters:** `cassandra-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter` `websocket-starter`

---

## Responsibilities

Fan-out in-app notifications to users. Real-time delivery via reactive WebSocket. Multi-device read-state sync via Redis Pub/Sub across pods.

---

## DB init

K8S `InitContainer` runs `cqlsh`.  
Scripts: `infrastructure/k8s/db-init/notification-service/cql/`  
`spring.cassandra.schema-action: NONE`

---

## Schema (keyspace `x_social_notifications`)

```cql
CREATE TABLE notifications_by_user (
  user_id          UUID,
  notification_id  TIMEUUID,
  type             TEXT,
  actor_id         UUID,
  actor_username   TEXT,
  actor_avatar_url TEXT,
  entity_type      TEXT,
  entity_id        UUID,
  message          TEXT,
  deep_link        TEXT,
  is_read          BOOLEAN   DEFAULT FALSE,
  is_deleted       BOOLEAN   DEFAULT FALSE,
  delivered_at     TIMESTAMP,
  created_at       TIMESTAMP,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 7776000;    -- 90 days TTL

CREATE TABLE device_sessions (
  user_id     UUID,
  device_id   UUID,
  pod_id      TEXT,         -- K8S pod for WS routing
  platform    TEXT,
  last_active TIMESTAMP,
  PRIMARY KEY (user_id, device_id)
);
```

---

## WebSocket protocol

```
Connect: WS /ws/notifications

Server → Client:
  NOTIFICATION        { id, type, message, deepLink, actor, timestamp }
  READ_STATE_UPDATE   { notificationIds }
  UNREAD_COUNT_UPDATE { count }

Client → Server:
  PING
  MARK_READ { notificationId }
```

**Multi-device read sync:**
```
Device A marks read → Cassandra is_read=true
→ Publish Kafka: notification.read { userId, notificationId }
→ consumer pushes READ_STATE_UPDATE to all other user sessions via Redis Pub/Sub
```

---

## Kafka consumed

`post.liked` · `post.reposted` · `post.created` · `comment.created` · `comment.liked` · `user.followed` · `user.verified` · `media.upload.completed` · `notification.read` · `group.member.joined` · `group.post.pinned` · `conversation.created`

Axon: `UserPreferencesUpdatedEvent` → update per-user notification delivery rules

---

## API

```
GET    /api/v1/notifications
POST   /api/v1/notifications/read-all
PUT    /api/v1/notifications/{id}/read
GET    /api/v1/notifications/unread-count
DELETE /api/v1/notifications/{id}
WS     /ws/notifications
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `notif:unread:{userId}` | 5 min |
| `notif:settings:{userId}` | 10 min |
| `notif:ws-sessions:{userId}` | 30 s |

---

## Tests

- **Unit:** `NotificationFanoutServiceTest`, `UserPreferenceFilterTest`
- **Integration:** Cassandra + Redis + Kafka containers
- **Automation:** Kafka event → notification stored → WS push → mark read → multi-device sync

---
---

# search-service

**Type:** Spring Boot · Port `8086`  
**Primary DB:** Elasticsearch 8 — dedicated cluster  
**Cache:** Redis  
**Starters:** `elasticsearch-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter`

---

## Responsibilities

Full-text search (posts, users, hashtags, groups), autocomplete, trending hashtags. Maintains denormalised search indices fed by Kafka CDC events. **Owns only Elasticsearch — no PostgreSQL/Cassandra schema.**

---

## DB init

K8S `Job` calls ES REST API to create indices.  
Mapping files: `infrastructure/k8s/db-init/search-service/mappings/`

```
users_v1.json
posts_v1.json
hashtags_v1.json
groups_v1.json
```

---

## Index overview

| Index | Refresh | Primary query |
|-------|---------|---------------|
| `users_v1` | 1 s | Username, displayName, bio, verified |
| `posts_v1` | 1 s | Content, hashtags, mentions, groupId |
| `hashtags_v1` | 10 s | Tag name, trending score |
| `groups_v1` | 5 s | Name, description, tags, category |

---

## CDC sync (Kafka → ES)

Bulk flush every 5 s. Individually indexed writes are batched to avoid per-document overhead.

| Topic consumed | ES action |
|----------------|-----------|
| `post.created` | Index document |
| `post.updated` | Update document |
| `post.deleted` | Set `isDeleted=true` |
| `post.liked` | Script-update `likeCount` |
| `post.reposted` | Script-update `repostCount` |
| `user.profile.updated` | Update user document |
| `user.verified` | Set `isVerified=true` |
| `group.created` | Index document |
| `group.updated` | Update document |
| `group.deleted` | Set `isDeleted=true` |
| `group.post.created` | Tag post with `groupId` |
| `comment.created` | Index (if comment search enabled) |

---

## Trending hashtag algorithm

```
Every 10 s:
  raw  = Σ(like_count × 2 + repost_count × 3) for posts with this tag in last 24 h
  score = raw / (hours_since_first_use / 24 + 1)   ← time decay

Top-50 cached  Redis key: search:trending:hashtags  TTL 1 min
```

---

## API

```
GET  /api/v1/search?q=&type=ALL|USERS|POSTS|HASHTAGS|GROUPS&page=&size=
GET  /api/v1/search/trending/hashtags?limit=10&period=1h|6h|24h
GET  /api/v1/search/suggestions?q=
GET  /api/v1/search/users?q=&verified=
GET  /api/v1/search/posts?q=&from=&to=&authorId=&groupId=
GET  /api/v1/search/groups?q=&tags=&category=
POST /api/v1/search/admin/reindex         # ADMIN only
GET  /api/v1/search/admin/index-stats
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `search:trending:hashtags` | 1 min |
| `search:result:{queryHash}` | 30 s |
| `search:suggestions:{prefix}` | 1 min |

---

## Tests

- **Unit:** `SearchQueryBuilderTest`, `TrendingScoreCalculatorTest`, `BulkIndexPipelineTest`
