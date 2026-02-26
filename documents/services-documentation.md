# Service Documentation Index

---

# user-service

**Tech:** Spring Boot 4.0.2 · PostgreSQL (R2DBC) · Redis · Kafka · Axon · Keycloak Admin API  
**Port:** 8081  
**Module path:** `spring-services/services/user-service`

## Responsibilities

Manages all user identity, profile, social graph (follow/unfollow), account settings, and Keycloak synchronization.

## Key Features

- User registration synced with Keycloak (Admin REST API via WebClient).
- Profile: avatar, background image, bio, links, location, birth date.
- Social graph: followers / following with atomic counter updates (Redis distributed lock).
- Private accounts with follow-request approval flow.
- Account settings: notification preferences (per-service), theme, privacy.
- Account history: login events, password changes, profile updates (IP-tracked).
- Identity verification submission & review flow.
- When avatar/background changes: optionally auto-creates a post via Kafka `user.avatar.changed`.

## Starters Used

`starter-postgres`, `starter-redis`, `starter-kafka`, `starter-metrics`, `starter-security`, `starter-websocket` (for live settings sync)

## Kafka Events Published

| Topic | Trigger |
|-------|---------|
| `user.profile.updated` | Any profile field update |
| `user.avatar.changed` | Avatar image updated |
| `user.background.changed` | Background image updated |
| `user.followed` | User follows another |
| `user.unfollowed` | User unfollows |
| `user.verified` | Account verified by admin |

## Axon Commands/Events

| Command | Description |
|---------|-------------|
| `UpdateUserPreferencesCommand` | Broadcast preference changes to all services |
| `UserPreferencesUpdatedEvent` | Consumed by notification-service, post-service |

## Database Schema

Flyway migration: `V1__init_users.sql`, `V2__add_followers.sql`, `V3__add_verifications.sql`

Tables: `users`, `followers`, `account_history`, `verifications`, `follow_requests`

## API Surface

```
GET  /api/v1/users/{username}           Public profile
GET  /api/v1/users/me                   Own profile
PUT  /api/v1/users/me                   Update profile
PUT  /api/v1/users/me/avatar            Update avatar
PUT  /api/v1/users/me/background        Update background
PUT  /api/v1/users/me/settings          Update settings
GET  /api/v1/users/me/history           Account history
POST /api/v1/users/me/verify            Submit verification
GET  /api/v1/users/{userId}/followers   Paginated followers
GET  /api/v1/users/{userId}/following   Paginated following
POST /api/v1/users/{userId}/follow      Follow
DELETE /api/v1/users/{userId}/follow    Unfollow
```

## Cache Keys

| Key | TTL |
|-----|-----|
| `user:profile:{userId}` | 5 min |
| `user:profile:username:{username}` | 5 min |
| `user:followers:count:{userId}` | 1 min |
| `user:following:count:{userId}` | 1 min |

## Tests

- **Unit:** UserServiceImpl, FollowService, KeycloakSyncService (all external mocked)
- **Integration:** Testcontainers (postgres, redis, kafka), full repository tests
- **Automation:** WebTestClient E2E for all endpoints

---

# media-service

**Tech:** Spring Boot 4.0.2 · PostgreSQL (R2DBC) · Redis · Kafka · Cloudinary · FFmpeg · libvips  
**Port:** 8082

## Responsibilities

Central media management service. Handles file upload validation, compression, transcoding, CDN upload via Cloudinary, and content safety delegation to `media-guard-service`.

## Async Upload Flow

1. Receive multipart → validate magic bytes, size limits.
2. Save metadata as `PROCESSING`.
3. Compress/transcode on `Schedulers.boundedElastic()`.
4. Call `media-guard-service` for NSFW/malware check.
5. Upload to Cloudinary.
6. Update to `READY` or `REJECTED`.
7. Publish `media.upload.completed` or `media.upload.failed`.

## File Limits

| Type | Max size | Output |
|------|---------|--------|
| Avatar | 2 MB | WebP 400x400 max |
| Post image | 8 MB | WebP max 2048px |
| Video | 512 MB | H.264/AAC 1080p max |
| Document | 50 MB | PDF pass-through |

## Kafka Events

`media.upload.completed`, `media.upload.failed`

## API Surface

```
POST   /api/v1/media/upload            Multipart upload (returns mediaId)
GET    /api/v1/media/{mediaId}         Metadata + CDN URL
GET    /api/v1/media/{mediaId}/status  Polling status
DELETE /api/v1/media/{mediaId}         Soft delete
```

---

# post-service

**Tech:** Spring Boot 4.0.2 · PostgreSQL (R2DBC) · Redis · Kafka · WebClient  
**Port:** 8083

## Responsibilities

Core social posting engine. Handles post creation (with moderation gate), interactions (like, repost, bookmark), feeds, and system auto-posts.

## Post Creation Flow

1. Receive `CreatePostRequest`.
2. If media IDs present → await `media.upload.completed` Kafka event (max 5 min timeout).
3. Call `post-guard-service` → if `REJECTED` return 422; if `FLAGGED` store with `PENDING_REVIEW`.
4. Save post as `PUBLISHED` or `PENDING_REVIEW`.
5. Publish `post.created`.
6. Invalidate feed cache.

