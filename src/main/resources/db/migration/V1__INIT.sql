CREATE TABLE category_types
(
    category_type_id VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    image_url        TEXT,
    CONSTRAINT pk_category_types PRIMARY KEY (category_type_id),
    CONSTRAINT uc_category_types_name UNIQUE (name)
);

CREATE TABLE users
(
    user_id             VARCHAR(255) NOT NULL,
    nickname            VARCHAR(50)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password            VARCHAR(100) NOT NULL,
    description         VARCHAR(255) NOT NULL,
    black_list          JSONB,
    account_locked      BOOLEAN      NOT NULL,
    enabled             BOOLEAN      NOT NULL,
    subscriber_count    INT          NOT NULL,
    subscriptions_count INT          NOT NULL,
    image_url           TEXT,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uc_users_email UNIQUE (email),
    CONSTRAINT uc_users_nickname UNIQUE (nickname)
);

CREATE TABLE chat_rooms
(
    chat_id      VARCHAR(255) NOT NULL,
    sender_id    VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    muted        BOOLEAN      NOT NULL,
    CONSTRAINT pk_chat_rooms PRIMARY KEY (chat_id),
    CONSTRAINT FK_CHAT_ROOMS_ON_SENDER FOREIGN KEY (sender_id) REFERENCES users (user_id),
    CONSTRAINT FK_CHAT_ROOMS_ON_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES users (user_id)
);

