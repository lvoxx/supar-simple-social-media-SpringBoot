# message-notification-service

**Type:** Spring Boot  
**Port:** `8089`  
**Module path:** `spring-services/services/message-notification-service`  
**Database:** Apache Cassandra (reactive)  
**Cache:** Redis  
**Messaging:** Kafka (consumer + producer)  
**Push channels:** FCM · APNs · Web Push (VAPID)  

---

## Trách nhiệm

Dịch vụ push notification chuyên biệt cho tin nhắn riêng tư. Tách khỏi `notification-service` vì yêu cầu: đa kênh push (FCM/APNs/Web Push), ẩn nội dung tin nhắn theo cài đặt quyền riêng tư, batching/deduplication message dồn dập, và vòng đời quản lý device token.

---

## Lý do tách riêng khỏi notification-service

| Yêu cầu | notification-service | message-notification-service |
|---------|---------------------|------------------------------|
| Kênh gửi | WebSocket in-app | FCM + APNs + Web Push + WebSocket |
| Nội dung nhạy cảm | Không | Ẩn nội dung nếu `messagePreview=false` |
| Batching | Không cần | Cần (dồn 10 msg/5s → 1 push) |
| `allowDmFrom` filter | Không áp dụng | Kiểm tra trước khi gửi |
| Token lifecycle | Không | Đăng ký / refresh / deregister per device |

---

## Luồng quyết định gửi push

```
Kafka: message.sent
    │
    ├─► Load participants (từ Redis cache → private-message-svc)
    │
    └─► Với mỗi participant (trừ sender):
          │
          ├── [1] User đang online WS? → SKIP (WS delivery đảm nhiệm)
          │
          ├── [2] allowDmFrom: sender có được phép? → SKIP nếu không
          │
          ├── [3] Per-conv settings:
          │         muteUntil/mutedForever? → SKIP
          │         notifyOn = NONE? → SKIP
          │         notifyOn = MENTIONS_ONLY + không được mention? → SKIP
          │
          ├── [4] Batching (Redis sliding window 5s):
          │         Nếu đã có message pending cùng conv → accumulate
          │         Nếu hết window → flush batch push
          │
          ├── [5] Build payload:
          │         messagePreview=true  → "UserA: Nội dung tin nhắn..."
          │         messagePreview=false → "Tin nhắn mới từ UserA"
          │         GROUP_CHAT           → "GroupName: UserA đã gửi tin nhắn"
          │
          └── [6] Dispatch song song tới tất cả device tokens:
                    FCM → Firebase HTTP v1 API
                    APNs → Apple HTTP/2 APNs
                    Web Push → VAPID RFC 8291/8292
                    Log kết quả → Cassandra (TTL 30 ngày)
```

---

## Batching Algorithm

```
Khi nhận message.sent:
  key = "msg:notif:batch:{userId}:{convId}"
  LPUSH key "{sender}: {preview_snippet}"
  EXPIRE key 5  ← 5 giây window

Sau 5 giây (Reactor scheduled timer):
  items = LRANGE key 0 -1
  DEL key
  
  if len(items) == 1:
    send normal push: "UserA: Xin chào!"
  elif len(items) > 1:
    send batched push: "3 tin nhắn mới từ UserA"
                    hoặc "5 tin nhắn mới trong GroupName"
```

---

## Stale Token Handling

```
FCM response: "NOT_REGISTERED" hoặc "INVALID_ARGUMENT"
  → UPDATE device_push_tokens SET is_active = false
  → Publish log metric: push.token.stale

APNs response: 410 Gone + "Unregistered"
  → Tương tự, deactivate token

Web Push response: 404 hoặc 410
  → Tương tự, deactivate subscription
```

---

## Starters sử dụng

`starter-cassandra` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events Consumed

| Topic | Hành động |
|-------|-----------|
| `message.sent` | Trigger chính — đánh giá và dispatch push |
| `message.reaction.added` | Notify message owner khi có reaction mới |
| `conversation.created` | Notify participants được mời vào conversation mới |
| `conversation.settings.updated` | Invalidate settings cache |

## Kafka Events Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `message.notification.delivered` | Push gửi thành công | private-message-svc |
| `message.notification.failed` | Hết retry, vẫn thất bại | ai-dashboard-svc |

---

## Cassandra Schema

```cql
-- Device push tokens
CREATE TABLE device_push_tokens (
  user_id        UUID,
  device_id      UUID,
  platform       TEXT,        -- FCM_ANDROID|APNS_IOS|WEB_PUSH
  token          TEXT,
  app_version    TEXT,
  registered_at  TIMESTAMP,
  last_active    TIMESTAMP,
  is_active      BOOLEAN DEFAULT TRUE,
  PRIMARY KEY (user_id, device_id)
);

-- Delivery log (append-only, TTL 30 ngày)
CREATE TABLE message_notification_log (
  user_id          UUID,
  notification_id  TIMEUUID,
  conversation_id  UUID,
  message_id       UUID,
  channel          TEXT,        -- WEBSOCKET|FCM|APNS|WEB_PUSH
  status           TEXT,        -- PENDING|SENT|DELIVERED|FAILED|SKIPPED
  sent_at          TIMESTAMP,
  failure_reason   TEXT,
  retry_count      INT DEFAULT 0,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 2592000;   -- 30 ngày
```

