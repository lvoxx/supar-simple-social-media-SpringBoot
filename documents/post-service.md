# post-service

**Type:** Spring Boot  
**Port:** `8083`  
**Module path:** `spring-services/services/post-service`  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (Redisson)  
**Messaging:** Apache Kafka  

---

## Trách nhiệm

Engine đăng bài xã hội cốt lõi. Quản lý vòng đời post (tạo, sửa, xóa mềm), các tương tác (like, repost, bookmark), feed (home & explore), và system auto-posts. Mọi post trong group đều đi qua service này.

---

## Loại Post

| `post_type` | Mô tả |
|-------------|-------|
| `ORIGINAL` | Post gốc do user tạo |
| `REPLY` | Trả lời một post khác (reply_to_id) |
| `REPOST` | Đăng lại không kèm bình luận |
| `QUOTE` | Đăng lại kèm bình luận của user |
| `AUTO` | System post (đổi avatar, achievement) |

---

## Luồng tạo post

```
1. User gửi CreatePostRequest
2. Validate nội dung (độ dài, format)
3. Nếu có media IDs:
   a. Await Kafka event: media.upload.completed (timeout 5 phút)
   b. Nếu timeout → status = PENDING_MEDIA, thông báo user
4. Gọi post-guard-service (FastAPI):
   a. APPROVED  → lưu status = PUBLISHED
   b. FLAGGED   → lưu status = PENDING_REVIEW
   c. REJECTED  → trả lỗi 422 cho user
5. Nếu post trong group → gọi group-service để đăng ký association
6. Publish Kafka: post.created
7. Invalidate feed cache của tất cả follower
8. Trả kết quả về user
```

---

## Bộ đếm tương tác (Counter Strategy)

Tránh DB hotspot trên post viral:

```
Tương tác xảy ra:
  → Redis INCR/DECR counter: post:like-count:{postId}
  → Batch write job (mỗi 30 giây): đẩy delta từ Redis xuống PostgreSQL
  → Cache TTL: 5 phút sau lần INCR cuối cùng

Ưu điểm: DB không bị spam update; Redis hấp thụ toàn bộ burst traffic
```

---

## Feed Algorithm

### Home Feed
```
SELECT posts WHERE author_id IN (following list)
ORDER BY created_at DESC
LIMIT 20 (cursor-based pagination)
```
Cache per user trong Redis Sorted Set: `feed:home:{userId}` TTL 30 giây.

### Explore Feed
```
score = (like_count * 2 + repost_count * 3 + reply_count) / age_decay_factor
age_decay_factor = EXTRACT(EPOCH FROM (NOW() - created_at)) / 3600 + 2
ORDER BY score DESC
```
Cache: `feed:explore:global` TTL 1 phút (shared, không per-user).

---

## Auto-Post (System Posts)

Consume Kafka events và tạo `POST_TYPE = AUTO`:

| Kafka Event | Nội dung Auto Post |
|-------------|-------------------|
| `user.avatar.changed` | "📸 [username] đã cập nhật ảnh đại diện" (nếu `post_on_avatar_change=true`) |
| `user.background.changed` | "🖼️ [username] đã cập nhật ảnh nền" |

---

## Starters sử dụng

`starter-postgres` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events

### Published

| Topic | Trigger |
|-------|---------|
| `post.created` | Post được published |
| `post.updated` | Nội dung post được chỉnh sửa |
| `post.deleted` | Post bị xóa mềm |
| `post.liked` | User like post |
| `post.reposted` | User repost |
| `post.bookmarked` | User bookmark |
| `post.reported` | User report vi phạm |

### Consumed

| Topic | Hành động |
|-------|-----------|
| `media.upload.completed` | Cập nhật post PENDING_MEDIA → PUBLISHED |
| `media.upload.failed` | Cập nhật post → MEDIA_FAILED, thông báo user |
| `user.avatar.changed` | Tạo auto-post nếu user setting cho phép |
| `user.background.changed` | Tạo auto-post nếu user setting cho phép |

---

## Database Schema

