# media-service

**Type:** Spring Boot · Port `8082`  
**Primary DB:** PostgreSQL (R2DBC) — schema `x_social_media`  
**Cache:** Redis  
**External:** Cloudinary CDN · media-guard-service  
**Starters:** `postgres-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter`

---

## Responsibilities

Centralised media management. Accepts uploads, validates content, compresses/transcodes, delegates to media-guard-service for safety checks, uploads to Cloudinary, and notifies callers via Kafka.

---

## DB init

K8S `Job` runs Flyway CLI before Pod starts.  
Scripts: `infrastructure/k8s/db-init/media-service/sql/V1__init_media_assets.sql`

---

## Schema

```sql
-- media_assets
id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid()
owner_id             UUID        NOT NULL
owner_type           VARCHAR(20) NOT NULL     -- USER | POST | COMMENT | MESSAGE | GROUP
original_filename    TEXT
content_type         VARCHAR(100)
file_size_bytes      BIGINT
cloudinary_public_id TEXT
cloudinary_url       TEXT
cdn_url              TEXT
thumbnail_url        TEXT
width                INT
height               INT
duration_seconds     INT
status               VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
                                            -- PROCESSING | READY | REJECTED | DELETED
rejection_reason     TEXT
created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
updated_at           TIMESTAMPTZ
created_by           UUID
updated_by           UUID
is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE
deleted_at           TIMESTAMPTZ
deleted_by           UUID
```

---

## Async upload pipeline

```
POST /api/v1/media/upload
  → Validate magic bytes + MIME + file size
  → Insert row  status=PROCESSING
  → Return { mediaId, status:"PROCESSING" }

Async (Schedulers.boundedElastic()):
  image  → libvips: resize + compress → WebP
  video  → FFmpeg: transcode H.264/AAC
  → Call media-guard-service (NSFW / deepfake / malware)
  → Upload to Cloudinary
  → Update status = READY or REJECTED
  → Publish Kafka: media.upload.completed / media.upload.failed
```

## File limits

| Type | Max size | Output |
|------|---------|--------|
| Avatar | 2 MB | WebP 400×400 |
| Post image | 8 MB | WebP 2048 px |
| Group background | 5 MB | WebP 1500×500 |
| Video | 512 MB | H.264/AAC 1080p |
| Audio | 50 MB | AAC |
| Attachment | 50 MB | PDF passthrough |

---

## Kafka

### Published

| Topic | Consumers |
|-------|-----------|
| `media.upload.completed` | post-svc, user-svc, comment-svc, private-message-svc |
| `media.upload.failed` | post-svc, user-svc |

---

## API

```
POST   /api/v1/media/upload
GET    /api/v1/media/{mediaId}
GET    /api/v1/media/{mediaId}/status
DELETE /api/v1/media/{mediaId}
POST   /api/v1/internal/media/assign          # service-to-service: bind mediaId to owner
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `media:asset:{mediaId}` | 24 h |
| `media:owner:{ownerId}:list` | 5 min |

---

## Tests

- **Unit:** `MagicByteValidatorTest`, `ImageProcessorTest`, `CloudinaryClientTest`
- **Integration:** PostgreSQL + Kafka + WireMock (Cloudinary, media-guard)
- **Automation:** upload → poll READY → soft delete → verify 404
