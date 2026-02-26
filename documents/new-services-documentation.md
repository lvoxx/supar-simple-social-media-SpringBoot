# New Services Documentation

---

# group-service

**Tech:** Spring Boot 4.0.2 · PostgreSQL (R2DBC) · Redis · Kafka · WebClient (post-service, media-service)  
**Port:** 8087  
**Module path:** `spring-services/services/group-service`

## Responsibilities

Manages social groups: creation, membership lifecycle, roles & permissions, join screening (bot prevention), group policies, content pinning, tags, and group settings. Post creation inside groups is **delegated to post-service** — group-service only manages group context, pinned posts, and the group-to-post association.

## Key Features

| Feature | Detail |
|---------|--------|
| Group visibility | `PUBLIC` (anyone can find & join), `PRIVATE` (find but must request), `INVITE_ONLY` (hidden, invite-required) |
| Membership roles | `OWNER` → `ADMIN` → `MODERATOR` → `MEMBER` (hierarchical permissions) |
| Join screening | Private groups can define 1–5 required questions answered on join request |
| Anti-bot policy | `minAccountAgeDays`, `requireAnswers`, manual MODERATOR review |
| Pinned posts | Max 5 pins per group, ordered, MODERATOR+ required |
| Group rules | Ordered list of rule items `{title, description}` displayed to all members |
| Group policy | JSON document controlling auto-approve, DM permissions, post rights, invite rights |
| Tags | Searchable tag array (max 10), used by search-service |
| Avatar & background | Uploaded via media-service (same flow as user avatars) |
| Member moderation | Ban (with reason + audit), mute (suppress notifications), remove |
| Activity log | All admin actions logged in `group_member_activity` for audit trail |

## Starters Used

`starter-postgres`, `starter-redis`, `starter-kafka`, `starter-metrics`, `starter-security`

## Kafka Events Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `group.created` | New group created | search-svc, user-analysis-svc |
| `group.updated` | Name/description/tags updated | search-svc |
| `group.deleted` | Group soft deleted | search-svc, private-message-svc |
| `group.member.joined` | Member approved/joined | notification-svc, user-analysis-svc |
| `group.member.left` | Member left or removed | notification-svc, private-message-svc |
| `group.member.role.changed` | Role promoted/demoted | notification-svc |
| `group.member.banned` | Member banned | notification-svc |
| `group.post.pinned` | Post pinned by moderator | notification-svc |
| `group.post.created` | Post created in group context | search-svc (group post index) |

## Kafka Events Consumed

| Topic | Action |
|-------|--------|
| `post.created` | If `groupId` present, register group-post association |
| `post.deleted` | Remove from `group_pinned_posts` if pinned |
| `user.profile.updated` | Invalidate member display cache |

## Permission Matrix

| Action | MEMBER | MODERATOR | ADMIN | OWNER |
|--------|--------|-----------|-------|-------|
| View group & members | ✅ | ✅ | ✅ | ✅ |
| Create post in group | ✅* | ✅ | ✅ | ✅ |
| Pin/unpin post | ❌ | ✅ | ✅ | ✅ |
| Approve join requests | ❌ | ✅ | ✅ | ✅ |
| Mute / remove member | ❌ | ✅ | ✅ | ✅ |
| Ban member | ❌ | ✅ | ✅ | ✅ |
| Update rules / policy | ❌ | ❌ | ✅ | ✅ |
| Change member roles (up to MODERATOR) | ❌ | ❌ | ✅ | ✅ |
| Delete group | ❌ | ❌ | ❌ | ✅ |
| Transfer ownership | ❌ | ❌ | ❌ | ✅ |

\* Only if `policy.allowMemberPost = true`

## Database Schema

Flyway migrations: `V1__init_groups.sql`, `V2__add_join_questions.sql`, `V3__add_invitations.sql`, `V4__add_activity_log.sql`

Tables: `groups`, `group_members`, `group_join_questions`, `group_pinned_posts`, `group_invitations`, `group_join_requests`, `group_member_activity`, `group_post_associations`

```sql
-- group_post_associations (lightweight join of group ↔ post, actual post in post-service)
group_id UUID,
post_id UUID,
posted_by UUID,
status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, PENDING_APPROVAL, REJECTED
is_pinned BOOLEAN DEFAULT FALSE,
created_at TIMESTAMPTZ,
PRIMARY KEY (group_id, post_id)
```

## Full API Surface