```sql
-- posts
id                UUID        PRIMARY KEY DEFAULT gen_random_uuid()
author_id         UUID        NOT NULL
group_id          UUID                                -- NULL nếu không trong group
content           TEXT                               -- max 280 ký tự
reply_to_id       UUID        REFERENCES posts(id)   -- cho reply / thread
repost_of_id      UUID        REFERENCES posts(id)   -- cho repost / quote
post_type         VARCHAR(20) NOT NULL               -- ORIGINAL|REPLY|REPOST|QUOTE|AUTO
status            VARCHAR(20) DEFAULT 'PUBLISHED'    -- DRAFT|PENDING_MEDIA|PUBLISHED|PENDING_REVIEW|HIDDEN|DELETED
visibility        VARCHAR(20) DEFAULT 'PUBLIC'       -- PUBLIC|FOLLOWERS_ONLY|MENTIONED_ONLY
like_count        INT         DEFAULT 0
repost_count      INT         DEFAULT 0
reply_count       INT         DEFAULT 0
bookmark_count    INT         DEFAULT 0
view_count        BIGINT      DEFAULT 0
is_edited         BOOLEAN     DEFAULT FALSE
edited_at         TIMESTAMPTZ
is_pinned         BOOLEAN     DEFAULT FALSE           -- pinned trên profile
is_deleted        BOOLEAN     DEFAULT FALSE
deleted_at        TIMESTAMPTZ
deleted_by        UUID
created_at        TIMESTAMPTZ DEFAULT NOW()
updated_at        TIMESTAMPTZ

-- post_media
post_id           UUID REFERENCES posts(id)
media_id          UUID
position          INT DEFAULT 0
PRIMARY KEY (post_id, media_id)

-- post_likes
post_id           UUID
user_id           UUID
created_at        TIMESTAMPTZ DEFAULT NOW()
PRIMARY KEY (post_id, user_id)

-- post_bookmarks
post_id           UUID
user_id           UUID
created_at        TIMESTAMPTZ DEFAULT NOW()
PRIMARY KEY (post_id, user_id)

-- post_edits  (audit trail)
id                UUID PRIMARY KEY
post_id           UUID REFERENCES posts(id)
previous_content  TEXT
edited_at         TIMESTAMPTZ
edited_by         UUID

-- post_reports
id                UUID PRIMARY KEY
post_id           UUID REFERENCES posts(id)
reporter_id       UUID
reason            VARCHAR(50)
detail            TEXT
status            VARCHAR(20) DEFAULT 'PENDING'
created_at        TIMESTAMPTZ

-- post_hashtags
post_id           UUID
hashtag           VARCHAR(100)
PRIMARY KEY (post_id, hashtag)

-- post_mentions
post_id           UUID
mentioned_user_id UUID
PRIMARY KEY (post_id, mentioned_user_id)
```

---

## API Endpoints

```
# Post CRUD
POST   /api/v1/posts
GET    /api/v1/posts/{postId}
PUT    /api/v1/posts/{postId}
DELETE /api/v1/posts/{postId}
GET    /api/v1/posts/{postId}/thread          # Full thread chain

# Tương tác
POST   /api/v1/posts/{postId}/like
DELETE /api/v1/posts/{postId}/like
POST   /api/v1/posts/{postId}/repost          # Body: { quoteContent? }
DELETE /api/v1/posts/{postId}/repost
POST   /api/v1/posts/{postId}/bookmark
DELETE /api/v1/posts/{postId}/bookmark
POST   /api/v1/posts/{postId}/report

# Feeds
GET    /api/v1/posts/feed/home                # Home feed (following)
GET    /api/v1/posts/feed/explore             # Trending explore feed

# Timeline người dùng
GET    /api/v1/users/{userId}/posts
GET    /api/v1/users/{userId}/likes
GET    /api/v1/users/{userId}/bookmarks
GET    /api/v1/users/{userId}/reposts

# View tracking (fire-and-forget)
POST   /api/v1/posts/{postId}/view
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `post:detail:{postId}` | 5 phút | Chi tiết post |
| `post:like-count:{postId}` | Vô hạn (flush mỗi 30s) | Counter Redis |
| `post:repost-count:{postId}` | Vô hạn | Counter Redis |
| `feed:home:{userId}` | 30 giây | Home feed page 1 |
| `feed:explore:global` | 1 phút | Explore feed |
| `post:liked-by:{userId}:{postId}` | 5 phút | Kiểm tra đã like chưa |
| `post:bookmarked-by:{userId}:{postId}` | 5 phút | Kiểm tra đã bookmark chưa |

---

## Rate Limits

| Endpoint | Giới hạn |
|----------|---------|
| `POST /posts` | 30 req/giờ per userId |
| `POST /{postId}/like` | 200 req/phút per userId |
| `POST /{postId}/report` | 10 req/ngày per userId |

---

## Tests

### Unit Tests
- `PostServiceImplTest` — mock guard client, media client, repository
- `FeedServiceTest` — mock Redis sorted set operations
- `CounterFlushJobTest` — kiểm tra batch write logic

### Integration Tests (Testcontainers)
- `PostRepositoryIT` — PostgreSQL container
- `PostCreationFlowIT` — PostgreSQL + Kafka + WireMock post-guard-service
- `FeedCacheIT` — PostgreSQL + Redis

### Automation Tests
- `PostApiAutomationTest` — tạo post → like → repost → bookmark → delete
- `FeedApiAutomationTest` — kiểm tra home feed pagination
