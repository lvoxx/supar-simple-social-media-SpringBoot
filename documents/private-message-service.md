# private-message-service

**Type:** Spring Boot · Port `8088`  
**Primary DB:** Apache Cassandra — keyspace `x_social_messages`  
**Cache:** Redis (metadata cache + Pub/Sub for cross-pod WS routing)  
**Starters:** `cassandra-starter` `redis-starter` `kafka-starter` `metrics-starter` `security-starter` `websocket-starter`

---

## Responsibilities

End-to-end private messaging: 1-on-1 DMs, user-created group chats (up to 500 members), and group-channel conversations linked to social groups. Handles real-time WebSocket delivery, reactions, forwarding, read receipts, typing indicators, per-conversation notification settings, and media attachments.

---

## Conversation types

| Type | Max members | Created by |
|------|:-----------:|-----------|
| `DIRECT` | 2 | Either user |
| `GROUP_CHAT` | 500 | Any user |
| `GROUP_CHANNEL` | Unlimited* | group-service (auto) |

\* Bounded by group membership.

---

## Real-time architecture

```
Sender (WS pod-1)
  │ send message
  ▼
private-message-service (pod-1):
  1. Write to Cassandra
  2. PUBLISH Redis channel "conv:{convId}"   ← cross-pod routing
  3. Publish Kafka: message.sent             ← offline push

Redis Pub/Sub fanout:
  pod-1 → deliver to sender (confirmation)
  pod-2 → deliver to recipient B (online)
  pod-3 → deliver to recipient C (online)

Kafka message.sent → message-notification-service → FCM / APNs / Web Push
```

---

## DB init

K8S `InitContainer` runs `cqlsh`.  
Scripts: `infrastructure/k8s/db-init/private-message-service/cql/`  
`spring.cassandra.schema-action: NONE`

---

## Schema (keyspace `x_social_messages`)

```cql
CREATE TABLE conversations (
  conversation_id UUID       PRIMARY KEY,
  type            TEXT,                      -- DIRECT|GROUP_CHAT|GROUP_CHANNEL
  name            TEXT,
  avatar_url      TEXT,
  group_id        UUID,
  created_by      UUID,
  created_at      TIMESTAMP,
  updated_at      TIMESTAMP,
  settings        TEXT,                      -- JSON global conversation settings
  is_deleted      BOOLEAN DEFAULT FALSE
);

CREATE TABLE conversation_participants (
  conversation_id       UUID,
  user_id               UUID,
  role                  TEXT DEFAULT 'MEMBER',  -- OWNER|ADMIN|MEMBER
  status                TEXT DEFAULT 'ACTIVE',  -- ACTIVE|LEFT|REMOVED|MUTED
  joined_at             TIMESTAMP,
  last_read_message_id  UUID,
  last_read_at          TIMESTAMP,
  notification_settings TEXT,                   -- JSON per-conv settings
  PRIMARY KEY (conversation_id, user_id)
);

-- Reverse lookup: all conversations for a user, sorted by latest message
CREATE TABLE conversations_by_user (
  user_id           UUID,
  last_message_at   TIMESTAMP,
  conversation_id   UUID,
  conversation_type TEXT,
  unread_count      INT,
  is_muted          BOOLEAN,
  PRIMARY KEY (user_id, last_message_at, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, conversation_id ASC);

CREATE TABLE messages (
  conversation_id                UUID,
  message_id                     TIMEUUID,
  sender_id                      UUID,
  message_type                   TEXT,    -- TEXT|IMAGE|VIDEO|AUDIO|FILE|STICKER|FORWARDED|SYSTEM
  content                        TEXT,
  media_ids                      LIST<UUID>,
  forwarded_from_message_id      UUID,
  forwarded_from_conversation_id UUID,
  reply_to_message_id            UUID,
  status                         TEXT,    -- SENT|DELIVERED|READ|FAILED|DELETED
  is_deleted                     BOOLEAN DEFAULT FALSE,
  deleted_at                     TIMESTAMP,
  deleted_by                     UUID,
  edited_at                      TIMESTAMP,
  metadata                       TEXT,    -- JSON: {fileName, fileSize, duration}
  created_at                     TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

CREATE TABLE message_reactions (
  conversation_id UUID,
  message_id      TIMEUUID,
  user_id         UUID,
  emoji           TEXT,
  reacted_at      TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);

CREATE TABLE message_read_receipts (
  conversation_id UUID,
  message_id      TIMEUUID,
  user_id         UUID,
  read_at         TIMESTAMP,
  PRIMARY KEY (conversation_id, message_id, user_id)
);
```