```
-- Group CRUD
POST   /api/v1/groups
GET    /api/v1/groups/{groupId}
PUT    /api/v1/groups/{groupId}
DELETE /api/v1/groups/{groupId}
PUT    /api/v1/groups/{groupId}/avatar
PUT    /api/v1/groups/{groupId}/background
PUT    /api/v1/groups/{groupId}/rules
PUT    /api/v1/groups/{groupId}/policy
PUT    /api/v1/groups/{groupId}/transfer-ownership

-- Membership
GET    /api/v1/groups/{groupId}/members
GET    /api/v1/groups/{groupId}/members/{userId}
PUT    /api/v1/groups/{groupId}/members/{userId}/role
DELETE /api/v1/groups/{groupId}/members/{userId}
POST   /api/v1/groups/{groupId}/members/{userId}/ban
DELETE /api/v1/groups/{groupId}/members/{userId}/ban
POST   /api/v1/groups/{groupId}/members/{userId}/mute

-- Join / Leave
POST   /api/v1/groups/{groupId}/join
POST   /api/v1/groups/{groupId}/leave

-- Join requests (for PRIVATE / INVITE_ONLY)
GET    /api/v1/groups/{groupId}/join-requests
PUT    /api/v1/groups/{groupId}/join-requests/{reqId}    # {action: APPROVE|REJECT}

-- Invitations
POST   /api/v1/groups/{groupId}/invite                   # {userId}
GET    /api/v1/groups/{groupId}/invitations
POST   /api/v1/groups/{groupId}/invitations/{invId}/respond  # {action: ACCEPT|DECLINE}
DELETE /api/v1/groups/{groupId}/invitations/{invId}

-- Posts
GET    /api/v1/groups/{groupId}/posts
POST   /api/v1/groups/{groupId}/posts                    # delegates to post-service
GET    /api/v1/groups/{groupId}/posts/pinned
POST   /api/v1/groups/{groupId}/posts/{postId}/pin
DELETE /api/v1/groups/{groupId}/posts/{postId}/pin

-- Join questions
GET    /api/v1/groups/{groupId}/join-questions
PUT    /api/v1/groups/{groupId}/join-questions

-- Discovery
GET    /api/v1/groups/me/joined
GET    /api/v1/groups/me/owned
GET    /api/v1/groups/search?q=&tags=&category=

-- Admin audit
GET    /api/v1/groups/{groupId}/activity-log
```

## Cache Keys

```
group:detail:{groupId}                 TTL 5 min
group:members:count:{groupId}          TTL 1 min
group:pinned:{groupId}                 TTL 2 min
group:membership:{userId}:{groupId}    TTL 5 min     # {isMember, role, status}
group:policy:{groupId}                 TTL 10 min
```

## Tests

- **Unit:** GroupService, MembershipService, JoinRequestService, PolicyEvaluator (all I/O mocked)
- **Integration:** Testcontainers (postgres, redis, kafka); full join-request-approval flow, ban/unban cycle
- **Automation:** WebTestClient E2E — create group, join (public/private), approve, pin post, transfer ownership

---

# private-message-service

**Tech:** Spring Boot 4.0.2 · **Cassandra** · Redis · Kafka · Reactive WebSocket  
**Port:** 8088  
**Module path:** `spring-services/services/private-message-service`

## Responsibilities

End-to-end private messaging: direct messages (1-on-1), group chats (up to 500 members), and group-linked channel conversations. Handles real-time WebSocket delivery, reactions, message forwarding, read receipts, typing indicators, per-conversation settings, and media attachments.

## Architecture Overview

```
Client A (WebSocket) ──┐
                        ├──► private-message-service ──► Kafka: message.sent
Client B (WebSocket) ──┘          │                         │
                                   │                         ▼
                               Redis Pub/Sub        message-notification-svc
                           (internal WS routing)      (push for offline users)
```

- **Online delivery:** WebSocket channel; participants already subscribed receive `NEW_MESSAGE` push directly.
- **Offline delivery:** Kafka `message.sent` → `message-notification-service` handles FCM/APNs/Web Push.
- **Multi-pod WebSocket:** Redis Pub/Sub used for cross-pod message fanout (if WS connection is on a different pod than the sender).

## Conversation Types

| Type | Max Members | Created By | Notes |
|------|------------|-----------|-------|
| `DIRECT` | 2 | Either user | Cannot add participants; soft-deleted per participant |
| `GROUP_CHAT` | 500 | Any user | User-defined group chat independent of social groups |
| `GROUP_CHANNEL` | Unlimited* | group-service (auto) | Tied to a social group; admins = group admins |

\* GROUP_CHANNEL limits enforced by group membership, not by conversation participant table.

