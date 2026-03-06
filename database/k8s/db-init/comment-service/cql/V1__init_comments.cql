CREATE KEYSPACE IF NOT EXISTS x_social_comments
    WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1}
    AND durable_writes = true;

USE x_social_comments;

CREATE TABLE IF NOT EXISTS comments_by_post (
    post_id           UUID,
    comment_id        TIMEUUID,
    parent_comment_id UUID,
    author_id         UUID,
    author_username   TEXT,
    content           TEXT,
    depth             INT,
    status            TEXT,
    is_deleted        BOOLEAN,
    deleted_at        TIMESTAMP,
    deleted_by        UUID,
    media_ids         LIST<UUID>,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    PRIMARY KEY (post_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

CREATE TABLE IF NOT EXISTS comments_by_parent (
    parent_comment_id UUID,
    comment_id        TIMEUUID,
    post_id           UUID,
    author_id         UUID,
    content           TEXT,
    is_deleted        BOOLEAN,
    created_at        TIMESTAMP,
    PRIMARY KEY (parent_comment_id, comment_id)
) WITH CLUSTERING ORDER BY (comment_id DESC);

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id UUID,
    user_id    UUID,
    post_id    UUID,
    liked_at   TIMESTAMP,
    PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS comment_counters (
    comment_id  UUID PRIMARY KEY,
    like_count  COUNTER,
    reply_count COUNTER
);