---

## Business rules

| Rule | Detail |
|------|--------|
| Reaction | 1 per user per message (PUT semantics). Changing emoji overwrites. Physical delete allowed (not user-generated content). |
| Forward | Creates new `FORWARDED` message in target conv. Original is NOT copied — reference only. If source deleted → show `[Message unavailable]`. |
| Edit | `TEXT` type only, within 15 min of send, sender only. Overwrites content, sets `edited_at`. |
| Soft delete | `is_deleted=true`, content replaced by `[Message deleted]` in responses. |
| Read receipt privacy | If `user.settings.readReceipts=false` → Cassandra still updated, but `READ_RECEIPT` WS event NOT broadcast. |

---

## Per-conversation settings (JSON in `notification_settings` column)

```json
{
  "muteUntil":      "2026-06-01T00:00:00Z",
  "mutedForever":   false,
  "notifyOn":       "ALL_MESSAGES",
  "theme":          "BLUE",
  "nickname":       "Bob",
  "messagePreview": true
}
```

---

## WebSocket protocol

```
Connect: WS /ws/messages

Client → Server:
  JOIN_CONVERSATION  / LEAVE_CONVERSATION  { conversationId }
  TYPING_START       / TYPING_STOP         { conversationId }
  PING

Server → Client:
  NEW_MESSAGE         { conversationId, message }
  MESSAGE_UPDATED     { conversationId, messageId, content }
  MESSAGE_DELETED     { conversationId, messageId }
  MESSAGE_REACTION    { conversationId, messageId, emoji, userId, action: ADD|REMOVE }
  READ_RECEIPT        { conversationId, userId, lastReadMessageId }
  TYPING_INDICATOR    { conversationId, userId, typing: bool }
  PARTICIPANT_JOINED  { conversationId, userId }
  PARTICIPANT_LEFT    { conversationId, userId }
  PONG
```

---

## Kafka

### Published

| Topic | Consumers |
|-------|-----------|
| `message.sent` | message-notification-svc, user-analysis-svc |
| `message.delivered` | (multi-device sync) |
| `message.read` | (multi-device sync) |
| `message.reaction.added` | message-notification-svc |
| `message.reaction.removed` | (internal) |
| `message.deleted` | (internal) |
| `conversation.created` | notification-svc |
| `conversation.settings.updated` | message-notification-svc |

### Consumed

| Topic | Action |
|-------|--------|
| `user.profile.updated` | Invalidate participant name/avatar cache |
| `group.member.left` | Remove from GROUP_CHANNEL participants |
| `group.deleted` | Archive GROUP_CHANNEL conversation |

---

## API

```
POST   /api/v1/messages/conversations
GET    /api/v1/messages/conversations
GET    /api/v1/messages/conversations/{convId}
PUT    /api/v1/messages/conversations/{convId}
DELETE /api/v1/messages/conversations/{convId}
POST   /api/v1/messages/conversations/{convId}/mute
POST   /api/v1/messages/conversations/{convId}/settings

GET    /api/v1/messages/conversations/{convId}/participants
POST   /api/v1/messages/conversations/{convId}/participants
DELETE /api/v1/messages/conversations/{convId}/participants/{userId}
PUT    /api/v1/messages/conversations/{convId}/participants/{userId}/role

POST   /api/v1/messages/conversations/{convId}/messages
GET    /api/v1/messages/conversations/{convId}/messages
PUT    /api/v1/messages/conversations/{convId}/messages/{msgId}
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/forward
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/react
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}/react
POST   /api/v1/messages/conversations/{convId}/messages/read
GET    /api/v1/messages/conversations/{convId}/messages/{msgId}/reactions

WS     /ws/messages
```

---

## Cache keys

| Key | TTL |
|-----|-----|
| `msg:conv:{convId}` | 5 min |
| `msg:participants:{convId}` | 5 min |
| `msg:unread:{userId}:{convId}` | 1 min |
| `msg:conv-list:{userId}:page:0` | 30 s |
| `msg:settings:user:{userId}` | 5 min |
| `msg:settings:conv:{userId}:{convId}` | 5 min |

---

## Tests

- **Unit:** `ConversationServiceTest`, `MessageServiceTest`, `ReactionServiceTest`, `ForwardServiceTest`
- **Integration:** Cassandra + Redis + Kafka containers
- **Automation:** create DM → send → react → forward → delete → read receipt → group chat flow