## Message Types

| Type | Description |
|------|-------------|
| `TEXT` | Plain text, markdown-lite |
| `IMAGE` / `VIDEO` / `AUDIO` / `FILE` | Media attachment (via media-service) |
| `STICKER` | Predefined sticker ID references |
| `FORWARDED` | Reference to another message; original content not copied |
| `SYSTEM` | System-generated: `"UserA joined"`, `"UserB left"` |

## Reaction Rules

- Emoji reactions stored as Unicode codepoints.
- 1 active reaction per user per message (PUT semantics — changing emoji replaces previous).
- Reactions physically deletable (not user-uploaded content).
- Aggregate reaction counts returned with message response (grouped by emoji).

## Forward Logic

```
User A forwards msg-X (from conv-1) to conv-2:
  1. Create new message in conv-2 with type=FORWARDED
  2. Set forwarded_from_message_id = msg-X.id
  3. Set forwarded_from_conversation_id = conv-1.id
  4. If original message is deleted: show "[Forwarded message no longer available]"
  5. Original message NOT physically copied — reference only
```

## Read Receipts

```
User reads up to message_id M:
  1. UPDATE conversation_participants SET last_read_message_id = M, last_read_at = NOW()
  2. Publish WS event READ_RECEIPT to all participants
  3. Publish Kafka: message.read (for multi-device sync)
  4. Update unread count in conversations_by_user

Privacy: if user.settings.readReceipts = false → UPDATE still saved internally,
  but READ_RECEIPT WS event is NOT broadcast to other participants.
```

## Per-Conversation Settings (stored in `conversation_participants.notification_settings`)

```json
{
  "muteUntil": "2026-06-01T00:00:00Z",  // or null
  "mutedForever": false,
  "notifyOn": "ALL_MESSAGES",            // ALL_MESSAGES | MENTIONS_ONLY | NONE
  "theme": "BLUE",                       // color theme
  "nickname": "Bob",                     // DIRECT only — custom nickname for the other user
  "messagePreview": true                 // whether push body shows content
}
```

## User-Level Message Settings (stored in user-service, propagated via Kafka)

```json
{
  "allowDmFrom": "FOLLOWING",            // EVERYONE | FOLLOWING | NONE
  "readReceipts": true,
  "typingIndicators": true,
  "messagePreview": true,
  "archiveInactiveAfterDays": 30
}
```

## Starters Used

`starter-cassandra`, `starter-redis`, `starter-kafka`, `starter-metrics`, `starter-security`, `starter-websocket`

## Kafka Events Published

| Topic | Payload | Consumers |
|-------|---------|-----------|
| `message.sent` | conversationId, messageId, senderId, participants, type, preview | message-notification-svc, user-analysis-svc |
| `message.delivered` | conversationId, messageId, recipientId | (internal sync) |
| `message.read` | conversationId, lastReadMessageId, userId | (multi-device) |
| `message.reaction.added` | conversationId, messageId, emoji, userId | message-notification-svc |
| `message.reaction.removed` | conversationId, messageId, userId | (internal) |
| `message.forwarded` | sourceConvId, targetConvId, userId | (internal) |
| `message.deleted` | conversationId, messageId | (internal) |
| `conversation.created` | conversationId, type, participants | notification-svc |
| `conversation.settings.updated` | conversationId, userId, settings | message-notification-svc |

## Cassandra Schema

```cql
-- Full schema as defined in PROMPT SERVICE 8
-- Key indexes:
CREATE INDEX ON conversation_participants (user_id);  -- find convs by user
CREATE INDEX ON messages (sender_id);                  -- find msgs by sender
```

## Full API Surface

```
-- Conversations
POST   /api/v1/messages/conversations
GET    /api/v1/messages/conversations
GET    /api/v1/messages/conversations/{convId}
PUT    /api/v1/messages/conversations/{convId}
DELETE /api/v1/messages/conversations/{convId}
POST   /api/v1/messages/conversations/{convId}/mute
POST   /api/v1/messages/conversations/{convId}/settings

-- Participants
GET    /api/v1/messages/conversations/{convId}/participants
POST   /api/v1/messages/conversations/{convId}/participants
DELETE /api/v1/messages/conversations/{convId}/participants/{userId}
PUT    /api/v1/messages/conversations/{convId}/participants/{userId}/role

-- Messages
POST   /api/v1/messages/conversations/{convId}/messages
GET    /api/v1/messages/conversations/{convId}/messages
PUT    /api/v1/messages/conversations/{convId}/messages/{msgId}
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/forward
POST   /api/v1/messages/conversations/{convId}/messages/{msgId}/react
DELETE /api/v1/messages/conversations/{convId}/messages/{msgId}/react
POST   /api/v1/messages/conversations/{convId}/messages/read
GET    /api/v1/messages/conversations/{convId}/messages/{msgId}/reactions

-- WebSocket
WS     /ws/messages
```

