# media-service

**Type:** Spring Boot  
**Port:** `8082`  
**Module path:** `spring-services/services/media-service`  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis  
**External:** Cloudinary CDN  
**AI guard:** media-guard-service (FastAPI)  

---

## Trách nhiệm

Dịch vụ lưu trữ và xử lý media trung tâm. Tiếp nhận file upload, kiểm tra bảo mật, nén/transcode, upload lên Cloudinary CDN, và thông báo kết quả về service gốc qua Kafka. Được dùng bởi: `user-service`, `post-service`, `comment-service`, `private-message-service`, `group-service`.

---

## Luồng upload async

```
1. Service gọi  →  POST /api/v1/media/upload (multipart)
2. media-service:
   a. Kiểm tra magic bytes (từ chối exe, archive ngụy trang, script)
   b. Kiểm tra kích thước theo loại file
   c. Lưu metadata với status = PROCESSING
   d. Trả về ngay: { mediaId, status: "PROCESSING" }
3. Async (Schedulers.boundedElastic()):
   a. Image  → libvips: resize + compress → WebP
   b. Video  → FFmpeg: transcode H.264/AAC
4. Gọi media-guard-service (FastAPI):
   a. NSFW classification (CLIP)
   b. Deepfake detection
   c. Malware entropy check
5. Upload lên Cloudinary
6. Cập nhật metadata:  status = READY  hoặc  REJECTED
7. Publish Kafka:  media.upload.completed  /  media.upload.failed
8. Service gốc consume event → tiếp tục workflow
```

---

## Giới hạn file

| Loại | Kích thước tối đa | Output |
|------|------------------|--------|
| Avatar | 2 MB | WebP, max 400×400 px |
| Ảnh post | 8 MB | WebP, max 2048 px (longest side) |
| Ảnh background | 5 MB | WebP, max 1500×500 px |
| Video | 512 MB | H.264/AAC, max 1080p, max 10 phút |
| Audio | 50 MB | AAC, max 30 phút |
| File đính kèm | 50 MB | PDF pass-through |
| Sticker | 1 MB | WebP animated cho phép |

---

## Media Guard Rules (inline)

| Kiểm tra | Phương thức | Kết quả |
|----------|-------------|---------|
| Magic byte | File header inspection | Reject nếu không khớp extension |
| MIME type | Apache Tika | Reject nếu MIME không trong whitelist |
| Entropy analysis | Shannon entropy | Flag nếu entropy bất thường (polyglot, hidden payload) |
| NSFW | CLIP + classifier từ media-guard-svc | Reject nếu confidence > threshold |
| Deepfake | Face detection + frequency domain | Flag để human review |

---

## Starters sử dụng

`starter-postgres` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events

### Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `media.upload.completed` | Upload thành công, URL sẵn sàng | post-svc, user-svc, comment-svc, private-message-svc |
| `media.upload.failed` | Upload thất bại hoặc nội dung bị từ chối | post-svc, user-svc |

### Consumed

*(Không consume event nào — inbound only)*

---

## Database Schema

```sql
-- media_assets
id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid()
owner_id             UUID        NOT NULL
owner_type           VARCHAR(20) NOT NULL    -- USER | POST | COMMENT | MESSAGE | GROUP
original_filename    TEXT
content_type         VARCHAR(100)
file_size_bytes      BIGINT
cloudinary_public_id TEXT
cloudinary_url       TEXT
cdn_url              TEXT                    -- transformed CDN URL
thumbnail_url        TEXT
width                INT
height               INT
duration_seconds     INT                     -- video/audio
status               VARCHAR(20)             -- PROCESSING | READY | REJECTED | DELETED
rejection_reason     TEXT
is_deleted           BOOLEAN     DEFAULT FALSE
deleted_at           TIMESTAMPTZ
deleted_by           UUID
created_at           TIMESTAMPTZ DEFAULT NOW()
updated_at           TIMESTAMPTZ
```

---

## API Endpoints

```
# Upload
POST   /api/v1/media/upload
  Content-Type: multipart/form-data
  Fields: file, ownerType, ownerId (optional - set later by service)
  Response: { mediaId, status: "PROCESSING", estimatedProcessingSeconds }

# Truy vấn
GET    /api/v1/media/{mediaId}              # metadata + URL
GET    /api/v1/media/{mediaId}/status       # polling: PROCESSING | READY | REJECTED

# Quản lý
DELETE /api/v1/media/{mediaId}              # soft delete (owner only)
GET    /api/v1/media/owner/{ownerId}        # list media của một owner

# Internal (service-to-service, không expose ra gateway)
POST   /api/v1/internal/media/assign        # gán mediaId cho owner sau khi entity được tạo
```

---

## Tích hợp Cloudinary

```yaml
cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}
  upload-preset: x-social-media
  transformations:
    avatar:     "c_fill,w_400,h_400,q_auto,f_webp"
    post-image: "c_limit,w_2048,q_auto:good,f_webp"
    thumbnail:  "c_fill,w_320,h_180,q_auto,f_webp"
```

---

## Cache Keys

| Key | TTL |
|-----|-----|
| `media:asset:{mediaId}` | 24 giờ |
| `media:owner:{ownerId}:list` | 5 phút |

---

## Cấu trúc source

```
media-service/src/main/java/com/xsocial/media/
├── config/CloudinaryConfig.java
├── domain/
│   ├── entity/MediaAsset.java
│   └── repository/MediaAssetRepository.java
├── application/
│   ├── service/MediaUploadService.java, MediaProcessingService.java
│   ├── guard/MagicByteValidator.java, FileSizeValidator.java
│   └── dto/UploadResponse.java, MediaAssetResponse.java
├── infrastructure/
│   ├── kafka/MediaEventPublisher.java
│   ├── cloudinary/CloudinaryUploadClient.java
│   ├── processing/ImageProcessor.java, VideoProcessor.java
│   └── external/MediaGuardClient.java
└── web/
    ├── router/MediaRouter.java
    └── handler/MediaHandler.java
```

---

## Tests

### Unit Tests
- `MagicByteValidatorTest` — test các loại file hợp lệ/không hợp lệ
- `ImageProcessorTest` — mock libvips, kiểm tra output kích thước
- `CloudinaryUploadClientTest` — mock HTTP response

### Integration Tests (Testcontainers)
- `MediaUploadServiceIT` — PostgreSQL + Kafka + WireMock Cloudinary
- `MediaGuardClientIT` — WireMock media-guard-service

### Automation Tests
- `MediaUploadApiTest` — upload image → poll status → verify READY
- `MediaRejectTest` — upload file bất thường → verify REJECTED
