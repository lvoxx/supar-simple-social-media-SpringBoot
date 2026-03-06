CREATE TABLE IF NOT EXISTS posts (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id        UUID        NOT NULL,
    group_id         UUID,
    content          TEXT,
    reply_to_id      UUID,
    repost_of_id     UUID,
    post_type        VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
    status           VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    visibility       VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    like_count       INT         NOT NULL DEFAULT 0,
    repost_count     INT         NOT NULL DEFAULT 0,
    reply_count      INT         NOT NULL DEFAULT 0,
    bookmark_count   INT         NOT NULL DEFAULT 0,
    view_count       BIGINT      NOT NULL DEFAULT 0,
    is_edited        BOOLEAN     NOT NULL DEFAULT FALSE,
    edited_at        TIMESTAMPTZ,
    is_pinned        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    created_by       UUID,
    updated_by       UUID,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    deleted_by       UUID
);
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts(author_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_posts_group_id  ON posts(group_id)  WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_posts_reply_to  ON posts(reply_to_id);

CREATE TABLE IF NOT EXISTS post_likes (
    post_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS post_bookmarks (
    post_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS post_media (
    post_id    UUID        NOT NULL,
    media_id   UUID        NOT NULL,
    position   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, media_id)
);

CREATE TABLE IF NOT EXISTS post_hashtags (
    post_id    UUID         NOT NULL,
    hashtag    VARCHAR(100) NOT NULL,
    PRIMARY KEY (post_id, hashtag)
);

CREATE TABLE IF NOT EXISTS post_mentions (
    post_id           UUID NOT NULL,
    mentioned_user_id UUID NOT NULL,
    PRIMARY KEY (post_id, mentioned_user_id)
);

CREATE TABLE IF NOT EXISTS post_reports (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID        NOT NULL,
    reporter_id UUID        NOT NULL,
    reason      VARCHAR(50),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
