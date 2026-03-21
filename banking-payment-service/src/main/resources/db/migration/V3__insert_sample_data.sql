INSERT INTO accounts (id, account_number, account_holder_name, balance, status, version) VALUES
    (1, 'ACC1001', 'Alice Johnson', 5000.00, 'ACTIVE', 0),
    (2, 'ACC1002', 'Brian Smith', 2500.00, 'ACTIVE', 0),
    (3, 'ACC1003', 'Carla Davis', 1200.00, 'SUSPENDED', 0);

INSERT INTO transactions (account_id, amount, currency, type, status, created_at) VALUES
    (1, 1500.00, 'USD', 'CREDIT', 'SUCCESS', '2026-03-20 09:00:00'),
    (1, 250.00, 'USD', 'DEBIT', 'SUCCESS', '2026-03-20 12:30:00'),
    (2, 900.00, 'USD', 'CREDIT', 'SUCCESS', '2026-03-20 15:45:00'),
    (2, 120.00, 'USD', 'DEBIT', 'SUCCESS', '2026-03-20 18:10:00'),
    (3, 300.00, 'USD', 'CREDIT', 'PENDING', '2026-03-20 20:15:00');
