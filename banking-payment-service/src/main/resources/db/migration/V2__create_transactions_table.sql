CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_amount ON transactions (amount);
