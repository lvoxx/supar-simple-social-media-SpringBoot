# comment-service

**Type:** Spring Boot  
**Port:** `8084`  
**Module path:** `spring-services/services/comment-service`  
**Database:** Apache Cassandra (reactive)  
**Cache:** Redis  
**Messaging:** Apache Kafka  

---

## Trách nhiệm

Quản lý toàn bộ comment và reply trên posts. Thiết kế cho throughput cao với Cassandra để xử lý hàng triệu comment mỗi ngày, hỗ trợ nested reply tối đa 3 cấp, bộ đếm tương tác, và báo cáo vi phạm.

---

## Lý do chọn Cassandra

| Tiêu chí | Lý do |
|----------|-------|
| Write throughput | Comment là append-heavy (hàng triệu/ngày), Cassandra tối ưu cho write |
| Pagination | `TIMEUUID` clustering key cho phép cursor-based pagination hiệu quả |
| Query pattern | Đơn giản: by `postId`, by `parentCommentId` — không cần JOIN |
| Counter | Cassandra `COUNTER` type cho `like_count`, `reply_count` — atomic, không cần distributed lock |
| Scalability | Scale horizontal tự nhiên, không single point of failure |

---

## Cấu trúc nested comments

```
Post
└── Comment A (top-level, parentCommentId = NULL)
    ├── Reply B (cấp 1, parentCommentId = A)
    │   └── Reply C (cấp 2, parentCommentId = B)   ← MAX DEPTH
    └── Reply D (cấp 1, parentCommentId = A)
```

Giới hạn độ sâu = **3 cấp** — enforced ở service layer khi tạo reply.

---

## Consistency Model

| Thao tác | Consistency Level | Lý do |
|----------|------------------|-------|
| Write comment | `LOCAL_QUORUM` | Đảm bảo ghi đủ replica |
| Read top-level | `LOCAL_ONE` | Eventual OK cho read không critical |
| Counter update | `LOCAL_QUORUM` | Counter type yêu cầu quorum |
| Delete (soft) | `LOCAL_QUORUM` | Đảm bảo visibility đồng đều |

---

## Starters sử dụng

`starter-cassandra` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events

### Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `comment.created` | Comment/reply được tạo | notification-svc, search-svc, user-analysis-svc |
| `comment.liked` | User like comment | notification-svc |
| `comment.reposted` | User repost comment (quote) | notification-svc |
| `comment.reported` | User report comment | ai-dashboard-svc |
| `comment.deleted` | Comment bị xóa mềm | search-svc |

---

## Cassandra Schema (Keyspace: `x_social`)

```cql
-- Comments by post (top-level pagination)
CREATE TABLE comments_by_post (
  post_id          UUID,
  comment_id       TIMEUUID,
  parent_comment_id UUID,                -- NULL cho top-level
  author_id        UUID,
  content          TEXT,
  like_count       INT    DEFAULT 0,
  reply_count      INT    DEFAULT 0,
  status           TEXT   DEFAULT 'ACTIVE',  -- ACTIVE|HIDDEN|DELETED
  is_deleted       BOOLEAN DEFAULT FALSE,
  deleted_at       TIMESTAMP,
  deleted_by       UUID,
  media_ids        LIST<UUID>,
  depth            INT    DEFAULT 0,
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP,
  PRIMARY KEY (post_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

-- Replies by parent comment
CREATE TABLE comments_by_parent (
  parent_comment_id UUID,
  comment_id        TIMEUUID,
  post_id           UUID,
  author_id         UUID,
  content           TEXT,
  is_deleted        BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMP,
  PRIMARY KEY (parent_comment_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

-- Like tracking (để kiểm tra user đã like chưa)
CREATE TABLE comment_likes (
  comment_id        UUID,
  user_id           UUID,
  post_id           UUID,
  liked_at          TIMESTAMP,
  PRIMARY KEY (comment_id, user_id)
);

-- Counters (separate table — Cassandra counter requirement)
CREATE TABLE comment_counters (
  comment_id        UUID PRIMARY KEY,
  like_count        COUNTER,
  reply_count       COUNTER
);
```

---

## API Endpoints

```
# Comments của post
POST   /api/v1/posts/{postId}/comments
GET    /api/v1/posts/{postId}/comments           # top-level, cursor pagination

# Replies
GET    /api/v1/comments/{commentId}/replies      # nested replies, cursor pagination

# Tương tác
POST   /api/v1/comments/{commentId}/like
DELETE /api/v1/comments/{commentId}/like
POST   /api/v1/comments/{commentId}/report

# Quản lý
DELETE /api/v1/comments/{commentId}              # soft delete (owner + moderator)
PUT    /api/v1/comments/{commentId}              # chỉnh sửa (owner, trong 15 phút)
```

---

## Pagination với Cursor

```
Request:  GET /api/v1/posts/{postId}/comments?limit=20&cursor={lastCommentId}
Response:
{
  "items": [...],
  "nextCursor": "01HXZ...",     ← TIMEUUID của comment cuối trong page này
  "hasMore": true
}

Query Cassandra:
  SELECT * FROM comments_by_post
  WHERE post_id = ? AND comment_id < ?    ← cursor (TIMEUUID = time-sortable)
  LIMIT 20;
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `comment:detail:{commentId}` | 2 phút | Chi tiết comment |
| `comment:liked:{userId}:{commentId}` | 5 phút | Đã like chưa |
| `comment:replies:count:{commentId}` | 1 phút | Số lượng reply |

---

## Cấu trúc source

```
comment-service/src/main/java/com/xsocial/comment/
├── config/CassandraCommentConfig.java
├── domain/
│   ├── entity/Comment.java, CommentCounter.java
│   └── repository/CommentByPostRepository.java, CommentByParentRepository.java
├── application/
│   ├── service/CommentService.java, CommentLikeService.java
│   └── dto/CommentResponse.java, CreateCommentRequest.java
├── infrastructure/
│   └── kafka/CommentEventPublisher.java
└── web/
    ├── router/CommentRouter.java
    └── handler/CommentHandler.java
```

---

## Tests

### Unit Tests
- `CommentServiceTest` — mock repositories, depth limit enforcement
- `CommentLikeServiceTest` — mock counter repository

### Integration Tests (Testcontainers)
- `CommentRepositoryIT` — Cassandra container (cassandra:4.1)
- `CommentKafkaIT` — Cassandra + Kafka

### Automation Tests
- `CommentApiAutomationTest` — create → reply → nested reply → like → delete
- `PaginationAutomationTest` — cursor pagination across multiple pages
