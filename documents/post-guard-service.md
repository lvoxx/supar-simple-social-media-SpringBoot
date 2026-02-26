# post-guard-service

**Type:** FastAPI (Python)  
**Port:** `8090`  
**Module path:** `python-services/post-guard-service`  
**Database:** PostgreSQL (asyncpg)  
**Vector store:** ChromaDB  
**Messaging:** Kafka (aiokafka — producer)  
**AI:** BERT fine-tuned + RAG pipeline  

---

## Trách nhiệm

Kiểm duyệt nội dung post trước khi publish. Phát hiện spam, phishing, hate speech, nội dung vi phạm TOS. Sử dụng BERT fine-tuned kết hợp RAG (ChromaDB) để tăng độ chính xác bằng cách so sánh với các vi phạm đã biết.

---

## Kiến trúc model

```
Input text
    │
    ▼
[Sentence Transformer: all-MiniLM-L6-v2]
    │
    ▼ embedding vector
    │
    ├──► ChromaDB similarity search
    │      → Top-5 similar known violations (với labels)
    │
    └──► BERT fine-tuned classifier
           + RAG context từ ChromaDB
           │
           ▼
    {decision, reason, confidence, categories}
```

---

## Decision Logic

| Confidence | Quyết định | Hành động |
|-----------|-----------|-----------|
| ≥ 0.90 | `APPROVED` | post-service publish bình thường |
| 0.70 – 0.89 | `FLAGGED` | post-service lưu với `status=PENDING_REVIEW`, không ẩn ngay |
| < 0.70 | `REJECTED` | post-service trả 422 về client |

---

## Violation Categories

| Category | Mô tả |
|----------|-------|
| `SPAM` | Nội dung lặp, quảng cáo không mong muốn |
| `PHISHING` | Liên kết lừa đảo, giả mạo thương hiệu |
| `HATE_SPEECH` | Ngôn từ thù ghét dựa trên đặc điểm nhóm |
| `HARASSMENT` | Tấn công, đe dọa cá nhân |
| `MISINFORMATION` | Thông tin sai lệch có hại |
| `ADULT_CONTENT` | Nội dung người lớn không phù hợp |
| `TOS_VIOLATION` | Các vi phạm Terms of Service khác |
| `CLEAN` | Không vi phạm |

---

## Model Refresh (Daily)

```
Chạy lúc 02:00 UTC (APScheduler):

1. Query PostgreSQL: moderation_labels
   WHERE human_reviewed = true AND created_at > last_train_date

2. Fine-tune BERT:
   a. Load base model
   b. Train 1-3 epochs trên labeled data mới
   c. Evaluate trên validation set
   d. Nếu accuracy tốt hơn model hiện tại → swap

3. Update ChromaDB:
   a. Add embeddings cho data mới
   b. Xóa embeddings cũ > 6 tháng

4. Version model: bert_guard_v{YYYYMMDD}
5. Publish Kafka: ai.model.updated

Nếu training thất bại → giữ model cũ, alert qua log
```

---

## PostgreSQL Schema

```sql
-- moderation_labels (training data + decision audit)
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid()
post_id         UUID        NOT NULL
content         TEXT        NOT NULL
decision        VARCHAR(20)                 -- APPROVED|FLAGGED|REJECTED
categories      TEXT[]
confidence      FLOAT
model_version   VARCHAR(50)
human_reviewed  BOOLEAN     DEFAULT FALSE
human_decision  VARCHAR(20)
human_reviewer  UUID
reviewed_at     TIMESTAMPTZ
created_at      TIMESTAMPTZ DEFAULT NOW()

-- model_versions (tracking)
id              UUID        PRIMARY KEY
version_name    VARCHAR(50) UNIQUE
accuracy        FLOAT
f1_score        FLOAT
trained_at      TIMESTAMPTZ
training_samples INT
is_active       BOOLEAN     DEFAULT FALSE
created_at      TIMESTAMPTZ
```

---

## API Endpoints

```
# Kiểm tra nội dung
POST /api/v1/guard/post
  Body: { postId, content, authorId, groupId? }
  Response: {
    decision: "APPROVED|FLAGGED|REJECTED",
    reason: "...",
    confidence: 0.95,
    categories: ["SPAM"],
    modelVersion: "bert_guard_v20260101"
  }

# Batch kiểm tra (backfill)
POST /api/v1/guard/batch
  Body: { items: [{ postId, content, authorId }] }
  Response: { results: [...] }

# Model info
GET  /api/v1/guard/model/status
  Response: {
    activeVersion: "bert_guard_v20260101",
    accuracy: 0.94,
    lastTrainedAt: "...",
    trainingSamples: 12500
  }

# Manual retrain
POST /api/v1/guard/model/refresh
  Requires: role ADMIN
  Response: { jobId, status: "QUEUED" }

# Health
GET  /api/v1/health
GET  /api/v1/metrics   (Prometheus format)
```

---

## Cấu trúc source

```
post-guard-service/app/
├── main.py
├── config.py                    # Pydantic settings
├── dependencies.py              # DB pool, model singletons
├── api/v1/routes/
│   ├── guard.py                 # /guard/* endpoints
│   └── model.py                 # /model/* endpoints
├── services/
│   ├── guard_service.py         # Decision logic
│   ├── model_service.py         # Model loading / inference
│   └── rag_service.py           # ChromaDB retrieval
├── infrastructure/
│   ├── database.py              # asyncpg connection pool
│   ├── kafka.py                 # aiokafka producer
│   └── ml/
│       ├── bert_classifier.py   # BERT inference
│       ├── embeddings.py        # sentence-transformers
│       └── trainer.py           # Fine-tune pipeline
├── core/
│   ├── exceptions.py
│   └── middleware.py            # Logging, tracing
└── scheduler/
    └── model_refresh_job.py     # APScheduler daily job
```

---

## requirements.txt (chính)

```
fastapi==0.133.0
uvicorn[standard]==0.30.0
pydantic-settings==2.3.0
asyncpg==0.29.0
aioredis==2.0.1
aiokafka==0.11.0
transformers==4.44.0
sentence-transformers==3.1.0
chromadb==0.5.0
torch==2.3.0
scikit-learn==1.5.0
apscheduler==3.10.0
prometheus-fastapi-instrumentator==7.0.0
opentelemetry-sdk==1.25.0
```

---

## Tests

### Unit Tests (pytest)
- `test_guard_service.py` — mock model, test decision paths
- `test_rag_service.py` — mock ChromaDB
- `test_trainer.py` — mock training data fetch

### Integration Tests (pytest-asyncio)
- `test_guard_api_integration.py` — PostgreSQL container (testcontainers-python)
- `test_kafka_publisher.py` — Kafka container

### Automation Tests
- `test_guard_automation.py` — httpx TestClient, test approve/flag/reject scenarios

---

## Metrics (Prometheus)

```
post_guard_requests_total{decision="APPROVED|FLAGGED|REJECTED"}
post_guard_latency_seconds (histogram)
post_guard_model_version_info
post_guard_training_accuracy
```
