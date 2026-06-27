CREATE TABLE users
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(128) NOT NULL,
    password           VARCHAR(256) NOT NULL,
    email              VARCHAR(128) NOT NULL UNIQUE,
    pending_email      VARCHAR(128),
    verification_token VARCHAR(256),
    avatar_id          VARCHAR(256),
    currency_code      VARCHAR(3)   NOT NULL,
    is_activated       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE accounts
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    balance    DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_account_user_id (user_id)
);

CREATE TABLE categories
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    icon       VARCHAR(255) NOT NULL,
    status     VARCHAR(255) NOT NULL,
    user_id    BIGINT       NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_category_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_category_user_id (user_id),
    UNIQUE KEY uk_category_user_name_type (user_id, name, type)
);

CREATE TABLE refresh_tokens
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    token       VARCHAR(255)          NOT NULL,
    expiry_date DATETIME              NOT NULL,
    user_id     BIGINT                NULL,
    created_at  DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uc_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_on_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);