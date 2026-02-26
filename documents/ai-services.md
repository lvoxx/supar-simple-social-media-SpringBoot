# media-guard-service

**Type:** FastAPI (Python)  
**Port:** `8091`  
**Module path:** `python-services/media-guard-service`  
**AI:** CLIP + NSFW classifier + entropy analysis  

---

## Trách nhiệm

Phát hiện nội dung độc hại trong media: ảnh NSFW, deepfake, malware ẩn trong file hình ảnh, file polyglot. Được gọi bởi `media-service` sau bước compression, trước khi upload lên Cloudinary.

---

## Các kiểm tra

| Kiểm tra | Phương pháp | Kết quả |
|----------|-------------|---------|
| NSFW classification | CLIP embedding + fine-tuned binary classifier | score 0.0–1.0 |
| Deepfake detection | FaceForensics++ trained model, frequency domain artifacts | `REAL` / `SUSPECTED_FAKE` |
| Magic byte validation | File header inspection + libmagic | `VALID` / `MISMATCH` |
| Entropy analysis | Shannon entropy per file block | `NORMAL` / `SUSPICIOUS` (polyglot, steganography) |
| Malware heuristic | Known signature patterns + entropy spike | `CLEAN` / `SUSPICIOUS` |

---

## API Endpoints

```
# URL-based check (sau khi upload tạm lên storage)
POST /api/v1/guard/media
  Body: { mediaId, tempUrl, contentType, fileSizeBytes }
  Response: {
    safe: true|false,
    categories: ["NSFW", "DEEPFAKE", "MALWARE"],
    confidence: 0.97,
    details: { nsfwScore: 0.02, deepfakeScore: 0.01, ... }
  }

# Stream raw bytes check
POST /api/v1/guard/file-check
  Content-Type: application/octet-stream
  Response: { safe: bool, magicByteValid: bool, entropyNormal: bool }

# Model status
GET  /api/v1/guard/model/status
GET  /api/v1/health
GET  /api/v1/metrics
```

---

## Cấu trúc source

```
media-guard-service/app/
├── main.py
├── config.py
├── api/v1/routes/guard.py
├── services/
│   ├── media_guard_service.py
│   └── file_analyzer.py
├── infrastructure/ml/
│   ├── nsfw_classifier.py      # CLIP + classifier
│   ├── deepfake_detector.py
│   └── file_inspector.py       # magic byte + entropy
└── core/exceptions.py
```

---

## Tests

### Unit Tests
- `test_nsfw_classifier.py` — mock CLIP, test threshold
- `test_file_inspector.py` — test magic byte patterns, entropy

### Integration Tests
- `test_media_guard_api.py` — httpx TestClient với sample images

---

## Metrics

```
media_guard_requests_total{safe="true|false"}
media_guard_nsfw_score (histogram)
media_guard_latency_seconds
```

---
---

# user-analysis-service

**Type:** FastAPI (Python)  
**Port:** `8092`  
**Module path:** `python-services/user-analysis-service`  
**Database:** PostgreSQL (asyncpg) + Qdrant (vector)  
**Messaging:** Kafka (consumer + producer)  
**AI:** Isolation Forest · LSTM Autoencoder · Gradient Boosting · ALS  

---

## Trách nhiệm

Phân tích hành vi người dùng từ Kafka events. Phát hiện bot, hành vi bất thường, vi phạm TOS tiềm ẩn. Tạo recommendation signals cho post-service. Sử dụng RAG để lấy lịch sử hành vi liên quan khi phân tích.

---

## Behavior Events Tracked

| Event | Dữ liệu thu thập |
|-------|-----------------|
| Post view | postId, dwellTimeMs, scrollDepth |
| Post like/unlike | postId, timeSinceView |
| Follow/unfollow | targetId, followDuration |
| Search | query, clickedResults |
| Session | duration, deviceType, ipHash |
| Message sent | convType, messageLength (không content) |

---

## Models

