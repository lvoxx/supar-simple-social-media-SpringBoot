-- ============================================================================
-- Social Media Platform - User Service Database Schema
-- Database: PostgreSQL 16+
-- Description: Complete database schema for user profile management
-- ============================================================================

-- Drop existing tables (for clean setup)
DROP TABLE IF EXISTS muted_users CASCADE;
DROP TABLE IF EXISTS blocked_users CASCADE;
DROP TABLE IF EXISTS user_interests CASCADE;
DROP TABLE IF EXISTS user_preferences CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- USERS TABLE
-- Core user profile information
-- ============================================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    bio TEXT,
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    birth_date DATE,
    location VARCHAR(100),
    website VARCHAR(200),
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    is_private BOOLEAN DEFAULT FALSE NOT NULL,
    follower_count BIGINT DEFAULT 0 NOT NULL,
    following_count BIGINT DEFAULT 0 NOT NULL,
    post_count BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_username_format CHECK (username ~ '^[a-zA-Z0-9_]+$'),
    CONSTRAINT chk_email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT chk_follower_count CHECK (follower_count >= 0),
    CONSTRAINT chk_following_count CHECK (following_count >= 0),
    CONSTRAINT chk_post_count CHECK (post_count >= 0)
);

-- ============================================================================
-- USER PREFERENCES TABLE
-- User settings, privacy, and notification preferences
-- ============================================================================
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    -- Privacy Settings
    show_email BOOLEAN DEFAULT FALSE NOT NULL,
    show_birth_date BOOLEAN DEFAULT FALSE NOT NULL,
    allow_tagging BOOLEAN DEFAULT TRUE NOT NULL,
    allow_mentions BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Notification Settings
    notify_new_follower BOOLEAN DEFAULT TRUE NOT NULL,
    notify_post_like BOOLEAN DEFAULT TRUE NOT NULL,
    notify_comment BOOLEAN DEFAULT TRUE NOT NULL,
    notify_mention BOOLEAN DEFAULT TRUE NOT NULL,
    notify_message BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Content Settings
    default_post_visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL,
    language VARCHAR(10) DEFAULT 'en' NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC' NOT NULL,
    theme VARCHAR(20) DEFAULT 'LIGHT' NOT NULL,
    avatar_url VARCHAR(500) DEFAULT 'https://res.cloudinary.com/demo/image/upload/sample.jpg',
    cover_image_url VARCHAR(500) DEFAULT 'https://res.cloudinary.com/demo/image/upload/landscape.jpg',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_post_visibility CHECK (default_post_visibility IN ('PUBLIC', 'FOLLOWERS', 'PRIVATE')),
    CONSTRAINT chk_theme CHECK (theme IN ('LIGHT', 'DARK', 'AUTO'))
);

-- ============================================================================
-- USER INTERESTS TABLE
-- User interests for AI-powered recommendations
-- ============================================================================
CREATE TABLE user_interests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_category VARCHAR(50) NOT NULL,
    interest_name VARCHAR(100) NOT NULL,
    weight DOUBLE PRECISION DEFAULT 1.0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_weight CHECK (weight >= 0 AND weight <= 10),
    CONSTRAINT uk_user_interest UNIQUE(user_id, interest_category, interest_name)
);

-- ============================================================================
-- BLOCKED USERS TABLE
-- User blocking relationships
-- ============================================================================
CREATE TABLE blocked_users (
    id BIGSERIAL PRIMARY KEY,
    blocker_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_not_self_block CHECK (blocker_user_id != blocked_user_id),
    CONSTRAINT uk_block_relationship UNIQUE(blocker_user_id, blocked_user_id)
);

-- ============================================================================
-- MUTED USERS TABLE
-- User muting relationships
-- ============================================================================
CREATE TABLE muted_users (
    id BIGSERIAL PRIMARY KEY,
    muter_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muted_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT chk_not_self_mute CHECK (muter_user_id != muted_user_id),
    CONSTRAINT uk_mute_relationship UNIQUE(muter_user_id, muted_user_id)
);

-- ============================================================================
-- INDEXES for Performance Optimization
-- ============================================================================

-- Users table indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak_id ON users(keycloak_user_id);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_verified ON users(is_verified) WHERE is_verified = TRUE;

-- Search optimization (case-insensitive)
CREATE INDEX idx_users_username_lower ON users(LOWER(username));
CREATE INDEX idx_users_display_name_lower ON users(LOWER(display_name));

-- Composite index for search
CREATE INDEX idx_users_search ON users(LOWER(username), LOWER(display_name));

-- User preferences indexes
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);

-- User interests indexes
CREATE INDEX idx_user_interests_user_id ON user_interests(user_id);
CREATE INDEX idx_user_interests_category ON user_interests(interest_category);
CREATE INDEX idx_user_interests_user_category ON user_interests(user_id, interest_category);

-- Blocked users indexes
CREATE INDEX idx_blocked_users_blocker ON blocked_users(blocker_user_id);
CREATE INDEX idx_blocked_users_blocked ON blocked_users(blocked_user_id);
CREATE INDEX idx_blocked_users_relationship ON blocked_users(blocker_user_id, blocked_user_id);

