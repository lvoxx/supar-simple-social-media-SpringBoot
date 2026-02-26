# group-service

**Type:** Spring Boot  
**Port:** `8087`  
**Module path:** `spring-services/services/group-service`  
**Database:** PostgreSQL (R2DBC)  
**Cache:** Redis (Redisson)  
**Messaging:** Apache Kafka  

---

## Trách nhiệm

Quản lý groups xã hội: tạo nhóm, vòng đời membership, hệ thống phân cấp vai trò (Owner → Admin → Moderator → Member), chính sách tham gia, câu hỏi sàng lọc chống bot, nội dung ghim, tags, và cài đặt nhóm. **Posts trong group được tạo thông qua post-service** — group-service chỉ quản lý context và association.

---

## Loại Group

| Visibility | Tìm kiếm được? | Tham gia | Nội dung |
|-----------|--------------|----------|----------|
| `PUBLIC` | ✅ | Tức thì (hoặc chờ duyệt nếu `autoApprove=false`) | Hiển thị cho mọi người |
| `PRIVATE` | ✅ | Phải gửi request + trả lời câu hỏi | Ẩn với non-member |
| `INVITE_ONLY` | ❌ | Chỉ qua invitation | Ẩn hoàn toàn |

---

## Ma trận quyền hạn

| Thao tác | MEMBER | MODERATOR | ADMIN | OWNER |
|----------|:------:|:---------:|:-----:|:-----:|
| Xem group & member | ✅ | ✅ | ✅ | ✅ |
| Đăng post trong group | ✅* | ✅ | ✅ | ✅ |
| Ghim / bỏ ghim post | ❌ | ✅ | ✅ | ✅ |
| Duyệt join requests | ❌ | ✅ | ✅ | ✅ |
| Mute / remove member | ❌ | ✅ | ✅ | ✅ |
| Ban member | ❌ | ✅ | ✅ | ✅ |
| Cập nhật rules / policy | ❌ | ❌ | ✅ | ✅ |
| Thay đổi role (≤ MODERATOR) | ❌ | ❌ | ✅ | ✅ |
| Xóa group | ❌ | ❌ | ❌ | ✅ |
| Chuyển giao ownership | ❌ | ❌ | ❌ | ✅ |

\* Chỉ khi `policy.allowMemberPost = true`

---

## Luồng tham gia group

```
PUBLIC group (autoApproveMembers = true):
  POST /join → tạo group_member status=ACTIVE → publish group.member.joined

PUBLIC group (autoApproveMembers = false):
  POST /join → tạo join_request status=PENDING → MODERATOR duyệt → ACTIVE

PRIVATE group (requireJoinAnswers = false):
  POST /join + body → tạo join_request → MODERATOR duyệt → ACTIVE

PRIVATE group (requireJoinAnswers = true):
  POST /join + {answers: [...]} → validate đủ câu trả lời → join_request → duyệt

INVITE_ONLY:
  Invitation gửi bởi member có quyền → invitee nhận, accept → ACTIVE
```

---

## Chính sách Join (policy JSON)

```json
{
  "autoApproveMembers": true,
  "allowMemberPost": true,
  "allowMemberInvite": false,
  "requireJoinAnswers": false,
  "minAccountAgeDays": 30,
  "allowedRegions": [],
  "postApproval": false,
  "maxMembers": 10000
}
```

---

## Tích hợp với post-service

```
User tạo post trong group:
  1. Client gọi POST /api/v1/groups/{groupId}/posts
  2. group-service kiểm tra: user có quyền đăng không?
  3. group-service forward request tới post-service với header X-Group-Id
  4. post-service tạo post với groupId field
  5. post-service publish Kafka: post.created (với groupId)
  6. group-service consume post.created → tạo group_post_associations
  7. Nếu policy.postApproval = true → status = PENDING_APPROVAL
  8. Publish Kafka: group.post.created → search-service index
```

---

## Starters sử dụng

`starter-postgres` · `starter-redis` · `starter-kafka` · `starter-metrics` · `starter-security`

---

## Kafka Events

### Published

