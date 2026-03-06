CREATE KEYSPACE IF NOT EXISTS x_social_messages
    WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1}
    AND durable_writes = true;

USE x_social_messages;

CREATE TABLE IF NOT EXISTS conversations (
    conversation_id UUID PRIMARY KEY,
    type            TEXT,
    name            TEXT,
    avatar_url      TEXT,
    group_id        UUID,
    created_by      UUID,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    settings        TEXT,
    is_deleted      BOOLEAN
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id       UUID,
    user_id               UUID,
    role                  TEXT,
    status                TEXT,
    joined_at             TIMESTAMP,
    last_read_message_id  UUID,
    last_read_at          TIMESTAMP,
    notification_settings TEXT,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS ON conversation_participants (user_id);

CREATE TABLE IF NOT EXISTS conversations_by_user (
    user_id           UUID,
    last_message_at   TIMESTAMP,
    conversation_id   UUID,
    conversation_type TEXT,
    unread_count      INT,
    is_muted          BOOLEAN,
    PRIMARY KEY (user_id, last_message_at, conversation_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, conversation_id ASC);

CREATE TABLE IF NOT EXISTS messages (
    conversation_id                UUID,
    message_id                     TIMEUUID,
    sender_id                      UUID,
    message_type                   TEXT,
    content                        TEXT,
    reply_to_message_id            UUID,
    forwarded_from_message_id      UUID,
    forwarded_from_conversation_id UUID,
    media_ids                      TEXT,
    status                         TEXT,
    is_deleted                     BOOLEAN,
    deleted_at                     TIMESTAMP,
    edited_at                      TIMESTAMP,
    created_at                     TIMESTAMP,
    PRIMARY KEY (conversation_id, message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

CREATE TABLE IF NOT EXISTS message_reactions (
    conversation_id UUID,
    message_id      UUID,
    user_id         UUID,
    emoji           TEXT,
    created_at      TIMESTAMP,
    PRIMARY KEY (conversation_id, message_id, user_id)
);