## Cache Keys

```
msg:conv:{convId}                     TTL 5 min    # conversation metadata
msg:participants:{convId}             TTL 5 min    # participant list + roles
msg:unread:{userId}:{convId}          TTL 1 min    # unread count
msg:conv-list:{userId}:page:0         TTL 30 s     # first page of conversations
msg:settings:user:{userId}            TTL 5 min    # user-level message settings
msg:settings:conv:{userId}:{convId}   TTL 5 min    # per-conv notification settings
```

## Tests

- **Unit:** ConversationService, MessageService, ReactionService, TypingIndicatorService, ForwardService
- **Integration:** Testcontainers (Cassandra, Redis, Kafka); full send/read/react/forward flows
- **Automation:** WebTestClient — create DM, send text + media, react, forward, delete, read receipt

---

# message-notification-service

**Tech:** Spring Boot 4.0.2 · **Cassandra** · Redis · Kafka  
**Port:** 8089  
**Module path:** `spring-services/services/message-notification-service`

## Responsibilities

Dedicated push notification microservice for private messages. Handles multi-channel delivery (FCM, APNs, Web Push), per-user/per-conversation notification settings, message preview masking, batching/deduplication of rapid messages, stale token cleanup, and delivery logging.

## Why a Separate Service?

The general `notification-service` handles social notifications (likes, follows, mentions). Message notifications need:

1. **Different delivery channels** — FCM, APNs, Web Push (vs. in-app WebSocket only).
2. **Privacy-sensitive content** — `messagePreview` masking per user setting.
3. **Batching logic** — collapse 10 rapid messages from same conversation → 1 push.
4. **Tight integration** with `allowDmFrom` user preference.
5. **Token lifecycle management** — register, refresh, deregister per-device per-platform.

## Push Channels

| Channel | Platform | Library |
|---------|----------|---------|
| FCM (Firebase Cloud Messaging) | Android + Web | `firebase-admin` SDK via WebClient |
| APNs (Apple Push Notification service) | iOS + macOS | JWT-based APNs HTTP/2 via WebClient |
| Web Push | Browser | RFC 8291/8292 VAPID via `nl.martijndwars:web-push` |

## Notification Decision Flow

```
Kafka: message.sent received
  │
  ├─► Load all target participants (from Redis cache or WebClient to private-message-svc)
  │
  └─► For each participant (excluding sender):
        │
        ├─── [Check] Is user currently connected via WebSocket?
        │      └── YES → skip (WS delivery handles it)
        │
        ├─── [Check] Load user settings: allowDmFrom
        │      └── Sender not allowed → SKIP (log as SKIPPED)
        │
        ├─── [Check] Per-conversation settings: muteUntil / notifyOn
        │      └── Muted or NONE → SKIP
        │      └── MENTIONS_ONLY → only if message mentions @user
        │
        ├─── [Batch check] Sliding 5s window (Redis LPUSH/EXPIRE):
        │      └── > 1 message in window → accumulate, emit batch push
        │
        ├─── [Build payload]:
        │      messagePreview=true  → "{sender}: {first 80 chars of content}"
        │      messagePreview=false → "New message from {sender}"
        │      GROUP_CHAT           → "{groupName}: {sender} sent a message"
        │
        └─── [Dispatch] per registered device token:
               FCM / APNs / Web Push → async parallel
               └── Log result: SENT / FAILED / SKIPPED → Cassandra notification_delivery_log
```

## Batching Algorithm

```
Redis key: msg:notif:batch:{userId}:{convId}
  → LPUSH with message preview snippet
  → EXPIRE 5 seconds

On 5s window close (scheduled via Reactor interval):
  → Pop all items from list
  → If count == 1: send normal push
  → If count > 1: send "N new messages from {senderOrGroup}"
```

## Token Lifecycle

```
Register:   POST /api/v1/message-notifications/devices
              → Upsert device_push_tokens (user_id, device_id, platform, token)

Refresh:    Called by app on FCM token rotation
              → Update token in Cassandra

Logout/Deregister:
              → Mark is_active=false

Stale token: FCM/APNs returns "NotRegistered" or "InvalidRegistration"
              → Automatically mark token is_active=false
              → Publish event for audit log
```