| Topic | Trigger | Consumers |
|-------|---------|-----------|
| `group.created` | Group được tạo | search-svc, user-analysis-svc |
| `group.updated` | Name/description/tags thay đổi | search-svc |
| `group.deleted` | Group bị xóa mềm | search-svc, private-message-svc |
| `group.member.joined` | Member được approve / join | notification-svc, user-analysis-svc |
| `group.member.left` | Member rời nhóm / bị remove | notification-svc, private-message-svc |
| `group.member.role.changed` | Role được thay đổi | notification-svc |
| `group.member.banned` | Member bị ban | notification-svc |
| `group.post.pinned` | Post được ghim | notification-svc |
| `group.post.created` | Post được đăng trong group | search-svc |

### Consumed

| Topic | Hành động |
|-------|-----------|
| `post.created` | Đăng ký group-post association nếu có groupId |
| `post.deleted` | Xóa khỏi pinned_posts nếu đang ghim |
| `user.profile.updated` | Invalidate member display cache |

---

## Database Schema (Flyway)

```
V1__init_groups.sql
V2__add_join_questions.sql
V3__add_invitations.sql
V4__add_activity_log.sql
V5__add_post_associations.sql
```

```sql
-- groups
id                UUID        PRIMARY KEY DEFAULT gen_random_uuid()
slug              VARCHAR(100) UNIQUE NOT NULL
name              VARCHAR(150) NOT NULL
description       TEXT
avatar_url        TEXT
background_url    TEXT
visibility        VARCHAR(20) DEFAULT 'PUBLIC'
status            VARCHAR(20) DEFAULT 'ACTIVE'
owner_id          UUID        NOT NULL
member_count      INT         DEFAULT 0
post_count        INT         DEFAULT 0
tags              TEXT[]
category          VARCHAR(60)
rules             JSONB       -- [{title, description, order}]
policy            JSONB       -- join policy config
settings          JSONB       -- {theme, language, ...}
is_deleted        BOOLEAN     DEFAULT FALSE
deleted_at        TIMESTAMPTZ
deleted_by        UUID
created_at        TIMESTAMPTZ DEFAULT NOW()
updated_at        TIMESTAMPTZ

-- group_members
group_id          UUID REFERENCES groups(id)
user_id           UUID NOT NULL
role              VARCHAR(20) DEFAULT 'MEMBER'
status            VARCHAR(20) DEFAULT 'ACTIVE'    -- ACTIVE|BANNED|MUTED|PENDING_APPROVAL
join_answers      JSONB
invited_by        UUID
banned_by         UUID
ban_reason        TEXT
banned_at         TIMESTAMPTZ
joined_at         TIMESTAMPTZ DEFAULT NOW()
updated_at        TIMESTAMPTZ
PRIMARY KEY (group_id, user_id)

-- group_join_questions
id                UUID PRIMARY KEY
group_id          UUID REFERENCES groups(id)
question          TEXT NOT NULL
order_index       INT
is_required       BOOLEAN DEFAULT TRUE
created_at        TIMESTAMPTZ

-- group_join_requests
id                UUID PRIMARY KEY
group_id          UUID REFERENCES groups(id)
requester_id      UUID NOT NULL
answers           JSONB
status            VARCHAR(20) DEFAULT 'PENDING'
reviewed_by       UUID
reviewed_at       TIMESTAMPTZ
reject_reason     TEXT
created_at        TIMESTAMPTZ

-- group_invitations
id                UUID PRIMARY KEY
group_id          UUID REFERENCES groups(id)
inviter_id        UUID NOT NULL
invitee_id        UUID NOT NULL
status            VARCHAR(20) DEFAULT 'PENDING'
expires_at        TIMESTAMPTZ
created_at        TIMESTAMPTZ

-- group_pinned_posts   (max 5 per group)
group_id          UUID REFERENCES groups(id)
post_id           UUID NOT NULL
pinned_by         UUID NOT NULL
pinned_at         TIMESTAMPTZ DEFAULT NOW()
order_index       INT DEFAULT 0
PRIMARY KEY (group_id, post_id)

-- group_post_associations
group_id          UUID REFERENCES groups(id)
post_id           UUID NOT NULL
posted_by         UUID NOT NULL
status            VARCHAR(20) DEFAULT 'ACTIVE'
is_pinned         BOOLEAN DEFAULT FALSE
created_at        TIMESTAMPTZ
PRIMARY KEY (group_id, post_id)

-- group_member_activity   (audit log)
id                UUID PRIMARY KEY
group_id          UUID REFERENCES groups(id)
actor_id          UUID NOT NULL
target_id         UUID
action            VARCHAR(50)  -- BAN|UNBAN|MUTE|PROMOTE|DEMOTE|APPROVE|REJECT|REMOVE
detail            JSONB
created_at        TIMESTAMPTZ
```

