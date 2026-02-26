# notification-service

**Type:** Spring Boot  
**Port:** `8085`  
**Module path:** `spring-services/services/notification-service`  
**Database:** Apache Cassandra (reactive)  
**Cache:** Redis  
**Messaging:** Kafka (consumer) · Axon (event consumer)  
**Real-time:** Reactive WebSocket  

---

## Trách nhiệm

Fan-out thông báo từ tất cả domain events đến người dùng. Đẩy real-time qua WebSocket và đồng bộ trạng thái đã đọc giữa nhiều thiết bị.

---

## Lý do chọn Cassandra

| Tiêu chí | Lý do |
|----------|-------|
| Fan-out writes | Một event có thể tạo hàng ngàn notification (viral post) |
| Time-sorted | `TIMEUUID` clustering key — luôn lấy thông báo mới nhất |
| Multi-device sync | Append-only, không cần transaction phức tạp |
| TTL | Notification cũ tự động expire sau 90 ngày |

---

## Kafka Events Consumed

| Topic | Hành động |
|-------|-----------|
| `post.created` | Notify followers của author |
| `post.liked` | Notify post owner |
| `post.reposted` | Notify post owner |
| `comment.created` | Notify post owner + mentioned users |
| `comment.liked` | Notify comment owner |
| `user.followed` | Notify target user |
| `user.verified` | Notify verified user |
| `media.upload.completed` | Notify post/user owner |
| `notification.read` | Multi-device sync — đồng bộ trạng thái đọc |
| `group.member.joined` | Notify group admins |
| `group.post.pinned` | Notify group members |
| `conversation.created` | Notify invited participants |

---

## Axon Events Consumed

| Event | Hành động |
|-------|-----------|
| `UserPreferencesUpdatedEvent` | Cập nhật notification delivery rules per user |

---

## Cassandra Schema

```cql
-- Notifications per user (primary read path)
CREATE TABLE notifications_by_user (
  user_id           UUID,
  notification_id   TIMEUUID,
  type              TEXT,        -- LIKE|COMMENT|FOLLOW|MENTION|REPOST|SYSTEM|GROUP_JOIN|PINNED_POST
  actor_id          UUID,
  actor_username    TEXT,
  actor_avatar_url  TEXT,
  entity_type       TEXT,        -- POST|COMMENT|USER|GROUP
  entity_id         UUID,
  message           TEXT,
  deep_link         TEXT,        -- frontend route: /post/xxx
  is_read           BOOLEAN DEFAULT FALSE,
  is_deleted        BOOLEAN DEFAULT FALSE,
  delivered_at      TIMESTAMP,
  created_at        TIMESTAMP,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 7776000;   -- 90 ngày TTL

-- Device WebSocket sessions (routing cho multi-device)
CREATE TABLE device_sessions (
  user_id           UUID,
  device_id         UUID,
  session_token     TEXT,
  platform          TEXT,        -- WEB|ANDROID|IOS
  pod_id            TEXT,        -- K8S pod ID để route WS message
  last_active       TIMESTAMP,
  PRIMARY KEY (user_id, device_id)
);
```

---

## WebSocket Protocol

```
Connect:  WS /ws/notifications
  Headers: X-User-Id (từ gateway)

Server → Client events:
  NOTIFICATION {
    id, type, message, deepLink,
    actor: { id, username, avatarUrl },
    timestamp
  }
  READ_STATE_UPDATE { notificationIds: [...] }
  UNREAD_COUNT_UPDATE { count: 5 }

Client → Server:
  PING
  MARK_READ { notificationId }
```

### Multi-device Read Sync

```
Thiết bị A đọc notification:
  1. PUT /api/v1/notifications/{id}/read
  2. Cập nhật Cassandra is_read = true
  3. Publish Kafka: notification.read { userId, notificationId }
  4. notification-service consumer nhận event
  5. Push READ_STATE_UPDATE tới tất cả WebSocket session của userId
     (cross-pod routing qua Redis Pub/Sub)
```

---

## Fan-out Strategy

```
Nhận Kafka event (ví dụ: post.liked):
  1. Load danh sách recipient (thường là 1 user — owner)
  2. Kiểm tra UserPreferences: loại notification này có được bật không?
  3. Tạo notification record trong Cassandra
  4. Nếu user đang online (WebSocket session tồn tại):
     → Push trực tiếp qua WebSocket
  5. Nếu user offline:
     → Notification được lưu trong Cassandra, load khi user quay lại
  6. Cập nhật unread count trong Redis: INCR notif:unread:{userId}
```

---

## API Endpoints

```
# Danh sách thông báo
GET    /api/v1/notifications
  Params: ?limit=20&cursor={lastNotifId}&type=ALL|LIKE|COMMENT|...

# Đánh dấu đã đọc
POST   /api/v1/notifications/read-all
PUT    /api/v1/notifications/{id}/read
GET    /api/v1/notifications/unread-count

# Xóa
DELETE /api/v1/notifications/{id}            # soft delete

# WebSocket
WS     /ws/notifications
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `notif:unread:{userId}` | 5 phút | Số thông báo chưa đọc |
| `notif:settings:{userId}` | 10 phút | Notification preferences |
| `notif:ws-sessions:{userId}` | 30 giây | Danh sách pod có WS session |

---

## Tests

### Unit Tests
- `NotificationFanoutServiceTest` — mock Cassandra repository
- `UserPreferenceFilterTest` — kiểm tra logic lọc theo preferences

### Integration Tests (Testcontainers)
- `NotificationRepositoryIT` — Cassandra container
- `NotificationKafkaConsumerIT` — Cassandra + Kafka
- `WebSocketDeliveryIT` — Cassandra + Redis + WebSocket test client

### Automation Tests
- `NotificationApiAutomationTest` — Kafka event → notification created → mark read → multi-device sync
