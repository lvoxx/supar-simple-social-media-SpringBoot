# private-message-service

**Type:** Spring Boot  
**Port:** `8088`  
**Module path:** `spring-services/services/private-message-service`  
**Database:** Apache Cassandra (reactive)  
**Cache:** Redis (Redisson + Pub/Sub)  
**Messaging:** Kafka  
**Real-time:** Reactive WebSocket  

---

## Trách nhiệm

End-to-end private messaging: nhắn tin 1-1 (DM), group chat (tới 500 người), và channel conversation liên kết với social groups. Hỗ trợ media attachments, reactions, message forwarding, read receipts, typing indicators, và per-conversation settings.

---

## Loại Conversation

| Type | Số thành viên | Tạo bởi | Ghi chú |
|------|:------------:|---------|---------|
| `DIRECT` | 2 | Bất kỳ user | Không thêm được thành viên |
| `GROUP_CHAT` | 2–500 | Bất kỳ user | Group chat độc lập (không liên kết social group) |
| `GROUP_CHANNEL` | Không giới hạn* | Auto bởi group-service | Liên kết với social group qua `group_id` |

\* Giới hạn bởi group membership, không bởi bảng participant.

---

## Loại Message

| `message_type` | Mô tả |
|----------------|-------|
| `TEXT` | Văn bản thuần (markdown-lite) |
| `IMAGE` / `VIDEO` / `AUDIO` / `FILE` | Media đính kèm (qua media-service) |
| `STICKER` | Sticker ID tham chiếu |
| `FORWARDED` | Tin nhắn được chuyển tiếp (reference-only, không copy nội dung) |
| `SYSTEM` | Hệ thống: "UserA đã tham gia", "UserB đã rời nhóm" |

---

## Kiến trúc real-time

```
Client A (WebSocket pod-1) ─── gửi message
         │
         ▼
  private-message-service (pod-1):
    1. Lưu vào Cassandra
    2. Publish Redis Pub/Sub: channel "conv:{convId}"
    3. Publish Kafka: message.sent

Redis Pub/Sub:
    ├── Pod-1 nhận → push tới Client A (sender confirmation)
    ├── Pod-2 nhận → push tới Client B (online trên pod-2)
    └── Pod-3 nhận → push tới Client C (online trên pod-3)

Kafka: message.sent → message-notification-service
    → Đẩy FCM/APNs/Web Push cho user offline
```

---

## Cassandra Schema (Keyspace: `x_social_messages`)

```cql
-- Conversation metadata
CREATE TABLE conversations (
  conversation_id  UUID        PRIMARY KEY,
  type             TEXT,
  name             TEXT,
  avatar_url       TEXT,
  group_id         UUID,
  created_by       UUID,
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP,
  settings         TEXT,       -- JSON global settings
  is_deleted       BOOLEAN     DEFAULT FALSE
);

-- Participants
CREATE TABLE conversation_participants (
  conversation_id          UUID,
  user_id                  UUID,
  role                     TEXT DEFAULT 'MEMBER',   -- OWNER|ADMIN|MEMBER
  status                   TEXT DEFAULT 'ACTIVE',   -- ACTIVE|LEFT|REMOVED|MUTED
  joined_at                TIMESTAMP,
  last_read_message_id     UUID,
  last_read_at             TIMESTAMP,
  notification_settings    TEXT,                    -- JSON per-conv settings
  PRIMARY KEY (conversation_id, user_id)
);

-- Reverse lookup: conversations của một user
CREATE TABLE conversations_by_user (
  user_id           UUID,
  last_message_at   TIMESTAMP,
  conversation_id   UUID,
  conversation_type TEXT,
  unread_count      INT,
  is_muted          BOOLEAN,
  PRIMARY KEY (user_id, last_message_at, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, conversation_id ASC);

-- Messages
CREATE TABLE messages (
  conversation_id              UUID,
  message_id                   TIMEUUID,
  sender_id                    UUID,
  message_type                 TEXT,
  content                      TEXT,
  media_ids                    LIST<UUID>,
  forwarded_from_message_id    UUID,
  forwarded_from_conversation_id UUID,
  reply_to_message_id          UUID,
  status                       TEXT,   -- SENT|DELIVERED|READ|FAILED|DELETED
  is_deleted                   BOOLEAN DEFAULT FALSE,
  deleted_at                   TIMESTAMP,
  deleted_by                   UUID,
  edited_at                    TIMESTAMP,
  metadata                     TEXT,   -- JSON: {fileName, fileSize, duration}
  created_at                   TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

-- Reactions
CREATE TABLE message_reactions (
  conversation_id  UUID,
  message_id       TIMEUUID,
  user_id          UUID,
  emoji            TEXT,
  reacted_at       TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);

-- Read receipts
CREATE TABLE message_read_receipts (
  conversation_id  UUID,
  message_id       TIMEUUID,
  user_id          UUID,
  read_at          TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);
```

