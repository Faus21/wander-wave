CREATE TABLE category_types
(
    category_type_id BIGINT      NOT NULL,
    name             VARCHAR(50) NOT NULL,
    CONSTRAINT pk_category_types PRIMARY KEY (category_type_id)
);

CREATE TABLE chats
(
    chat_id     VARCHAR(255) NOT NULL,
    user1_id    VARCHAR(255) NOT NULL,
    user2_id    VARCHAR(255) NOT NULL,
    user_1_mute BOOLEAN      NOT NULL,
    user_2_mute BOOLEAN      NOT NULL,
    CONSTRAINT pk_chats PRIMARY KEY (chat_id)
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

CREATE TABLE hashtags
(
    hashtag_id VARCHAR(255) NOT NULL,
    title      VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_hashtags PRIMARY KEY (hashtag_id)
);

CREATE TABLE messages
(
    message_id VARCHAR(255)                NOT NULL,
    user_id    VARCHAR(255)                NOT NULL,
    chat_id    VARCHAR(255)                NOT NULL,
    content    VARCHAR(1024)               NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (message_id)
);

CREATE TABLE notifications
(
    notification_id VARCHAR(255)                NOT NULL,
    content         TEXT                        NOT NULL,
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
    category_type_id BIGINT                      NOT NULL,
    pros             TEXT[],
    cons             TEXT[],
    CONSTRAINT pk_posts PRIMARY KEY (post_id)
);

CREATE TABLE report_types
(
    report_type_id BIGINT      NOT NULL,
    name           VARCHAR(50) NOT NULL,
    CONSTRAINT pk_report_types PRIMARY KEY (report_type_id)
);

CREATE TABLE reports
(
    report_id      VARCHAR(255)  NOT NULL,
    description    VARCHAR(1024) NOT NULL,
    user_sender_id VARCHAR(255)  NOT NULL,
    user_id        VARCHAR(255),
    report_type_id BIGINT        NOT NULL,
    object_id      VARCHAR(8)    NOT NULL,
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
    user_id       VARCHAR(255) NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(100) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    access_token  VARCHAR(300),
    refresh_token VARCHAR(300),
    black_list    JSONB,
    image_url     TEXT,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

ALTER TABLE category_types
    ADD CONSTRAINT uc_262a074e684c09bb357b586ab UNIQUE (category_type_id);

ALTER TABLE reports
    ADD CONSTRAINT uc_6b16a63096b25c971350ca845 UNIQUE (report_id);

ALTER TABLE users
    ADD CONSTRAINT uc_74165e195b2f7b25de690d14a UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_77584fbe74cc86922be2a3560 UNIQUE (username);

ALTER TABLE roles
    ADD CONSTRAINT uc_81193087dbf8b34e6cad2f958 UNIQUE (role_id);

ALTER TABLE reports
    ADD CONSTRAINT uc_9019fdd54e97208edf9a1c549 UNIQUE (object_id);

ALTER TABLE hashtags
    ADD CONSTRAINT uc_9e463b4954335056defc66a9f UNIQUE (hashtag_id);

ALTER TABLE report_types
    ADD CONSTRAINT uc_c35b8d09c6c4611ee01f237ba UNIQUE (report_type_id);

ALTER TABLE category_types
    ADD CONSTRAINT uc_category_types_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_d0b7980d2354b7d22c2bbf9f9 UNIQUE (user_id);

ALTER TABLE reports
    ADD CONSTRAINT uc_dc04d5e2cd404fa93593ac6dd UNIQUE (user_sender_id);

ALTER TABLE hashtags
    ADD CONSTRAINT uc_hashtags_title UNIQUE (title);

ALTER TABLE report_types
    ADD CONSTRAINT uc_report_types_name UNIQUE (name);

ALTER TABLE chats
    ADD CONSTRAINT FK_CHATS_ON_USER1 FOREIGN KEY (user1_id) REFERENCES users (user_id);

ALTER TABLE chats
    ADD CONSTRAINT FK_CHATS_ON_USER2 FOREIGN KEY (user2_id) REFERENCES users (user_id);

ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENT_POST FOREIGN KEY (post_id) REFERENCES posts (post_id);

ALTER TABLE comments
    ADD CONSTRAINT FK_COMMENT_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGE_CHAT FOREIGN KEY (chat_id) REFERENCES chats (chat_id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGE_USER FOREIGN KEY (user_id) REFERENCES users (user_id);

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

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORTED_USER_REPORT FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reports
    ADD CONSTRAINT FK_REPORT_TYPE_REPORT FOREIGN KEY (report_type_id) REFERENCES report_types (report_type_id);

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