| Model | Mục đích | Algorithm |
|-------|---------|-----------|
| Anomaly detector | Activity velocity spikes | Isolation Forest |
| Bot classifier | Automated account detection | Gradient Boosting + velocity features |
| Session analyzer | Suspicious session patterns | LSTM Autoencoder |
| Recommendation | User-item collaborative filtering | ALS (Matrix factorization) |

---

## RAG Pipeline

```
User activity logs → chunked theo time window (1 giờ)
    → sentence-transformers embedding
    → store trong Qdrant

Khi phân tích user:
    → embed query ("user {userId} behavior analysis")
    → similarity search Qdrant → top-K context chunks
    → LLM-assisted explanation với context
```

---

## Kafka Events Consumed

Tất cả `user.behavior.*` events từ các service.

## Kafka Events Published

| Topic | Trigger |
|-------|---------|
| `ai.user.insights` | Sau mỗi lần phân tích hoàn tất |
| `ai.user.violation.suspected` | Bot / TOS violation detected |
| `ai.recommendation.ready` | Recommendation signals cập nhật |

---

## API Endpoints

```
POST /api/v1/analysis/user/{userId}           # Trigger on-demand analysis
GET  /api/v1/analysis/user/{userId}           # Kết quả phân tích mới nhất
GET  /api/v1/analysis/user/{userId}/timeline  # Behavior timeline (30 ngày)
GET  /api/v1/analysis/recommendations/{userId} # Feed recommendation signals
POST /api/v1/analysis/model/refresh           # Retrain all models (ADMIN)
GET  /api/v1/analysis/model/status            # Model versions + performance
GET  /api/v1/health
GET  /api/v1/metrics
```

---

## PostgreSQL Schema

```sql
-- behavior_events (time-series, partitioned by day)
id              UUID PRIMARY KEY
user_id         UUID NOT NULL
event_type      VARCHAR(50)
payload         JSONB
session_id      UUID
ip_hash         TEXT
created_at      TIMESTAMPTZ

-- user_analysis_reports
id              UUID PRIMARY KEY
user_id         UUID UNIQUE
bot_score       FLOAT         -- 0.0-1.0
anomaly_score   FLOAT
violation_suspected BOOLEAN
violation_categories TEXT[]
recommendation_signals JSONB
last_analyzed_at TIMESTAMPTZ
model_version   VARCHAR(50)
created_at      TIMESTAMPTZ
updated_at      TIMESTAMPTZ
```

---

## Cấu trúc source

```
user-analysis-service/app/
├── main.py
├── config.py
├── api/v1/routes/analysis.py
├── services/
│   ├── analysis_service.py
│   ├── recommendation_service.py
│   └── rag_service.py
├── infrastructure/
│   ├── database.py
│   ├── kafka/consumer.py, producer.py
│   ├── qdrant.py
│   └── ml/
│       ├── anomaly_detector.py
│       ├── bot_classifier.py
│       ├── session_analyzer.py
│       └── recommendation_engine.py
└── scheduler/retrain_job.py
```

---

## Tests

### Unit Tests
- `test_bot_classifier.py` — mock feature extraction
- `test_anomaly_detector.py` — synthetic velocity data

### Integration Tests
- `test_analysis_api.py` — PostgreSQL + Kafka containers
- `test_kafka_consumer.py` — behavior event processing

---
---

# ai-dashboard-service

**Type:** FastAPI (Python)  
**Port:** `8093`  
**Module path:** `python-services/ai-dashboard-service`  
**Database:** PostgreSQL (asyncpg — reads from all AI service DBs)  
**Messaging:** Kafka (consumer)  
**Real-time:** WebSocket  

---

## Trách nhiệm

Single pane of glass cho tất cả dữ liệu AI/moderation. Backend cho admin dashboard: thống kê kiểm duyệt, hàng đợi review, danh sách vi phạm nghi vấn, hiệu suất models, và xu hướng platform.

---

## Nguồn dữ liệu

| Nguồn | Dữ liệu |
|-------|---------|
| `post-guard-service` DB | Moderation queue, decision stats |
| `media-guard-service` DB | Media safety stats |
| `user-analysis-service` DB | Violation suspects, bot scores, model performance |
| Kafka consumers | Real-time events để cập nhật dashboard |