CREATE TABLE comments
(
    comment_id VARCHAR(255)                NOT NULL,
    content    VARCHAR(255)                NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (comment_id),
    CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE comment_reports
(
    report_id  VARCHAR(255) NOT NULL,
    comment_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_comment_reports PRIMARY KEY (report_id),
    CONSTRAINT FK_COMMENT_REPORT FOREIGN KEY (comment_id) REFERENCES comments (comment_id)
);

CREATE TABLE email_tokens
(
    token_id     VARCHAR(255)                NOT NULL,
    content      VARCHAR(255)                NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    validated_at TIMESTAMP WITHOUT TIME ZONE,
    user_id      VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_email_tokens PRIMARY KEY (token_id),
    CONSTRAINT FK_TOKEN_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE hashtags
(
    hashtag_id VARCHAR(255) NOT NULL,
    title      VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_hashtags PRIMARY KEY (hashtag_id),
    CONSTRAINT uc_hashtags_title UNIQUE (title)
);

CREATE TABLE messages
(
    id            VARCHAR(255)                NOT NULL,
    chat_id       VARCHAR(255)                NOT NULL,
    content       VARCHAR(1024)               NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sender_id     VARCHAR(255)                NOT NULL,
    recipient_id  VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_message_chat FOREIGN KEY (chat_id)
        REFERENCES chat_rooms (chat_id)
);

CREATE TABLE notifications
(
    notification_id VARCHAR(255)                NOT NULL,
    recipient_id    VARCHAR(255)                NOT NULL,
    is_read         BOOLEAN                     NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type            VARCHAR(255)                NOT NULL,
    object_id       VARCHAR(255)                NOT NULL,
    action_user_id  VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (notification_id),
    CONSTRAINT FK_NOTIFICATION_RECIPIENT_USER FOREIGN KEY (recipient_id) REFERENCES users (user_id),
    CONSTRAINT FK_NOTIFICATION_SENDER_USER FOREIGN KEY (action_user_id) REFERENCES users (user_id)
);

CREATE TABLE routes
(
    route_id                         VARCHAR(255) PRIMARY KEY,
    source_coords_display_name       VARCHAR(255),
    source_coords_location_name      VARCHAR(255),
    source_coords_latitude           NUMERIC(9, 6),
    source_coords_longitude          NUMERIC(9, 6),
    destination_coords_display_name  VARCHAR(255),
    destination_coords_location_name VARCHAR(255),
    destination_coords_latitude      NUMERIC(9, 6),
    destination_coords_longitude     NUMERIC(9, 6),
    description                      VARCHAR(500)
);

CREATE TABLE posts
(
    post_id              VARCHAR(255)                NOT NULL,
    title                VARCHAR(100)                NOT NULL,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id              VARCHAR(255)                NOT NULL,
    description          TEXT,
    category_type_id     VARCHAR(255)                NOT NULL,
    images               TEXT[],
    pros                 TEXT[],
    cons                 TEXT[],
    post_likes           INT                         NOT NULL,
    post_comments        INT                         NOT NULL,
    is_disabled_comments BOOLEAN,
    route_id             VARCHAR(255),
    CONSTRAINT pk_posts PRIMARY KEY (post_id),
    CONSTRAINT FK_POST_CATEGORY_TYPE FOREIGN KEY (category_type_id) REFERENCES category_types (category_type_id),
    CONSTRAINT FK_POST_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_posts_routes FOREIGN KEY (route_id) REFERENCES routes (route_id) ON DELETE SET NULL
);

CREATE TABLE places
(
    place_id      VARCHAR(255)     NOT NULL,
    display_name  VARCHAR(500)      NOT NULL,
    location_name VARCHAR(500)      NOT NULL,
    description   VARCHAR(500)     NOT NULL,
    rating        DOUBLE PRECISION NOT NULL,
    longitude     numeric(9, 6)    NOT NULL,
    latitude      numeric(9, 6)    NOT NULL,
    post_id       VARCHAR(255)     NOT NULL,
    CONSTRAINT pk_places PRIMARY KEY (place_id),
    CONSTRAINT FK_PLACE_POST FOREIGN KEY (post_id) REFERENCES posts (post_id)
);

CREATE TABLE post_hashtags
(
    hashtag_id VARCHAR(255) NOT NULL,
    post_id    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_post_hashtags PRIMARY KEY (hashtag_id, post_id),
    CONSTRAINT fk_poshas_on_hash_tag FOREIGN KEY (hashtag_id) REFERENCES hashtags (hashtag_id),
    CONSTRAINT fk_poshas_on_post FOREIGN KEY (post_id) REFERENCES posts (post_id)
);

CREATE TABLE post_reports
(
    report_id VARCHAR(255) NOT NULL,
    post_id   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_post_reports PRIMARY KEY (report_id),
    CONSTRAINT FK_POST_REPORT FOREIGN KEY (post_id) REFERENCES posts (post_id)
);

CREATE TABLE refresh_tokens
(
    refresh_token_id VARCHAR(255)                NOT NULL,
    token            TEXT                        NOT NULL,
    user_id          VARCHAR(255),
    expires_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (refresh_token_id),
    CONSTRAINT uc_refresh_tokens_token UNIQUE (token),
    CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE report_status
(
    report_status_id VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_report_status PRIMARY KEY (report_status_id),
    CONSTRAINT uc_report_status_name UNIQUE (name)
);

CREATE TABLE report_types
(
    report_type_id VARCHAR(255) NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_report_types PRIMARY KEY (report_type_id),
    CONSTRAINT uc_report_types_name UNIQUE (name)
);

CREATE TABLE reports
(
    report_id        VARCHAR(255)                NOT NULL,
    description      VARCHAR(1024)               NOT NULL,
    user_sender_id   VARCHAR(255)                NOT NULL,
    reported_user_id VARCHAR(255)                NOT NULL,
    report_type_id   VARCHAR(255)                NOT NULL,
    report_status_id VARCHAR(255)                NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reviewed_at      TIMESTAMP WITHOUT TIME ZONE,
    reviewed_by      VARCHAR(255),
    report_comment   VARCHAR(255),
    CONSTRAINT pk_reports PRIMARY KEY (report_id),
    CONSTRAINT FK_REPORT_STATUS_REPORT FOREIGN KEY (report_status_id) REFERENCES report_status (report_status_id),
    CONSTRAINT FK_REPORT_TYPE_REPORT FOREIGN KEY (report_type_id) REFERENCES report_types (report_type_id),
    CONSTRAINT FK_REVIEWED_BY_REPORT FOREIGN KEY (reviewed_by) REFERENCES users (user_id),
    CONSTRAINT FK_USER_REPORT FOREIGN KEY (reported_user_id) REFERENCES users (user_id),
    CONSTRAINT FK_USER_SENDER_REPORT FOREIGN KEY (user_sender_id) REFERENCES users (user_id)
);

CREATE TABLE roles
(
    role_id   VARCHAR(255) NOT NULL,
    role_type VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (role_id)
);

CREATE TABLE subscribers
(
    follower_id VARCHAR(255) NOT NULL,
    followed_id VARCHAR(255),
    CONSTRAINT fk_subscribers_on_user FOREIGN KEY (follower_id) REFERENCES users (user_id)
);

CREATE TABLE user_likes
(
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_user_likes PRIMARY KEY (user_id, post_id),
    CONSTRAINT FK_USER_LIKES_ON_POST FOREIGN KEY (post_id) REFERENCES posts (post_id),
    CONSTRAINT FK_USER_LIKES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE user_roles
(
    role_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id),
    CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE user_saved_posts
(
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_user_saved_posts PRIMARY KEY (user_id, post_id),
    CONSTRAINT FK_USER_SAVED_POSTS_ON_POST FOREIGN KEY (post_id) REFERENCES posts (post_id),
    CONSTRAINT FK_USER_SAVED_POSTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
);