-- Muted users indexes
CREATE INDEX idx_muted_users_muter ON muted_users(muter_user_id);
CREATE INDEX idx_muted_users_muted ON muted_users(muted_user_id);
CREATE INDEX idx_muted_users_relationship ON muted_users(muter_user_id, muted_user_id);

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

-- Trigger for users table
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for user_preferences table
CREATE TRIGGER update_user_preferences_updated_at 
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- STORED PROCEDURES / FUNCTIONS
-- ============================================================================

-- Function to increment follower count
CREATE OR REPLACE FUNCTION increment_follower_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET follower_count = follower_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement follower count
CREATE OR REPLACE FUNCTION decrement_follower_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET follower_count = GREATEST(follower_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment following count
CREATE OR REPLACE FUNCTION increment_following_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET following_count = following_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement following count
CREATE OR REPLACE FUNCTION decrement_following_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET following_count = GREATEST(following_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to increment post count
CREATE OR REPLACE FUNCTION increment_post_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET post_count = post_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to decrement post count
CREATE OR REPLACE FUNCTION decrement_post_count(p_user_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE users 
    SET post_count = GREATEST(post_count - 1, 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to check if user is blocked
CREATE OR REPLACE FUNCTION is_user_blocked(p_blocker_id BIGINT, p_blocked_id BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(
        SELECT 1 FROM blocked_users 
        WHERE blocker_user_id = p_blocker_id 
        AND blocked_user_id = p_blocked_id
    );
END;
$$ LANGUAGE plpgsql;

-- Function to check if user is muted
CREATE OR REPLACE FUNCTION is_user_muted(p_muter_id BIGINT, p_muted_id BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS(
        SELECT 1 FROM muted_users 
        WHERE muter_user_id = p_muter_id 
        AND muted_user_id = p_muted_id
    );
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- VIEWS for Common Queries
-- ============================================================================

-- View: User with preferences (for optimization)
CREATE OR REPLACE VIEW v_user_full_profile AS
SELECT 
    u.*,
    up.show_email,
    up.show_birth_date,
    up.allow_tagging,
    up.allow_mentions,
    up.default_post_visibility,
    up.language,
    up.timezone,
    up.theme
FROM users u
LEFT JOIN user_preferences up ON u.id = up.user_id;

-- View: Public user profiles (without sensitive data)
CREATE OR REPLACE VIEW v_user_public_profile AS
SELECT 
    id,
    username,
    display_name,
    bio,
    avatar_url,
    cover_image_url,
    location,
    website,
    is_verified,
    is_private,
    follower_count,
    following_count,
    post_count,
    created_at
FROM users;

-- ============================================================================
-- SAMPLE DATA (for testing)
-- ============================================================================

-- Insert sample users
INSERT INTO users (keycloak_user_id, username, email, display_name, bio, is_verified) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'johndoe', 'john@example.com', 'John Doe', 'Software Engineer | Coffee Lover', true),
('550e8400-e29b-41d4-a716-446655440002', 'janedoe', 'jane@example.com', 'Jane Doe', 'Designer | Travel Enthusiast', false),
('550e8400-e29b-41d4-a716-446655440003', 'bobsmith', 'bob@example.com', 'Bob Smith', 'Entrepreneur | Tech Blogger', true);

-- Insert sample preferences
INSERT INTO user_preferences (user_id) 
SELECT id FROM users;

-- Insert sample interests
INSERT INTO user_interests (user_id, interest_category, interest_name, weight) VALUES
((SELECT id FROM users WHERE username = 'johndoe'), 'TECHNOLOGY', 'Artificial Intelligence', 2.5),
((SELECT id FROM users WHERE username = 'johndoe'), 'TECHNOLOGY', 'Cloud Computing', 1.8),
((SELECT id FROM users WHERE username = 'janedoe'), 'ART', 'Digital Design', 3.0),
((SELECT id FROM users WHERE username = 'janedoe'), 'TRAVEL', 'Photography', 2.2);

-- ============================================================================
-- GRANTS (adjust based on your user setup)
-- ============================================================================

-- Grant permissions to application user (replace 'userservice_app' with your username)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO userservice_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO userservice_app;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO userservice_app;

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE users IS 'Core user profile information';
COMMENT ON TABLE user_preferences IS 'User settings, privacy and notification preferences';
COMMENT ON TABLE user_interests IS 'User interests for AI-powered recommendations';
COMMENT ON TABLE blocked_users IS 'User blocking relationships';
COMMENT ON TABLE muted_users IS 'User muting relationships';

COMMENT ON COLUMN users.keycloak_user_id IS 'UUID from Keycloak authentication system';
COMMENT ON COLUMN users.username IS 'Unique username (alphanumeric and underscore only)';
COMMENT ON COLUMN users.is_verified IS 'Whether user has verified badge';
COMMENT ON COLUMN users.is_private IS 'Whether user profile is private';
COMMENT ON COLUMN user_interests.weight IS 'Interest weight for recommendation algorithm (0-10)';

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================