---

## Cấu hình

```yaml
xsocial:
  message-notification:
    fcm:
      service-account-key: ${FCM_SERVICE_ACCOUNT_KEY}
      project-id: ${FCM_PROJECT_ID}
    apns:
      key-id: ${APNS_KEY_ID}
      team-id: ${APNS_TEAM_ID}
      private-key-path: ${APNS_PRIVATE_KEY_PATH}
      bundle-id: ${APNS_BUNDLE_ID}
      sandbox: ${APNS_SANDBOX:false}
    web-push:
      vapid-public-key: ${VAPID_PUBLIC_KEY}
      vapid-private-key: ${VAPID_PRIVATE_KEY}
      subject: mailto:push@xsocial.com
    batching:
      window-seconds: 5
      max-batch-size: 20
    retry:
      max-attempts: 3
      backoff-multiplier: 2.0
      initial-interval-ms: 500
    delivery-log-ttl-days: 30
```

---

## API Endpoints

```
# Device token management
POST   /api/v1/message-notifications/devices
  Body: {deviceId, platform, token, appVersion}
PUT    /api/v1/message-notifications/devices/{deviceId}
  Body: {token}
DELETE /api/v1/message-notifications/devices/{deviceId}
GET    /api/v1/message-notifications/devices

# Delivery log
GET    /api/v1/message-notifications/log
  Params: ?page=&size=&convId=&status=

# Admin / Ops
POST   /api/v1/message-notifications/test
  Body: {userId, message}     ← chỉ dùng ở dev/staging
GET    /api/v1/message-notifications/stats
  Requires: role ADMIN
  Returns: { deliveredToday, failedToday, fcmRate, apnsRate, webPushRate }
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `msg:notif:settings:{userId}` | 5 phút | User-level message settings |
| `msg:notif:conv-settings:{userId}:{convId}` | 5 phút | Per-conv notification settings |
| `msg:notif:token:{userId}` | 24 giờ | Serialized list of device tokens |
| `msg:notif:batch:{userId}:{convId}` | 5 giây | Batching sliding window |
| `msg:notif:ws-online:{userId}` | 30 giây | User có đang kết nối WebSocket không |

---

## Cấu trúc source

```
message-notification-service/src/main/java/com/xsocial/msgnotif/
├── config/
│   ├── FcmConfig.java
│   ├── ApnsConfig.java
│   └── WebPushConfig.java
├── domain/
│   ├── entity/DevicePushToken.java, MessageNotificationLog.java
│   └── repository/DeviceTokenRepository.java, NotificationLogRepository.java
├── application/
│   ├── service/
│   │   ├── NotificationDecisionService.java   # core decision logic
│   │   ├── BatchingService.java               # Redis sliding window
│   │   ├── DeviceTokenService.java            # token lifecycle
│   │   └── DeliveryLogService.java
│   └── dto/RegisterDeviceRequest.java, NotificationStats.java
├── infrastructure/
│   ├── kafka/MessageNotificationConsumer.java, NotificationEventPublisher.java
│   ├── push/
│   │   ├── FcmPushDispatcher.java
│   │   ├── ApnsPushDispatcher.java
│   │   └── WebPushDispatcher.java
│   └── redis/
│       ├── BatchingRedisService.java
│       └── OnlineStatusRedisService.java
└── web/
    ├── router/MessageNotificationRouter.java
    └── handler/DeviceTokenHandler.java, AdminHandler.java
```

---

## Tests

### Unit Tests
- `NotificationDecisionServiceTest` — mock settings, test tất cả decision paths (skip, batch, dispatch)
- `BatchingServiceTest` — mock Redis, test sliding window
- `FcmPushDispatcherTest` — mock FCM HTTP response, test stale token handling
- `ApnsPushDispatcherTest` — mock APNs response (200, 410)
- `WebPushDispatcherTest` — mock VAPID endpoint

### Integration Tests (Testcontainers)
- `DeviceTokenRepositoryIT` — Cassandra container
- `NotificationKafkaConsumerIT` — Cassandra + Redis + Kafka
- `PushDispatchIT` — Redis + WireMock FCM/APNs server

### Automation Tests
- `DeviceRegistrationAutomationTest` — register → update → deregister
- `PushDecisionAutomationTest` — Kafka event → mock FCM → verify delivery log
- `BatchingAutomationTest` — 5 rapid messages → verify single batched push