## Auto-Post System

Consumes `user.avatar.changed`, `user.background.changed` → creates `POST_TYPE=AUTO` post if user setting `post_on_avatar_change=true`.

## Interaction Counters

Redis atomic increment on like/repost/view → async batch-write to PostgreSQL every 30s (counter flush job). Prevents DB hotspot on viral posts.

## Feed Algorithm

Home feed: Union of followed users' posts (ordered by createdAt DESC).  
Explore feed: Trending posts scored by `(likeCount * 2 + repostCount * 3 + replyCount) / age_decay`.  
Feed cached per user in Redis sorted set with 30s TTL.

## API Surface

```
POST   /api/v1/posts                    Create post
GET    /api/v1/posts/{postId}           Get post
PUT    /api/v1/posts/{postId}           Edit post
DELETE /api/v1/posts/{postId}           Soft delete
GET    /api/v1/posts/{postId}/thread    Full thread
POST   /api/v1/posts/{postId}/like      Like / unlike
POST   /api/v1/posts/{postId}/repost    Repost
POST   /api/v1/posts/{postId}/bookmark  Bookmark
POST   /api/v1/posts/{postId}/report    Report
GET    /api/v1/posts/feed/home          Home feed
GET    /api/v1/posts/feed/explore       Explore feed
GET    /api/v1/users/{userId}/posts     User timeline
```

---

# comment-service

**Tech:** Spring Boot 4.0.2 · **Cassandra** · Redis · Kafka  
**Port:** 8084

## Why Cassandra

Comments are append-heavy (millions/day), need fast cursor-based pagination (TIMEUUID clustering key), and have simple query patterns (by postId, by parentCommentId). Cassandra excels here.

## Consistency Model

- Write consistency: `LOCAL_QUORUM`
- Read consistency: `LOCAL_ONE` (eventual OK for non-critical reads)
- Counter tables use `COUNTER` type (eventually consistent, no distributed lock needed)

## Nesting Strategy

Two tables: `comments_by_post` (top-level) + `comments_by_parent` (replies). Max nesting depth: 3 levels (enforced in service layer).

## Interaction Counters

`like_count`, `reply_count` use Cassandra `COUNTER` columns — atomic without locks.

## API Surface

```
POST   /api/v1/posts/{postId}/comments           Create comment
GET    /api/v1/posts/{postId}/comments           Top-level comments (cursor)
GET    /api/v1/comments/{commentId}/replies      Nested replies
POST   /api/v1/comments/{commentId}/like         Like/unlike
POST   /api/v1/comments/{commentId}/report       Report
DELETE /api/v1/comments/{commentId}              Soft delete
```

---

# notification-service

**Tech:** Spring Boot 4.0.2 · **Cassandra** · Redis · Kafka · Reactive WebSocket  
**Port:** 8085

## Responsibilities

Fan-out notification delivery from all domain events. Real-time push via WebSocket. Multi-device read-state synchronization.

## Kafka Topics Consumed

`post.created`, `post.liked`, `post.reposted`, `comment.created`, `comment.liked`, `user.followed`, `user.verified`, `media.upload.completed`, `notification.read`

## Axon Events Consumed

`UserPreferencesUpdatedEvent` → dynamically update which notification types are enabled per user.

## Multi-Device Sync

Read event on device A → publish `notification.read` to Kafka → all WebSocket sessions for that user receive `READ_STATE_UPDATE` push.

## WebSocket Protocol

```
Connect:  WS /ws/notifications?token=<gateway-validated>
Messages:
  → PING (client heartbeat)
  ← PONG
  ← NOTIFICATION {id, type, message, deepLink, timestamp}
  ← READ_STATE_UPDATE {notificationIds: [...]}
  ← UNREAD_COUNT_UPDATE {count: 5}
```

## API Surface

```
GET    /api/v1/notifications              Paginated (cursor)
POST   /api/v1/notifications/read-all     Mark all read
PUT    /api/v1/notifications/{id}/read    Mark one read
DELETE /api/v1/notifications/{id}         Delete (soft)
WS     /ws/notifications                  Real-time push
```

---

# search-service

**Tech:** Spring Boot 4.0.2 · **Elasticsearch 8** (reactive) · Redis · Kafka  
**Port:** 8086

## Index Strategy

| Index | Refresh Rate | Primary use |
|-------|-------------|-------------|
| `users_v1` | 1s | Username autocomplete, profile search |
| `posts_v1` | 1s | Full-text content search |
| `hashtags_v1` | 10s | Trending, autocomplete |

## Kafka Consumer Sync

Consumes domain events → reactive bulk upsert to Elasticsearch.  
On service startup: reindex check (compare ES doc count vs DB count, trigger full reindex if delta > 5%).

## Search Features

- Full-text post search with highlight snippets.
- User prefix search (autocomplete on username/displayName).
- Hashtag trending score (time-decayed weighted sum of post interactions).
- Faceted search: filter by date range, verified users, media type.

## API Surface