---

## WebSocket Protocol

```
Connect:  WS /ws/messages
  Headers: X-User-Id (từ gateway)

CLIENT → SERVER:
  { "type": "JOIN_CONVERSATION",  "conversationId": "..." }
  { "type": "LEAVE_CONVERSATION", "conversationId": "..." }
  { "type": "TYPING_START",       "conversationId": "..." }
  { "type": "TYPING_STOP",        "conversationId": "..." }
  { "type": "PING" }

SERVER → CLIENT:
  { "type": "NEW_MESSAGE",        "conversationId": "...", "message": {...} }
  { "type": "MESSAGE_UPDATED",    "conversationId": "...", "messageId": "...", "content": "..." }
  { "type": "MESSAGE_DELETED",    "conversationId": "...", "messageId": "..." }
  { "type": "MESSAGE_REACTION",   "conversationId": "...", "messageId": "...",
                                  "emoji": "👍", "userId": "...", "action": "ADD|REMOVE" }
  { "type": "READ_RECEIPT",       "conversationId": "...", "userId": "...",
                                  "lastReadMessageId": "..." }
  { "type": "TYPING_INDICATOR",   "conversationId": "...", "userId": "...", "typing": true }
  { "type": "PARTICIPANT_JOINED", "conversationId": "...", "userId": "..." }
  { "type": "PARTICIPANT_LEFT",   "conversationId": "...", "userId": "..." }
  { "type": "PONG" }
```

---

## Business Rules quan trọng

### Reaction
- 1 reaction per user per message (PUT semantics: thay đổi emoji = ghi đè).
- Reactions **không** soft delete — được physical delete (không phải user-uploaded content).
- Response trả về aggregate: `[{ emoji: "👍", count: 5, users: [...] }]`.

### Forward
- Tạo message mới với `type=FORWARDED`, `forwarded_from_message_id` = ID gốc.
- Nội dung gốc **không** bị copy — chỉ reference.
- Nếu message gốc bị xóa → hiển thị `[Tin nhắn không còn tồn tại]`.

### Edit
- Chỉ `message_type=TEXT`, trong vòng **15 phút** sau khi gửi.
- Chỉ sender mới được edit.
- `edited_at` được cập nhật; lịch sử edit không lưu (overwrite).

### Soft Delete
- Message content được thay bằng `[Tin nhắn đã bị xóa]` trong response.
- `is_deleted = true`, `content = null` trong Cassandra.

### Read Receipt Privacy
- Nếu `user.settings.readReceipts = false`: UPDATE vẫn lưu nội bộ nhưng **không** broadcast `READ_RECEIPT` WS event ra ngoài.

---

## Per-Conversation Settings

```json
{
  "muteUntil": "2026-06-01T00:00:00Z",
  "mutedForever": false,
  "notifyOn": "ALL_MESSAGES",
  "theme": "BLUE",
  "nickname": "Bob",
  "messagePreview": true
}
```

Lưu trong `conversation_participants.notification_settings`.

---

