-- Chat Rooms
CREATE INDEX idx_chat_rooms_sender_id ON chat_rooms (sender_id);
CREATE INDEX idx_chat_rooms_recipient_id ON chat_rooms (recipient_id);

-- Comments
CREATE INDEX idx_comments_user_id ON comments (user_id);
CREATE INDEX idx_comments_post_id ON comments (post_id);

-- Comment Reports
CREATE INDEX idx_comment_reports_comment_id ON comment_reports (comment_id);

-- Email Tokens
CREATE INDEX idx_email_tokens_user_id ON email_tokens (user_id);

-- Messages
CREATE INDEX idx_messages_chat_id ON messages (chat_id);

-- Notifications
CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);
CREATE INDEX idx_notifications_action_user_id ON notifications (action_user_id);

-- Posts
CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_posts_category_type_id ON posts (category_type_id);
CREATE INDEX idx_posts_route_id ON posts (route_id);

-- Places
CREATE INDEX idx_places_post_id ON places (post_id);

-- Post Hashtags
CREATE INDEX idx_post_hashtags_hashtag_id ON post_hashtags (hashtag_id);
CREATE INDEX idx_post_hashtags_post_id ON post_hashtags (post_id);

-- Post Reports
CREATE INDEX idx_post_reports_post_id ON post_reports (post_id);

-- Refresh Tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Reports
CREATE INDEX idx_reports_user_sender_id ON reports (user_sender_id);
CREATE INDEX idx_reports_reported_user_id ON reports (reported_user_id);
CREATE INDEX idx_reports_report_type_id ON reports (report_type_id);
CREATE INDEX idx_reports_report_status_id ON reports (report_status_id);
CREATE INDEX idx_reports_reviewed_by ON reports (reviewed_by);

-- Subscribers
CREATE INDEX idx_subscribers_follower_id ON subscribers (follower_id);
CREATE INDEX idx_subscribers_followed_id ON subscribers (followed_id);

-- User Likes
CREATE INDEX idx_user_likes_user_id ON user_likes (user_id);
CREATE INDEX idx_user_likes_post_id ON user_likes (post_id);

-- User Roles
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

-- User Saved Posts
CREATE INDEX idx_user_saved_posts_user_id ON user_saved_posts (user_id);
CREATE INDEX idx_user_saved_posts_post_id ON user_saved_posts (post_id);

-- Users
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_nickname ON users (nickname);

-- Hashtags
CREATE INDEX idx_hashtags_title ON hashtags (title);

-- Posts (Additional Indexes)
CREATE INDEX idx_posts_created_at ON posts (created_at);
CREATE INDEX idx_posts_post_likes ON posts (post_likes);
CREATE INDEX idx_posts_post_comments ON posts (post_comments);

-- Places (Additional Indexes)
CREATE INDEX idx_places_rating ON places (rating);

-- Reports (Additional Indexes)
CREATE INDEX idx_reports_created_at ON reports (created_at);

-- Messages
CREATE INDEX idx_messages_chat_id_created_at ON messages (chat_id, created_at);

-- Notifications
CREATE INDEX idx_notifications_recipient_id_is_read ON notifications (recipient_id, is_read);

-- Posts
CREATE INDEX idx_posts_user_id_created_at ON posts (user_id, created_at);

-- Users (GIN Index)
CREATE INDEX idx_users_black_list ON users USING GIN (black_list);

-- Messages (BRIN Index)
CREATE INDEX idx_messages_created_at_brin ON messages USING BRIN (created_at);

-- Notifications (Partial Index)
CREATE INDEX idx_notifications_unread ON notifications (recipient_id) WHERE is_read = false;

-- Posts (INCLUDE Index)
CREATE INDEX idx_posts_created_at_include_title ON posts (created_at) INCLUDE (title);

-- Users (Expression Index)
CREATE INDEX idx_users_lower_email ON users (LOWER(email));

-- -- Parallel Workers
-- SET max_parallel_workers = 8;
-- SET max_parallel_workers_per_gather = 4;
--
-- -- JIT Compilation
-- SET jit = on;
--
-- -- Memory Settings
-- SET shared_buffers = '512MB';
-- SET work_mem = '64MB';
-- SET maintenance_work_mem = '128MB';
-- SET effective_cache_size = '1536MB';
--
-- -- Checkpoint Settings
-- SET checkpoint_timeout = '15min';
-- SET checkpoint_completion_target = 0.9;
--
-- -- Connection Settings
-- SET max_connections = 100;
-- SET idle_in_transaction_session_timeout = '10min';
--
-- -- Autovacuum
-- SET autovacuum = on;

-- -- Reload Configuration
-- SELECT pg_reload_conf();