```
GET /api/v1/search?q=&type=ALL|USERS|POSTS|HASHTAGS
GET /api/v1/search/trending/hashtags
GET /api/v1/search/suggestions?q=
GET /api/v1/search/users?q=
GET /api/v1/search/posts?q=&from=&to=
```

---

# post-guard-service (FastAPI)

**Tech:** FastAPI 0.133 · Python 3.12 · BERT classifier · ChromaDB (RAG) · PostgreSQL · Kafka  
**Port:** 8090

## Model Architecture

- Base: `bert-base-multilingual-cased` fine-tuned on platform violation data.
- Categories: SPAM, PHISHING, HATE_SPEECH, HARASSMENT, MISINFORMATION, ADULT_CONTENT, TOS_VIOLATION.
- RAG: ChromaDB stores embeddings of known violations; new content compared via cosine similarity.

## Decision Logic

```
1. Embed incoming content (sentence-transformers)
2. RAG lookup → retrieve top-5 similar known violations
3. BERT inference with RAG context
4. Confidence threshold:
   > 0.9  → APPROVED
   0.7-0.9 → FLAGGED (human review queue)
   < 0.7  → REJECTED
```

## Model Refresh

APScheduler runs daily at 02:00 UTC:
1. Query `moderation_labels` table for new human-reviewed items.
2. Fine-tune BERT (1-3 epochs on new data).
3. Update ChromaDB index.
4. Version model: `bert_guard_v{YYYYMMDD}`.
5. Publish `ai.model.updated` to Kafka.

## API Surface

```
POST /api/v1/guard/post            Content check
POST /api/v1/guard/batch           Batch content check
GET  /api/v1/guard/model/status    Model info
POST /api/v1/guard/model/refresh   Manual retrain trigger
```

---

# media-guard-service (FastAPI)

**Tech:** FastAPI 0.133 · Python 3.12 · CLIP · custom NSFW classifier  
**Port:** 8091

## Detection Capabilities

| Check | Method |
|-------|--------|
| NSFW image | CLIP + fine-tuned classifier |
| Deepfake detection | Face detection + frequency analysis |
| Malware in image | Magic byte + entropy analysis |
| Hidden executable | Polyglot file detection |

## API Surface

```
POST /api/v1/guard/media          URL-based safety check
POST /api/v1/guard/file-check     Raw bytes check (streaming)
GET  /api/v1/guard/model/status   Model versions
```

---

# user-analysis-service (FastAPI)

**Tech:** FastAPI 0.133 · Python 3.12 · Kafka (aiokafka) · PostgreSQL · Redis · scikit-learn · PyTorch · Qdrant  
**Port:** 8092

## Behavior Events Tracked

Post views (dwell time), scroll depth, click-through rate, search patterns, follow/unfollow velocity, session duration, device switching patterns, like/unlike patterns.

## Models

| Model | Purpose | Algorithm |
|-------|---------|-----------|
| Anomaly detector | Detect abnormal activity spikes | Isolation Forest |
| Bot classifier | Detect automated accounts | Gradient Boosting + velocity features |
| Session analyzer | Detect suspicious session patterns | LSTM autoencoder |
| Recommendation embeddings | User-item collaborative filtering | Matrix factorization (ALS) |

## RAG Integration

User activity history chunked into time windows → embedded → stored in Qdrant. On analysis request, retrieve relevant behavioral context for LLM-assisted anomaly explanation.

## API Surface

```
POST /api/v1/analysis/user/{userId}           On-demand analysis
GET  /api/v1/analysis/user/{userId}           Latest report
GET  /api/v1/analysis/user/{userId}/timeline  Behavior timeline
POST /api/v1/analysis/model/refresh           Retrain all models
GET  /api/v1/analysis/recommendations/{userId} Feed recommendation signals
```

---

# ai-dashboard-service (FastAPI)

**Tech:** FastAPI 0.133 · Python 3.12 · WebSocket · PostgreSQL · Kafka consumer  
**Port:** 8093

## Responsibilities

Single pane of glass for all AI/moderation data. Admin-only service (role: ADMIN, MODERATOR).

## Data Aggregation

- Reads from `post-guard-service` DB (moderation queue, stats).
- Reads from `user-analysis-service` DB (violation suspects, model performance).
- Consumes `ai.user.insights`, `ai.model.updated` from Kafka.
- Real-time metric streaming via WebSocket.

## Moderation Queue

Human reviewers can: approve, reject, escalate, or ban from the moderation queue. All decisions published back to respective services via Kafka.

## API Surface

```
GET  /api/v1/dashboard/overview                 Platform-wide AI metrics
GET  /api/v1/dashboard/moderation/stats         Moderation statistics
GET  /api/v1/dashboard/moderation/queue         Flagged content queue
PUT  /api/v1/dashboard/moderation/{id}/review   Review decision
GET  /api/v1/dashboard/users/violations         TOS violators list
GET  /api/v1/dashboard/users/bots               Bot detection results
GET  /api/v1/dashboard/models                   All model versions + metrics
GET  /api/v1/dashboard/trends                   Platform behavior trends
WS   /ws/dashboard/live                         Real-time metric stream
```
