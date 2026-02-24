-- V1__create_media_tables.sql
-- This migration should be run by a separate migration module/service
-- NOT by this media-service application

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Media Types
CREATE TYPE media_type AS ENUM ('IMAGE', 'VIDEO', 'ATTACHMENT', 'CHAT_MEDIA', 'AVATAR', 'COVER');

-- Visibility Types
CREATE TYPE visibility_type AS ENUM ('PUBLIC', 'FRIENDS', 'PRIVATE', 'GROUP', 'MESSAGE');

-- Media Status
CREATE TYPE media_status AS ENUM ('UPLOADING', 'READY', 'FAILED');

-- Variant Types
CREATE TYPE variant_type AS ENUM ('THUMB', 'SMALL', 'MEDIUM', 'LARGE', 'STREAM');

-- Relation Types
CREATE TYPE relation_type AS ENUM ('OWNER', 'FRIEND', 'GROUP_MEMBER', 'MESSAGE_MEMBER');

-- Main media object table
CREATE TABLE media_object (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id VARCHAR(255) NOT NULL,
    media_type media_type NOT NULL,
    visibility visibility_type NOT NULL,
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    size_bytes BIGINT NOT NULL,
    format VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    status media_status NOT NULL DEFAULT 'UPLOADING'
);

-- Indexes for media_object
CREATE INDEX idx_media_object_owner_id ON media_object(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_object_created_at ON media_object(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_object_status ON media_object(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_object_visibility ON media_object(visibility) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_object_media_type ON media_object(media_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_media_object_owner_created ON media_object(owner_id, created_at DESC) WHERE deleted_at IS NULL;

-- Media variants table (optimized CDN URLs)
CREATE TABLE media_variant (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    media_id UUID NOT NULL REFERENCES media_object(id) ON DELETE CASCADE,
    variant variant_type NOT NULL,
    cdn_url TEXT NOT NULL,
    bytes BIGINT NOT NULL,
    UNIQUE(media_id, variant)
);

-- Indexes for media_variant
CREATE INDEX idx_media_variant_media_id ON media_variant(media_id);

-- Original media storage (never exposed publicly)
CREATE TABLE media_original (
    media_id UUID PRIMARY KEY REFERENCES media_object(id) ON DELETE CASCADE,
    cloudinary_public_id VARCHAR(500) NOT NULL UNIQUE,
    secure_url TEXT NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    signature VARCHAR(500) NOT NULL
);

-- Media Access Control List
CREATE TABLE media_acl (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    media_id UUID NOT NULL REFERENCES media_object(id) ON DELETE CASCADE,
    allowed_user_id VARCHAR(255) NOT NULL,
    relation_type relation_type NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(media_id, allowed_user_id, relation_type)
);

-- Indexes for media_acl - optimized for authorization checks
CREATE INDEX idx_media_acl_media_id ON media_acl(media_id);
CREATE INDEX idx_media_acl_user_id ON media_acl(allowed_user_id);
CREATE INDEX idx_media_acl_media_user ON media_acl(media_id, allowed_user_id);
CREATE INDEX idx_media_acl_relation_type ON media_acl(relation_type);