---

## Kafka Events Consumed

| Topic | Hành động |
|-------|-----------|
| `ai.user.insights` | Cập nhật user analysis panel |
| `ai.user.violation.suspected` | Thêm vào violation queue |
| `ai.model.updated` | Cập nhật model registry |
| `message.notification.failed` | Theo dõi push failure rate |

---

## Moderation Queue Workflow

```
post-guard-service → FLAGGED → ai-dashboard-service (hiển thị cho reviewer)
    │
    ├── Reviewer click APPROVE → publish Kafka → post-service: unhide
    ├── Reviewer click REJECT  → publish Kafka → post-service: delete
    └── Reviewer click ESCALATE → escalate level, notify senior moderator
    
Audit trail lưu mọi review decision với reviewer ID + timestamp
```

---

## API Endpoints

```
# Overview
GET  /api/v1/dashboard/overview
  Response: {
    totalUsers, activeToday, postsToday,
    flaggedQueue: 15, botsSuspected: 3,
    modelAccuracies: {...}
  }

# Moderation
GET  /api/v1/dashboard/moderation/stats
  Params: ?period=1d|7d|30d
GET  /api/v1/dashboard/moderation/queue
  Params: ?status=PENDING&type=POST|MEDIA&page=&size=
PUT  /api/v1/dashboard/moderation/{id}/review
  Body: { action: APPROVE|REJECT|ESCALATE, reason? }

# Users
GET  /api/v1/dashboard/users/violations
  Params: ?minScore=0.7&page=&size=
GET  /api/v1/dashboard/users/bots
  Params: ?minBotScore=0.8&page=&size=

# Models
GET  /api/v1/dashboard/models
  Response: [{ name, version, accuracy, f1Score, lastTrainedAt, isActive }]

# Trends
GET  /api/v1/dashboard/trends
  Params: ?period=24h|7d
  Response: { topHashtags, contentCategories, violationTrend, userGrowth }

# Real-time WebSocket
WS   /ws/dashboard/live
  Server pushes: QUEUE_UPDATE, METRIC_UPDATE, VIOLATION_ALERT
```

---

## WebSocket Protocol

```
Connect:  WS /ws/dashboard/live
  Requires: role ADMIN or MODERATOR

Server → Client (real-time):
  { "type": "QUEUE_UPDATE",    "queueSize": 23 }
  { "type": "VIOLATION_ALERT", "userId": "...", "botScore": 0.95 }
  { "type": "METRIC_UPDATE",   "metric": "flaggedToday", "value": 15 }
  { "type": "MODEL_UPDATED",   "modelName": "bert_guard", "version": "v20260101" }
```

---

## Cấu trúc source

```
ai-dashboard-service/app/
├── main.py
├── config.py
├── api/v1/routes/
│   ├── overview.py
│   ├── moderation.py
│   ├── users.py
│   ├── models.py
│   └── trends.py
├── services/
│   ├── dashboard_service.py
│   ├── moderation_service.py
│   └── trend_service.py
├── infrastructure/
│   ├── database.py               # Read từ nhiều DB schemas
│   ├── kafka/consumer.py
│   └── websocket_manager.py     # WebSocket connection pool
└── core/
    ├── exceptions.py
    └── auth.py                   # Admin/Moderator role check
```

---

## Tests

### Unit Tests
- `test_dashboard_service.py` — mock DB queries, test aggregations
- `test_moderation_service.py` — mock review actions

### Integration Tests
- `test_dashboard_api.py` — PostgreSQL container
- `test_kafka_consumer.py` — Kafka container

### Automation Tests
- `test_moderation_workflow.py` — full review cycle (flag → human review → approve/reject)

---

## Metrics

```
dashboard_moderation_queue_size (gauge)
dashboard_review_actions_total{action="APPROVE|REJECT|ESCALATE"}
dashboard_model_accuracy{model="bert_guard|nsfw_classifier"}
dashboard_ws_connections (gauge)
```
