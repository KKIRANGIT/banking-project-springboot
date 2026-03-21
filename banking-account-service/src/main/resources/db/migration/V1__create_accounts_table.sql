CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
);

CREATE INDEX idx_accounts_status ON accounts (status);
