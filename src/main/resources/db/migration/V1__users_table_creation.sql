CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    full_name     VARCHAR(128)          NOT NULL,
    password      VARCHAR(256)          NOT NULL,
    email         VARCHAR(128)          NOT NULL,
    avatar_url    VARCHAR(256),
    currency_code VARCHAR(3)            NOT NULL,
    is_activated  BOOLEAN               NOT NULL,
    created_at    datetime              NOT NULL,
    updated_at    datetime              NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email)
);