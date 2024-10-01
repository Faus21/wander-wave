CREATE TABLE category_types
(
    category_type_id VARCHAR(255) NOT NULL,
    name             VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_category_types PRIMARY KEY (category_type_id)
);

CREATE TABLE chat_rooms
(
    chat_id      VARCHAR(255) NOT NULL,
    sender_id    VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    muted        BOOLEAN      NOT NULL,
    CONSTRAINT pk_chat_rooms PRIMARY KEY (chat_id)
);

CREATE TABLE comments
(
    comment_id VARCHAR(255)                NOT NULL,
    content    VARCHAR(255)                NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (comment_id)
);

CREATE TABLE email_tokens
(
    token_id     VARCHAR(255)                NOT NULL,
    content      VARCHAR(255)                NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    validated_at TIMESTAMP WITHOUT TIME ZONE,
    user_id      VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_email_tokens PRIMARY KEY (token_id)
);

CREATE TABLE hashtags
(
    hashtag_id VARCHAR(255) NOT NULL,
    title      VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_hashtags PRIMARY KEY (hashtag_id)
);

CREATE TABLE messages
(
    chat_id      VARCHAR(255)                NOT NULL,
    content      VARCHAR(1024)               NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sender_id    VARCHAR(255)                NOT NULL,
    recipient_id VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (sender_id, recipient_id)
);

CREATE TABLE notifications
(
    notification_id VARCHAR(255)                NOT NULL,
    content         VARCHAR(255)                NOT NULL,
    recipient_id    VARCHAR(255)                NOT NULL,
    is_read         BOOLEAN                     NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type            VARCHAR(255)                NOT NULL,
    object_id       VARCHAR(255)                NOT NULL,
    action_user_id  VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (notification_id)
);

CREATE TABLE places
(
    place_id    VARCHAR(255)     NOT NULL,
    description VARCHAR(500)     NOT NULL,
    rating      DOUBLE PRECISION NOT NULL,
    longitude   numeric(9, 6)    NOT NULL,
    latitude    numeric(9, 6)    NOT NULL,
    image_url   TEXT,
    post_id     VARCHAR(255)     NOT NULL,
    CONSTRAINT pk_places PRIMARY KEY (place_id)
);

CREATE TABLE post_hashtags
(
    hashtag_id VARCHAR(255) NOT NULL,
    post_id    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_post_hashtags PRIMARY KEY (hashtag_id, post_id)
);

CREATE TABLE posts
(
    post_id          VARCHAR(255)                NOT NULL,
    title            VARCHAR(100)                NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id          VARCHAR(255)                NOT NULL,
    description      TEXT,
    category_type_id VARCHAR(255)                NOT NULL,
    pros             TEXT[],
    cons             TEXT[],
    CONSTRAINT pk_posts PRIMARY KEY (post_id)
);

CREATE TABLE refresh_tokens
(
    refresh_token_id VARCHAR(255)                NOT NULL,
    token            VARCHAR(255)                NOT NULL,
    user_id          VARCHAR(255),
    expires_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (refresh_token_id)
);

CREATE TABLE report_types
(
    report_type_id VARCHAR(255) NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_report_types PRIMARY KEY (report_type_id)
);

CREATE TABLE reports
(
    report_id      VARCHAR(255)                NOT NULL,
    description    VARCHAR(1024)               NOT NULL,
    user_sender_id VARCHAR(255)                NOT NULL,
    user_id        VARCHAR(255),
    report_type_id VARCHAR(255)                NOT NULL,
    object_id      VARCHAR(8)                  NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_reports PRIMARY KEY (report_id)
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
    followed_id VARCHAR(255)
);

CREATE TABLE user_likes
(
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_user_likes PRIMARY KEY (user_id, post_id)
);

CREATE TABLE user_roles
(
    role_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE user_saved_posts
(
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_user_saved_posts PRIMARY KEY (user_id, post_id)
);

CREATE TABLE user_viewed_posts
(
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    post_id    VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_user_viewed_posts PRIMARY KEY (user_id, post_id)
);

CREATE TABLE users
(
    user_id        VARCHAR(255) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(100) NOT NULL,
    description    VARCHAR(255) NOT NULL,
    black_list     JSONB,
    account_locked BOOLEAN      NOT NULL,
    enabled        BOOLEAN      NOT NULL,
    image_url      TEXT,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

ALTER TABLE category_types
    ADD CONSTRAINT uc_262a074e684c09bb357b586ab UNIQUE (category_type_id);

ALTER TABLE report_types
    ADD CONSTRAINT uc_c35b8d09c6c4611ee01f237ba UNIQUE (report_type_id);

ALTER TABLE category_types
    ADD CONSTRAINT uc_category_types_name UNIQUE (name);

ALTER TABLE chat_rooms
    ADD CONSTRAINT uc_chat_rooms_recipient UNIQUE (recipient_id);

ALTER TABLE hashtags
    ADD CONSTRAINT uc_hashtags_title UNIQUE (title);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_user UNIQUE (user_id);

ALTER TABLE report_types
    ADD CONSTRAINT uc_report_types_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_nickname UNIQUE (nickname);

ALTER TABLE chat_rooms
    ADD CONSTRAINT FK_CHAT_ROOMS_ON_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES users (user_id);

ALTER TABLE chat_rooms
    ADD CONSTRAINT FK_CHAT_ROOMS_ON_SENDER FOREIGN KEY (sender_id) REFERENCES users (user_id);

ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGE_CHAT FOREIGN KEY (chat_id) REFERENCES chat_rooms (chat_id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATION_RECIPIENT_USER FOREIGN KEY (recipient_id) REFERENCES users (user_id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATION_SENDER_USER FOREIGN KEY (action_user_id) REFERENCES users (user_id);

ALTER TABLE places
    ADD CONSTRAINT FK_PLACE_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE posts
    ADD CONSTRAINT FK_POST_CATEGORY_TYPE FOREIGN KEY (category_type_id) REFERENCES category_types (category_type_id);

ALTER TABLE posts
    ADD CONSTRAINT FK_POST_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORTED_USER_REPORT FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORT_TYPE_REPORT FOREIGN KEY (report_type_id) REFERENCES report_types (report_type_id);

ALTER TABLE email_tokens
    ADD CONSTRAINT FK_TOKEN_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE user_likes
    ADD CONSTRAINT FK_USER_LIKES_ON_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE user_likes
    ADD CONSTRAINT FK_USER_LIKES_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE user_saved_posts
    ADD CONSTRAINT FK_USER_SAVED_POSTS_ON_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE user_saved_posts
    ADD CONSTRAINT FK_USER_SAVED_POSTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reports
    ADD CONSTRAINT FK_USER_SENDER_REPORT FOREIGN KEY (user_sender_id) REFERENCES users (user_id);

ALTER TABLE user_viewed_posts
    ADD CONSTRAINT FK_USER_VIEWED_POSTS_ON_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE user_viewed_posts
    ADD CONSTRAINT FK_USER_VIEWED_POSTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE post_hashtags
    ADD CONSTRAINT fk_poshas_on_hash_tag FOREIGN KEY (hashtag_id) REFERENCES hashtags (hashtag_id);

ALTER TABLE post_hashtags
    ADD CONSTRAINT fk_poshas_on_post FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE subscribers
    ADD CONSTRAINT fk_subscribers_on_user FOREIGN KEY (follower_id) REFERENCES users (user_id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roles (role_id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (user_id);