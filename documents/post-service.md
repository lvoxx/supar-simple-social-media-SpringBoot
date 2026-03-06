# post-service

**Type:** Spring Boot · Port `8083`  
**Primary DB:** PostgreSQL (R2DBC) — schema `x_social_posts`  
**Cache:** Redis  
**Starters:** `postgres-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter`

---

## Responsibilities

Core posting engine. Post lifecycle, interactions (like, repost, bookmark, report), home/explore feeds, group-context posts, and system auto-posts. Every post in a group routes through this service.

---

## DB init

K8S `Job` runs Flyway CLI.  
Scripts: `infrastructure/k8s/db-init/post-service/sql/`

---

## Schema

```sql
-- posts
id               UUID        PRIMARY KEY DEFAULT gen_random_uuid()
author_id        UUID        NOT NULL
group_id         UUID                          -- NULL when not in a group
content          TEXT
reply_to_id      UUID                          -- no FK, app-enforced
repost_of_id     UUID                          -- no FK, app-enforced
post_type        VARCHAR(20) NOT NULL          -- ORIGINAL|REPLY|REPOST|QUOTE|AUTO
status           VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'
                                               -- DRAFT|PENDING_MEDIA|PUBLISHED|PENDING_REVIEW|HIDDEN|DELETED
visibility       VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
like_count       INT         NOT NULL DEFAULT 0
repost_count     INT         NOT NULL DEFAULT 0
reply_count      INT         NOT NULL DEFAULT 0
bookmark_count   INT         NOT NULL DEFAULT 0
view_count       BIGINT      NOT NULL DEFAULT 0
is_edited        BOOLEAN     NOT NULL DEFAULT FALSE
edited_at        TIMESTAMPTZ
is_pinned        BOOLEAN     NOT NULL DEFAULT FALSE
created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
updated_at       TIMESTAMPTZ
created_by       UUID
updated_by       UUID
is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE
deleted_at       TIMESTAMPTZ
deleted_by       UUID

-- post_media  (no FK — app-enforced)
post_id          UUID        NOT NULL
media_id         UUID        NOT NULL
position         INT         NOT NULL DEFAULT 0
PRIMARY KEY (post_id, media_id)

-- post_likes
post_id          UUID        NOT NULL
user_id          UUID        NOT NULL
created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
PRIMARY KEY (post_id, user_id)

-- post_bookmarks
post_id          UUID        NOT NULL
user_id          UUID        NOT NULL
created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
PRIMARY KEY (post_id, user_id)

-- post_hashtags
post_id          UUID        NOT NULL
hashtag          VARCHAR(100) NOT NULL
PRIMARY KEY (post_id, hashtag)

-- post_mentions
post_id          UUID        NOT NULL
mentioned_user_id UUID       NOT NULL
PRIMARY KEY (post_id, mentioned_user_id)

-- post_edits  (audit, append-only)
id               UUID        PRIMARY KEY DEFAULT gen_random_uuid()
post_id          UUID        NOT NULL
previous_content TEXT
edited_at        TIMESTAMPTZ NOT NULL
edited_by        UUID        NOT NULL

-- post_reports
id               UUID        PRIMARY KEY DEFAULT gen_random_uuid()
post_id          UUID        NOT NULL
reporter_id      UUID        NOT NULL
reason           VARCHAR(50)
status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

---

## Interaction counter strategy

```
On like / repost:
  Redis INCR  post:like-count:{postId}

Batch flush job every 30 s:
  UPDATE posts SET like_count = <redis value>

Result: DB never hit on viral-post bursts; Redis absorbs all writes.
```

---

## Feed algorithms

**Home feed** — posts from followed users ordered by `created_at DESC`, cursor pagination.  
Cached in Redis Sorted Set `feed:home:{userId}` TTL 30 s.

**Explore feed** — scored:
```
score = (like_count × 2 + repost_count × 3 + reply_count)
        / (EXTRACT(EPOCH FROM NOW() - created_at) / 3600 + 2)
```
Cached as `feed:explore:global` TTL 1 min.

---

## Kafka

### Published

`post.created` · `post.updated` · `post.deleted` · `post.liked` · `post.reposted` · `post.bookmarked` · `post.reported`

### Consumed

| Topic | Action |
|-------|--------|
| `media.upload.completed` | Promote `PENDING_MEDIA` post → `PUBLISHED` |
| `media.upload.failed` | Mark post `MEDIA_FAILED` |
| `user.avatar.changed` | Create `AUTO` post (if user setting allows) |
| `user.background.changed` | Create `AUTO` post (if user setting allows) |

---

## API

```
POST   /api/v1/posts
GET    /api/v1/posts/{postId}
PUT    /api/v1/posts/{postId}
DELETE /api/v1/posts/{postId}
GET    /api/v1/posts/{postId}/thread

POST   /api/v1/posts/{postId}/like
DELETE /api/v1/posts/{postId}/like
POST   /api/v1/posts/{postId}/repost
DELETE /api/v1/posts/{postId}/repost
POST   /api/v1/posts/{postId}/bookmark
DELETE /api/v1/posts/{postId}/bookmark
POST   /api/v1/posts/{postId}/report
POST   /api/v1/posts/{postId}/view

GET    /api/v1/posts/feed/home
GET    /api/v1/posts/feed/explore
GET    /api/v1/users/{userId}/posts
GET    /api/v1/users/{userId}/likes
GET    /api/v1/users/{userId}/bookmarks
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `post:detail:{postId}` | 5 min |
| `post:like-count:{postId}` | flushed every 30 s |
| `feed:home:{userId}` | 30 s |
| `feed:explore:global` | 1 min |
| `post:liked-by:{userId}:{postId}` | 5 min |

---

## Rate limits

| Endpoint | Limit |
|----------|-------|
| `POST /posts` | 30/hour per userId |
| `POST /{postId}/report` | 10/day per userId |

---

## Tests

- **Unit:** `PostServiceImplTest`, `FeedServiceTest`, `CounterFlushJobTest`
- **Integration:** PostgreSQL + Kafka + WireMock (post-guard-service)
- **Automation:** create → like → repost → bookmark → feed verify → delete
