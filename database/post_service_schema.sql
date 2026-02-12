-- ============================================================================
-- Social Media Platform - Post Service Database Schema
-- Database: PostgreSQL 16+
-- Description: Complete database schema for post management
-- ============================================================================

-- Drop existing tables (for clean setup)
DROP TABLE IF EXISTS post_shares CASCADE;
DROP TABLE IF EXISTS post_bookmarks CASCADE;
DROP TABLE IF EXISTS post_media CASCADE;
DROP TABLE IF EXISTS posts CASCADE;

-- ============================================================================
-- POSTS TABLE
-- Core post information with advanced features
-- ============================================================================
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL,
    
    -- Comment settings
    comments_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    allowed_commenters VARCHAR(20) DEFAULT 'EVERYONE' NOT NULL,
    
    -- Interaction settings
    sharing_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Status
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Counters (denormalized for performance)
    like_count BIGINT DEFAULT 0 NOT NULL,
    comment_count BIGINT DEFAULT 0 NOT NULL,
    share_count BIGINT DEFAULT 0 NOT NULL,
    bookmark_count BIGINT DEFAULT 0 NOT NULL,
    view_count BIGINT DEFAULT 0 NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    archived_at TIMESTAMP,
    
    CONSTRAINT chk_visibility CHECK (visibility IN ('PUBLIC', 'FOLLOWERS', 'PRIVATE', 'CUSTOM')),
    CONSTRAINT chk_allowed_commenters CHECK (allowed_commenters IN ('EVERYONE', 'FOLLOWERS', 'MENTIONED', 'NOBODY')),
    CONSTRAINT chk_like_count CHECK (like_count >= 0),
    CONSTRAINT chk_comment_count CHECK (comment_count >= 0),
    CONSTRAINT chk_share_count CHECK (share_count >= 0),
    CONSTRAINT chk_bookmark_count CHECK (bookmark_count >= 0),
    CONSTRAINT chk_view_count CHECK (view_count >= 0),
    CONSTRAINT chk_content_length CHECK (LENGTH(content) <= 5000)
);

-- ============================================================================
-- POST MEDIA TABLE
-- Media attachments for posts (images/videos)
-- ============================================================================
CREATE TABLE post_media (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_url VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    display_order INT DEFAULT 0 NOT NULL,
    width INT,
    height INT,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
    CONSTRAINT chk_display_order CHECK (display_order >= 0),
    CONSTRAINT uk_post_media_order UNIQUE(post_id, display_order)
);

-- ============================================================================
-- POST SHARES TABLE
-- Track post shares by users
-- ============================================================================
CREATE TABLE post_shares (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    shared_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_post_share UNIQUE(post_id, user_id)
);

-- ============================================================================
-- POST BOOKMARKS TABLE
-- User bookmarks for posts
-- ============================================================================
CREATE TABLE post_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    collection_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_post_bookmark UNIQUE(post_id, user_id)
);

-- ============================================================================
-- INDEXES for Performance Optimization
-- ============================================================================

-- Posts table indexes
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_visibility ON posts(visibility);
CREATE INDEX idx_posts_deleted ON posts(is_deleted) WHERE is_deleted = FALSE;
CREATE INDEX idx_posts_archived ON posts(is_archived) WHERE is_archived = FALSE;
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC) WHERE is_deleted = FALSE AND is_archived = FALSE;

-- Post media indexes
CREATE INDEX idx_post_media_post_id ON post_media(post_id);
CREATE INDEX idx_post_media_order ON post_media(post_id, display_order);

-- Post shares indexes
CREATE INDEX idx_post_shares_post_id ON post_shares(post_id);
CREATE INDEX idx_post_shares_user_id ON post_shares(user_id);
CREATE INDEX idx_post_shares_created ON post_shares(created_at DESC);

-- Post bookmarks indexes
CREATE INDEX idx_post_bookmarks_post_id ON post_bookmarks(post_id);
CREATE INDEX idx_post_bookmarks_user_id ON post_bookmarks(user_id);
CREATE INDEX idx_post_bookmarks_collection ON post_bookmarks(user_id, collection_name);
CREATE INDEX idx_post_bookmarks_created ON post_bookmarks(created_at DESC);

-- ============================================================================
-- TRIGGERS for Auto-updating Timestamps
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for posts table
CREATE TRIGGER update_posts_updated_at 
    BEFORE UPDATE ON posts
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for comments table
CREATE TRIGGER update_comments_updated_at 
    BEFORE UPDATE ON comments
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- STORED PROCEDURES / FUNCTIONS
-- ============================================================================

-- Function to increment like count
CREATE OR REPLACE FUNCTION increment_post_like_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET like_count = like_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement like count
CREATE OR REPLACE FUNCTION decrement_post_like_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET like_count = GREATEST(like_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment comment count
CREATE OR REPLACE FUNCTION increment_post_comment_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET comment_count = comment_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement comment count
CREATE OR REPLACE FUNCTION decrement_post_comment_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET comment_count = GREATEST(comment_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment share count
CREATE OR REPLACE FUNCTION increment_post_share_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET share_count = share_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement share count
CREATE OR REPLACE FUNCTION decrement_post_share_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET share_count = GREATEST(share_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment bookmark count
CREATE OR REPLACE FUNCTION increment_post_bookmark_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET bookmark_count = bookmark_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement bookmark count
CREATE OR REPLACE FUNCTION decrement_post_bookmark_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET bookmark_count = GREATEST(bookmark_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment view count
CREATE OR REPLACE FUNCTION increment_post_view_count(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET view_count = view_count + 1
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Soft delete post
CREATE OR REPLACE FUNCTION soft_delete_post(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET is_deleted = TRUE,
        deleted_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Archive post
CREATE OR REPLACE FUNCTION archive_post(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET is_archived = TRUE,
        archived_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- Unarchive post
CREATE OR REPLACE FUNCTION unarchive_post(p_post_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE posts 
    SET is_archived = FALSE,
        archived_at = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE posts IS 'Core post information with advanced sharing and permission features';
COMMENT ON TABLE post_media IS 'Media attachments for posts (images/videos)';
COMMENT ON TABLE post_shares IS 'Track post shares by users';
COMMENT ON TABLE post_bookmarks IS 'User bookmarks for saving posts';

COMMENT ON COLUMN posts.visibility IS 'Post visibility: PUBLIC, FOLLOWERS, PRIVATE, or CUSTOM';
COMMENT ON COLUMN posts.allowed_commenters IS 'Who can comment: EVERYONE, FOLLOWERS, MENTIONED, or NOBODY';
COMMENT ON COLUMN posts.sharing_enabled IS 'Whether post can be shared';
COMMENT ON COLUMN posts.is_archived IS 'Whether post is archived by owner';
COMMENT ON COLUMN post_media.public_id IS 'Cloudinary public ID for media deletion';
COMMENT ON COLUMN post_bookmarks.collection_name IS 'Optional collection/folder name for organizing bookmarks';

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================