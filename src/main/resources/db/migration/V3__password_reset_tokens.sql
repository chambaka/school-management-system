CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    session_hash VARCHAR(128) NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed TINYINT(1) NOT NULL DEFAULT 0,
    verified_at DATETIME(6) NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_reset_user (user_id),
    KEY idx_reset_session (session_hash),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
