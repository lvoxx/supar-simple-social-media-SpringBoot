# ai-dashboard-service

**Type:** FastAPI · Port `8093`  
**Primary DB:** PostgreSQL (asyncpg) — schema `x_social_ai_dashboard`  
**Starters:** *(FastAPI — no Spring starters)*

---

## Responsibilities

Admin-only backend for AI and moderation oversight. Aggregates data from post-guard-service, media-guard-service, and user-analysis-service databases. Provides a moderation queue with human-review workflow. Streams real-time metrics via WebSocket.

---

## DB init

K8S `Job` runs `psql` before Pod starts.  
Scripts: `infrastructure/k8s/db-init/ai-dashboard-service/sql/`

---

## Schema

```sql
-- moderation_queue  (items awaiting human review)
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid()
source_service  VARCHAR(40) NOT NULL    -- post-guard|media-guard|user-analysis
entity_id       UUID        NOT NULL
entity_type     VARCHAR(20) NOT NULL    -- POST|MEDIA|USER
ai_decision     VARCHAR(20)
ai_confidence   FLOAT
categories      TEXT[]
status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
assigned_to     UUID
reviewed_by     UUID
review_action   VARCHAR(20)             -- APPROVE|REJECT|ESCALATE
review_reason   TEXT
reviewed_at     TIMESTAMPTZ
escalated_to    UUID
created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
updated_at      TIMESTAMPTZ

-- review_audit_log  (append-only)
id              UUID        PRIMARY KEY DEFAULT gen_random_uuid()
queue_item_id   UUID        NOT NULL
reviewer_id     UUID        NOT NULL
action          VARCHAR(20) NOT NULL
reason          TEXT
created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

---

## Moderation review workflow

```
AI flags content → post-guard/media-guard/user-analysis
  → publish Kafka: ai.user.violation.suspected / post moderation event
  → ai-dashboard-service inserts into moderation_queue

Moderator reviews via dashboard:
  PUT /api/v1/dashboard/moderation/{id}/review { action: APPROVE|REJECT|ESCALATE }
  → update moderation_queue
  → append review_audit_log
  → publish Kafka back to originating service (approve/reject post, suspend user)
```

---

## Kafka consumed

`ai.user.insights` · `ai.user.violation.suspected` · `ai.model.updated` · `message.notification.failed`

---

## WebSocket protocol

```
Connect: WS /ws/dashboard/live  (requires role ADMIN or MODERATOR)

Server → Client:
  QUEUE_UPDATE    { queueSize: 23 }
  VIOLATION_ALERT { userId, botScore: 0.95 }
  METRIC_UPDATE   { metric: "flaggedToday", value: 15 }
  MODEL_UPDATED   { modelName: "bert_guard", version: "v20260101" }
```

---

## API

```
GET  /api/v1/dashboard/overview
GET  /api/v1/dashboard/moderation/stats?period=1d|7d|30d
GET  /api/v1/dashboard/moderation/queue?status=PENDING&type=POST|MEDIA|USER
PUT  /api/v1/dashboard/moderation/{id}/review
GET  /api/v1/dashboard/users/violations?minScore=0.7
GET  /api/v1/dashboard/users/bots?minBotScore=0.8
GET  /api/v1/dashboard/models
GET  /api/v1/dashboard/trends?period=24h|7d
WS   /ws/dashboard/live
GET  /api/v1/health
GET  /api/v1/metrics
```

---

## Tests

- **Unit:** `test_dashboard_service.py`, `test_moderation_service.py`
- **Integration:** PostgreSQL + Kafka containers
- **Automation:** flag → queue entry → human review (approve/reject) → audit log verify