## Delivery Log Schema

```cql
-- TTL 30 days, append-only, no soft delete required
CREATE TABLE message_notification_log (
  user_id UUID,
  notification_id TIMEUUID,
  conversation_id UUID,
  message_id UUID,
  channel TEXT,     -- WEBSOCKET, FCM, APNS, WEB_PUSH
  status TEXT,      -- PENDING, SENT, DELIVERED, FAILED, SKIPPED
  sent_at TIMESTAMP,
  failure_reason TEXT,
  retry_count INT DEFAULT 0,
  PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 2592000;
```

## Starters Used

`starter-cassandra`, `starter-redis`, `starter-kafka`, `starter-metrics`, `starter-security`

## Kafka Topics Consumed

| Topic | Action |
|-------|--------|
| `message.sent` | Main trigger — evaluate and dispatch push |
| `message.reaction.added` | Notify message owner of new reaction (if applicable settings) |
| `conversation.created` | Notify invited participants of new conversation |
| `conversation.settings.updated` | Invalidate settings cache for affected userId+convId |

## Kafka Topics Published

| Topic | Trigger |
|-------|---------|
| `message.notification.delivered` | Successful push delivery confirmed |
| `message.notification.failed` | Push delivery exhausted retries |

## Full API Surface

```
-- Device token management
POST   /api/v1/message-notifications/devices
         Body: {deviceId, platform: FCM_ANDROID|APNS_IOS|WEB_PUSH, token, appVersion}
PUT    /api/v1/message-notifications/devices/{deviceId}
         Body: {token}   # token refresh
DELETE /api/v1/message-notifications/devices/{deviceId}
GET    /api/v1/message-notifications/devices

-- Delivery log
GET    /api/v1/message-notifications/log?page=&size=&convId=
         # Last 30 days, paginated by notification_id cursor

-- Admin / ops
POST   /api/v1/message-notifications/test
         Body: {userId, message: "Test notification"}  # dev/staging only
GET    /api/v1/message-notifications/stats
         # Admin: delivery rates, failure rates, per-channel breakdown
         # Requires role: ADMIN
```

## Configuration

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

## Cache Keys

```
msg:notif:settings:{userId}                TTL 5 min    # user-level message settings
msg:notif:conv-settings:{userId}:{convId}  TTL 5 min    # per-conv notification settings
msg:notif:token:{userId}                   TTL 24 h     # serialized list of device tokens
msg:notif:batch:{userId}:{convId}          TTL 5 s      # batching sliding window
msg:notif:ws-online:{userId}               TTL 30 s     # is user connected via WebSocket?
```

## Tests

- **Unit:** NotificationDecisionEngine, BatchingService, FcmDispatcher, ApnsDispatcher, WebPushDispatcher (all external HTTP mocked)
- **Integration:** Testcontainers (Cassandra, Redis, Kafka); mock FCM/APNs server via WireMock
- **Automation:** Full Kafka consumer → dispatch → log cycle with WireMock push server

---

## Cross-Service Interaction Map (New Services)

```
┌──────────────┐    POST /groups/{id}/posts     ┌──────────────┐
│ group-service│ ──────────────────────────────► │ post-service │
│   :8087      │                                 │   :8083      │
│              │ ◄── Kafka: post.created ──────── │              │
└──────────────┘    (group-post association)     └──────────────┘
       │
       │ PUT /groups/{id}/avatar
       ▼
┌──────────────┐
│media-service │
│   :8082      │
└──────────────┘

┌──────────────────────────┐   Kafka: message.sent    ┌──────────────────────────────┐
│  private-message-service │ ────────────────────────► │ message-notification-service │
│         :8088            │                           │           :8089              │
│                          │ ◄─── Kafka: notification  │                              │
│  WS: online delivery ────┤       .delivered          │  FCM / APNs / Web Push ──────┤
│  Redis Pub/Sub (cross-pod)│                           │  Batching + Dedup ───────────┤
└──────────────────────────┘                           │  Token lifecycle ────────────┘
       │                                                        │
       │ POST media                                             │ GET user/conv settings
       ▼                                                        ▼
┌──────────────┐                                    ┌──────────────────────────┐
│media-service │                                    │  private-message-service │
│   :8082      │                                    │  (settings cache hit)    │
└──────────────┘                                    └──────────────────────────┘

group-service → Kafka: group.created  → search-service (index groups)
group-service → Kafka: group.member.joined → notification-service
group-service → Kafka: group.deleted → private-message-svc (archive GROUP_CHANNEL conv)
```
