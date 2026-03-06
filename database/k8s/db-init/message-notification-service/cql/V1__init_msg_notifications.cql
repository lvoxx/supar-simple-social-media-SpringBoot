CREATE KEYSPACE IF NOT EXISTS x_social_msg_notifications
    WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1}
    AND durable_writes = true;

USE x_social_msg_notifications;

CREATE TABLE IF NOT EXISTS device_push_tokens (
    user_id       UUID,
    device_id     UUID,
    platform      TEXT,
    token         TEXT,
    app_version   TEXT,
    registered_at TIMESTAMP,
    last_active   TIMESTAMP,
    is_active     BOOLEAN,
    PRIMARY KEY (user_id, device_id)
);

CREATE TABLE IF NOT EXISTS message_notification_log (
    user_id          UUID,
    notification_id  TIMEUUID,
    conversation_id  UUID,
    message_id       UUID,
    channel          TEXT,
    status           TEXT,
    sent_at          TIMESTAMP,
    failure_reason   TEXT,
    retry_count      INT,
    PRIMARY KEY (user_id, notification_id)
) WITH CLUSTERING ORDER BY (notification_id DESC)
  AND default_time_to_live = 2592000;
