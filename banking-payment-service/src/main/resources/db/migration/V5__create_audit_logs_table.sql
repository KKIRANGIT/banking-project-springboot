CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    topic_name VARCHAR(100) NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    transaction_id BIGINT NULL,
    account_number VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_account_number ON audit_logs (account_number);
CREATE INDEX idx_audit_logs_transaction_id ON audit_logs (transaction_id);