---

## API Endpoints

```
# Group CRUD
POST   /api/v1/groups
GET    /api/v1/groups/{groupId}
PUT    /api/v1/groups/{groupId}
DELETE /api/v1/groups/{groupId}
PUT    /api/v1/groups/{groupId}/avatar
PUT    /api/v1/groups/{groupId}/background
PUT    /api/v1/groups/{groupId}/rules
PUT    /api/v1/groups/{groupId}/policy
PUT    /api/v1/groups/{groupId}/transfer-ownership    # {newOwnerId}

# Membership
GET    /api/v1/groups/{groupId}/members
GET    /api/v1/groups/{groupId}/members/{userId}
PUT    /api/v1/groups/{groupId}/members/{userId}/role
DELETE /api/v1/groups/{groupId}/members/{userId}
POST   /api/v1/groups/{groupId}/members/{userId}/ban
DELETE /api/v1/groups/{groupId}/members/{userId}/ban
POST   /api/v1/groups/{groupId}/members/{userId}/mute

# Join / Leave
POST   /api/v1/groups/{groupId}/join
POST   /api/v1/groups/{groupId}/leave

# Join requests
GET    /api/v1/groups/{groupId}/join-requests
PUT    /api/v1/groups/{groupId}/join-requests/{reqId}    # {action: APPROVE|REJECT}

# Invitations
POST   /api/v1/groups/{groupId}/invite
GET    /api/v1/groups/{groupId}/invitations
POST   /api/v1/groups/{groupId}/invitations/{invId}/respond
DELETE /api/v1/groups/{groupId}/invitations/{invId}

# Posts
GET    /api/v1/groups/{groupId}/posts
POST   /api/v1/groups/{groupId}/posts
GET    /api/v1/groups/{groupId}/posts/pinned
POST   /api/v1/groups/{groupId}/posts/{postId}/pin
DELETE /api/v1/groups/{groupId}/posts/{postId}/pin

# Join questions
GET    /api/v1/groups/{groupId}/join-questions
PUT    /api/v1/groups/{groupId}/join-questions

# Discovery
GET    /api/v1/groups/me/joined
GET    /api/v1/groups/me/owned
GET    /api/v1/groups/search?q=&tags=&category=

# Admin audit
GET    /api/v1/groups/{groupId}/activity-log
```

---

## Cache Keys

| Key | TTL | Mô tả |
|-----|-----|-------|
| `group:detail:{groupId}` | 5 phút | Chi tiết group |
| `group:members:count:{groupId}` | 1 phút | Số lượng member |
| `group:pinned:{groupId}` | 2 phút | Danh sách pinned posts |
| `group:membership:{userId}:{groupId}` | 5 phút | `{isMember, role, status}` |
| `group:policy:{groupId}` | 10 phút | Join policy |

---

## Rate Limits

| Endpoint | Giới hạn |
|----------|---------|
| `POST /groups` | 3 req/ngày per userId |
| `POST /join` | 5 req/giờ per userId |
| `POST /invite` | 20 req/ngày per userId |

---

## Tests

### Unit Tests
- `GroupServiceTest` — mock repositories, policy evaluator
- `MembershipServiceTest` — join flow, ban/unban logic
- `JoinRequestServiceTest` — question validation, approval flow

### Integration Tests (Testcontainers)
- `GroupRepositoryIT` — PostgreSQL container
- `GroupMembershipIT` — PostgreSQL + Redis + Kafka
- `GroupJoinFlowIT` — full join request approval cycle

### Automation Tests
- `GroupApiAutomationTest` — create → join (private) → approve → pin post → transfer ownership
- `GroupBanAutomationTest` — ban member → verify removed from group → unban