## User-Level Message Settings

```json
{
  "allowDmFrom": "FOLLOWING",
  "readReceipts": true,
  "typingIndicators": true,
  "messagePreview": true,
  "archiveInactiveAfterDays": 30
}
```

Lưu trong `user-service.notification_settings.message`. Propagate sang cache qua Kafka `user.profile.updated`.

---

## Starters sử dụng

`cassandra-starter` · `redis-starter` · `kafka-starter` · `metrics-starter` · `security-starter` · `websocket-starter`

---

## Kafka Events Published

| Topic | Payload chính | Consumers |
|-------|--------------|-----------|
| `message.sent` | convId, msgId, senderId, participants, type, preview | message-notification-svc, user-analysis-svc |
| `message.delivered` | convId, msgId, recipientId | (sync) |
| `message.read` | convId, lastReadMessageId, userId | (multi-device) |
| `message.reaction.added` | convId, msgId, emoji, userId | message-notification-svc |
| `message.reaction.removed` | convId, msgId, userId | (internal) |
| `message.deleted` | convId, msgId | (internal) |
| `conversation.created` | convId, type, participants | notification-svc |
| `conversation.settings.updated` | convId, userId, settings | message-notification-svc |

## Kafka Events Consumed

| Topic | Hành động |
|-------|-----------|
| `user.profile.updated` | Invalidate participant name/avatar cache |
| `group.member.left` | Remove participant khỏi GROUP_CHANNEL conversation |
| `group.deleted` | Archive GROUP_CHANNEL conversation |

---

## API Endpoints

```
# Conversations
POST   /api/v1/messages/conversations
GET    /api/v1/messages/conversations
GET    /api/v1/messages/conversations/{convId}
PUT    /api/v1/messages/conversations/{convId}
DELETE /api/v1/messages/conversations/{convId}
POST   /api/v1/messages/conversations/{convId}/mute
POST   /api/v1/messages/conversations/{convId}/settings

# Participants
GET    /api/v1/messages/conversations/{convId}/participants
POST   /api/v1/messages/conversations/{convId}/participants
DELETE /api/v1/messages/conversations/{convId}/participants/{userId}
PUT    /api/v1/messages/conversations/{convId}/participants/{userId}/role

# Messages
POST   /api/v1/messages/conversations/{convId}/messages
GET    /api/v1/messages/conversations/{convId}/messages
PUT    /api/v1/messages/conversations/{convId}/messages/{msgId}
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/forward
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/react
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}/react
POST   /api/v1/messages/conversations/{convId}/messages/read
GET    /api/v1/messages/conversations/{convId}/messages/{msgId}/reactions

# WebSocket
WS     /ws/messages
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `msg:conv:{convId}` | 5 phút | Conversation metadata |
| `msg:participants:{convId}` | 5 phút | Participant list + roles |
| `msg:unread:{userId}:{convId}` | 1 phút | Unread count |
| `msg:conv-list:{userId}:page:0` | 30 giây | First page of conversations |
| `msg:settings:user:{userId}` | 5 phút | User-level message settings |
| `msg:settings:conv:{userId}:{convId}` | 5 phút | Per-conv notification settings |

---

## Tests

### Unit Tests
- `ConversationServiceTest` — DM creation, duplicate prevention
- `MessageServiceTest` — send, edit (15min rule), soft delete
- `ReactionServiceTest` — add, change, remove reaction
- `ForwardServiceTest` — forward logic, deleted source handling

### Integration Tests (Testcontainers)
- `MessageRepositoryIT` — Cassandra container
- `WebSocketDeliveryIT` — Cassandra + Redis Pub/Sub + WS test client
- `MessageKafkaIT` — Cassandra + Kafka

### Automation Tests
- `DmFlowAutomationTest` — create DM → send → react → forward → delete
- `GroupChatAutomationTest` — create group chat → add participants → send → read receipt
- `ReadReceiptPrivacyTest` — verify read receipt not broadcast when setting=false
