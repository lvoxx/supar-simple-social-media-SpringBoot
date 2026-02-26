# search-service

**Type:** Spring Boot  
**Port:** `8086`  
**Module path:** `spring-services/services/search-service`  
**Database:** Elasticsearch 8 (reactive)  
**Cache:** Redis  
**Messaging:** Kafka (consumer — CDC sync)  

---

## Trách nhiệm

Cung cấp full-text search cho posts, users, hashtags, và groups. Đồng bộ dữ liệu realtime từ PostgreSQL/Cassandra sang Elasticsearch qua Kafka events. Hỗ trợ autocomplete, trending hashtags, và faceted search.

---

## Elasticsearch Indices

| Index | Refresh Rate | Replicas | Mô tả |
|-------|-------------|---------|-------|
| `users_v1` | 1s | 1 | Username, displayName, bio, verified status |
| `posts_v1` | 1s | 1 | Content, hashtags, mentions, engagement scores |
| `hashtags_v1` | 10s | 1 | Tag name, post count, trending score |
| `groups_v1` | 5s | 1 | Group name, description, tags, category |

---

## Index Mappings

```json
// posts_v1
{
  "mappings": {
    "properties": {
      "content":        { "type": "text", "analyzer": "standard" },
      "authorId":       { "type": "keyword" },
      "authorUsername": { "type": "keyword" },
      "createdAt":      { "type": "date" },
      "likeCount":      { "type": "integer" },
      "repostCount":    { "type": "integer" },
      "replyCount":     { "type": "integer" },
      "tags":           { "type": "keyword" },
      "mentionedUserIds": { "type": "keyword" },
      "groupId":        { "type": "keyword" },
      "visibility":     { "type": "keyword" },
      "isDeleted":      { "type": "boolean" }
    }
  }
}

// users_v1
{
  "mappings": {
    "properties": {
      "username":      { "type": "keyword" },
      "displayName":   { "type": "search_as_you_type" },
      "bio":           { "type": "text" },
      "isVerified":    { "type": "boolean" },
      "followerCount": { "type": "integer" },
      "avatarUrl":     { "type": "keyword", "index": false },
      "isDeleted":     { "type": "boolean" }
    }
  }
}
```

---

## CDC Sync (Kafka → Elasticsearch)

Không dùng Debezium — các service tự publish domain events:

```
Kafka Event               →  Elasticsearch action
─────────────────────────────────────────────────────
post.created              →  index post document
post.updated              →  update post document
post.deleted              →  update isDeleted = true
post.liked                →  update likeCount (script update)
post.reposted             →  update repostCount

user.profile.updated      →  update user document
user.verified             →  update isVerified = true
user.avatar.changed       →  update avatarUrl

comment.created           →  (nếu cần full-text comment search)

group.created             →  index group document
group.updated             →  update group document
group.deleted             →  update isDeleted = true
group.post.created        →  update post with groupId
```

### Bulk Indexing Pipeline

```java
// Batch update mỗi 5 giây để tránh individual indexing overhead
Flux.interval(Duration.ofSeconds(5))
    .flatMap(tick -> flushBulkQueue())
    .subscribe();
```

---

## Kafka Events Consumed

| Topic | Hành động |
|-------|-----------|
| `post.created` | Index post mới |
| `post.updated` | Update post document |
| `post.deleted` | Mark isDeleted = true |
| `post.liked` | Script update likeCount |
| `post.reposted` | Script update repostCount |
| `user.profile.updated` | Update user document |
| `user.verified` | Update isVerified flag |
| `user.avatar.changed` | Update avatarUrl |
| `group.created` | Index group mới |
| `group.updated` | Update group document |
| `group.deleted` | Mark isDeleted = true |
| `group.post.created` | Tag post với groupId |

---

## Trending Hashtag Algorithm

```
Mỗi 10 giây (scheduled job):
  trending_score = Σ (post.like_count * 2 + post.repost_count * 3)
                   cho tất cả posts dùng hashtag này
                   trong 24h qua

Áp dụng time decay:
  score = raw_score / (hours_since_first_use / 24 + 1)

Index vào hashtags_v1.trendingScore
Cache kết quả top-50: Redis TTL 1 phút
```

---

## Startup Reindex Check

```
On application start:
  1. Đếm document trong ES index
  2. Đếm record không bị xóa trong PostgreSQL
  3. Nếu delta > 5% → trigger full reindex job
  4. Full reindex chạy async, không block startup
  5. Publish log metric: search.reindex.triggered
```

---

## API Endpoints

```
# Universal search
GET    /api/v1/search
  Params: q, type=ALL|USERS|POSTS|HASHTAGS|GROUPS, page, size

# Trending
GET    /api/v1/search/trending/hashtags
  Params: ?limit=10&period=1h|6h|24h

# Autocomplete
GET    /api/v1/search/suggestions?q=     # prefix match: users + hashtags

# Specific searches
GET    /api/v1/search/users?q=&verified=
GET    /api/v1/search/posts?q=&from=&to=&authorId=&groupId=
GET    /api/v1/search/groups?q=&tags=&category=

# Admin
POST   /api/v1/search/admin/reindex      # trigger manual reindex (ADMIN role)
GET    /api/v1/search/admin/index-stats  # ES index health stats
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `search:trending:hashtags` | 1 phút | Top trending hashtags |
| `search:result:{queryHash}` | 30 giây | Kết quả search (key = MD5 của query params) |
| `search:suggestions:{prefix}` | 1 phút | Autocomplete suggestions |

---

## Tests

### Unit Tests
- `SearchQueryBuilderTest` — Elasticsearch query DSL building
- `TrendingScoreCalculatorTest` — trending algorithm
- `BulkIndexPipelineTest` — mock ES client, batch logic

### Integration Tests (Testcontainers)
- `SearchRepositoryIT` — Elasticsearch container (elasticsearch:8.x)
- `KafkaSyncConsumerIT` — Elasticsearch + Kafka

### Automation Tests
- `SearchApiAutomationTest` — index document → search → verify result → delete → search again
