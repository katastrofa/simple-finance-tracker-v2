-- Rich dev seed data: 3 wallets, categories, accounts, and transactions
-- Uses post-V1.2.0 table/column names
-- All IDs are explicit to avoid auto-increment surprises

-- ============================================================
-- WALLETS (old "accounts" table)
-- ============================================================
INSERT INTO wallets (id, name, permalink, owner) VALUES
    (1, 'Cash', 'cash', 1),
    (2, 'Main Bank Account', 'main-bank', 1),
    (3, 'Savings Account', 'savings', 1);

-- Update user permissions to include all 3 wallets
UPDATE users SET permissions = JSON_SET(
    permissions,
    '$.perWallet."1"', JSON_EXTRACT(permissions, '$.default'),
    '$.perWallet."2"', JSON_EXTRACT(permissions, '$.default'),
    '$.perWallet."3"', JSON_EXTRACT(permissions, '$.default')
) WHERE id = 1;

-- ============================================================
-- CATEGORIES
-- ============================================================

-- === Cash wallet (wallet=1) ===
INSERT INTO categories (id, name, description, parent, wallet, owner) VALUES
    (1,  'Income',           'All income sources',        NULL, 1, 1),
    (2,  'Food & Drink',     NULL,                        NULL, 1, 1),
    (3,  'Transport',        NULL,                        NULL, 1, 1),
    (4,  'Entertainment',    NULL,                        NULL, 1, 1),
    (5,  'Shopping',         NULL,                        NULL, 1, 1),
    (6,  'Health',           NULL,                        NULL, 1, 1),
    (7,  'Salary',           'Monthly salary',            1,    1, 1),
    (8,  'Other Income',     NULL,                        1,    1, 1),
    (9,  'Groceries',        NULL,                        2,    1, 1),
    (10, 'Restaurants',      NULL,                        2,    1, 1),
    (11, 'Coffee',           NULL,                        2,    1, 1),
    (12, 'Public Transport', NULL,                        3,    1, 1),
    (13, 'Streaming',        'Netflix, Spotify, etc.',    4,    1, 1),
    (14, 'Going Out',        NULL,                        4,    1, 1),
    (15, 'Clothing',         NULL,                        5,    1, 1),
    (16, 'Household',        NULL,                        5,    1, 1),
    (17, 'Pharmacy',         NULL,                        6,    1, 1),
    (18, 'Doctor',           NULL,                        6,    1, 1);

-- === Main Bank wallet (wallet=2) ===
INSERT INTO categories (id, name, description, parent, wallet, owner) VALUES
    (19, 'Income',           'All income sources',          NULL, 2, 1),
    (20, 'Housing',          NULL,                          NULL, 2, 1),
    (21, 'Food & Drink',     NULL,                          NULL, 2, 1),
    (22, 'Transport',        NULL,                          NULL, 2, 1),
    (23, 'Entertainment',    NULL,                          NULL, 2, 1),
    (24, 'Shopping',         NULL,                          NULL, 2, 1),
    (25, 'Health',           NULL,                          NULL, 2, 1),
    (26, 'Financial',        'Bank fees and transfers',     NULL, 2, 1),
    (27, 'Salary',           'Monthly salary',              19,  2, 1),
    (28, 'Freelance',        'Side projects',               19,  2, 1),
    (29, 'Other Income',     NULL,                          19,  2, 1),
    (30, 'Rent',             NULL,                          20,  2, 1),
    (31, 'Utilities',        'Electricity, water, internet',20,  2, 1),
    (32, 'Groceries',        NULL,                          21,  2, 1),
    (33, 'Restaurants',      NULL,                          21,  2, 1),
    (34, 'Coffee',           NULL,                          21,  2, 1),
    (35, 'Public Transport', NULL,                          22,  2, 1),
    (36, 'Fuel',             NULL,                          22,  2, 1),
    (37, 'Car Maintenance',  NULL,                          22,  2, 1),
    (38, 'Streaming',        'Netflix, Spotify, etc.',      23,  2, 1),
    (39, 'Going Out',        NULL,                          23,  2, 1),
    (40, 'Hobbies',          NULL,                          23,  2, 1),
    (41, 'Clothing',         NULL,                          24,  2, 1),
    (42, 'Electronics',      NULL,                          24,  2, 1),
    (43, 'Household',        NULL,                          24,  2, 1),
    (44, 'Pharmacy',         NULL,                          25,  2, 1),
    (45, 'Doctor',           NULL,                          25,  2, 1),
    (46, 'Bank Fees',        NULL,                          26,  2, 1),
    (47, 'Transfers',        'Between accounts',            26,  2, 1);

-- === Savings wallet (wallet=3) ===
INSERT INTO categories (id, name, description, parent, wallet, owner) VALUES
    (48, 'Income',        'Interest and transfers in', NULL, 3, 1),
    (49, 'Financial',     'Transfers and fees',        NULL, 3, 1),
    (50, 'Interest',      NULL,                        48,  3, 1),
    (51, 'Transfer In',   NULL,                        48,  3, 1),
    (52, 'Transfer Out',  NULL,                        49,  3, 1),
    (53, 'Bank Fees',     NULL,                        49,  3, 1);

-- ============================================================
-- ACCOUNTS (money accounts) + ACCOUNT CURRENCIES
-- ============================================================

-- Cash wallet: Wallet (EUR, start 200)
INSERT INTO accounts (id, name, created, wallet, owner) VALUES
    (1, 'Wallet', '2025-01-01', 1, 1);
INSERT INTO account_currencies (account, currency, start_amount) VALUES
    (1, 'EUR', 200.00);

-- Main Bank: Checking Account (EUR, start 3500)
INSERT INTO accounts (id, name, created, wallet, owner) VALUES
    (2, 'Checking Account', '2025-01-01', 2, 1);
INSERT INTO account_currencies (account, currency, start_amount) VALUES
    (2, 'EUR', 3500.00);

-- Savings: Savings EUR (EUR, start 10000)
INSERT INTO accounts (id, name, created, wallet, owner) VALUES
    (3, 'Savings EUR', '2025-01-01', 3, 1);
INSERT INTO account_currencies (account, currency, start_amount) VALUES
    (3, 'EUR', 10000.00);

-- ============================================================
-- TRANSACTIONS
-- ============================================================
-- Spanning January - March 2025
-- Account 1 = Wallet (Cash), Account 2 = Checking (Main Bank), Account 3 = Savings
-- Category references: see IDs above

-- === JANUARY 2025 ===

-- Salary (cat 27, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-05', 'Income', 3000.00, 'January salary', 27, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1);

-- Rent (cat 30, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-03', 'Expense', 800.00, 'Rent January', 30, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1);

-- Utilities (cat 31, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-08', 'Expense', 45.00, 'Electricity bill', 31, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-01-08', 'Expense', 25.00, 'Water bill', 31, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-01-10', 'Expense', 35.00, 'Internet subscription', 31, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Groceries (cat 32, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-06', 'Expense', 62.30, 'Weekly groceries - Lidl', 32, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-01-13', 'Expense', 55.80, 'Weekly groceries - Albert', 32, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-01-20', 'Expense', 71.40, 'Weekly groceries - Lidl', 32, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-01-27', 'Expense', 48.90, 'Weekly groceries', 32, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Restaurants & Coffee (cats 33, 34, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-11', 'Expense', 32.50, 'Dinner with friends', 33, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-01-18', 'Expense', 15.80, 'Lunch at work', 33, 2, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-01-09', 'Expense', 4.50, 'Morning coffee', 34, 2, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-01-22', 'Expense', 4.50, 'Morning coffee', 34, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Transport (cat 35, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-02', 'Expense', 30.00, 'Monthly transit pass', 35, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Streaming (cat 38, account 2)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-15', 'Expense', 13.99, 'Netflix', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-01-15', 'Expense', 9.99, 'Spotify', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Cash spending (account 1, various cash wallet categories)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-12', 'Expense', 8.50, 'Snacks at kiosk', 9, 1, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-01-19', 'Expense', 12.00, 'Taxi ride', 12, 1, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-01-25', 'Expense', 22.00, 'Pub with colleagues', 14, 1, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Transfer: Bank -> Cash (ATM withdrawal) (cat 47 = Transfers in Main Bank)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-10', 'Transfer', 100.00, 'ATM withdrawal', 47, 2, 'EUR', 'Verified', 100.00, 1, 'EUR', 1);

-- Transfer: Bank -> Savings (cat 47)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-06', 'Transfer', 500.00, 'Monthly savings', 47, 2, 'EUR', 'Verified', 500.00, 3, 'EUR', 1);

-- === FEBRUARY 2025 ===

-- Salary
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-05', 'Income', 3000.00, 'February salary', 27, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1);

-- Freelance income (cat 28)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-12', 'Income', 450.00, 'Freelance web project', 28, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1);

-- Rent
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-03', 'Expense', 800.00, 'Rent February', 30, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1);

-- Utilities
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-07', 'Expense', 52.00, 'Electricity bill', 31, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-07', 'Expense', 25.00, 'Water bill', 31, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-10', 'Expense', 35.00, 'Internet subscription', 31, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Groceries
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-03', 'Expense', 58.20, 'Weekly groceries - Lidl', 32, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-10', 'Expense', 67.50, 'Weekly groceries - Albert', 32, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-17', 'Expense', 73.10, 'Weekly groceries - Tesco', 32, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-02-24', 'Expense', 51.30, 'Weekly groceries', 32, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Restaurants
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-14', 'Expense', 65.00, 'Valentine dinner', 33, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-22', 'Expense', 18.50, 'Lunch at cafe', 33, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Transport
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-01', 'Expense', 30.00, 'Monthly transit pass', 35, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-02-15', 'Expense', 45.00, 'Fuel', 36, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Streaming
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-15', 'Expense', 13.99, 'Netflix', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-02-15', 'Expense', 9.99, 'Spotify', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Shopping
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-08', 'Expense', 89.00, 'Winter jacket', 41, 2, 'EUR', 'Verified', NULL, NULL, NULL, 1),
    ('2025-02-20', 'Expense', 29.99, 'USB-C hub', 42, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Health
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-18', 'Expense', 12.50, 'Cold medicine', 44, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Cash spending (account 1)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-09', 'Expense', 15.00, 'Market shopping', 9, 1, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-02-16', 'Expense', 6.00, 'Coffee and pastry', 11, 1, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-02-23', 'Expense', 35.00, 'Board game night drinks', 14, 1, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Transfers
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-07', 'Transfer', 80.00, 'ATM withdrawal', 47, 2, 'EUR', 'Verified', 80.00, 1, 'EUR', 1),
    ('2025-02-06', 'Transfer', 500.00, 'Monthly savings', 47, 2, 'EUR', 'Verified', 500.00, 3, 'EUR', 1);

-- Bank fees (cat 46)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-02-28', 'Expense', 3.50, 'Monthly account fee', 46, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- === MARCH 2025 ===

-- Salary
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-05', 'Income', 3000.00, 'March salary', 27, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Rent
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-03', 'Expense', 800.00, 'Rent March', 30, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Utilities
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-07', 'Expense', 48.00, 'Electricity bill', 31, 2, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-03-07', 'Expense', 25.00, 'Water bill', 31, 2, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-03-10', 'Expense', 35.00, 'Internet subscription', 31, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Groceries
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-03', 'Expense', 64.70, 'Weekly groceries - Lidl', 32, 2, 'EUR', 'None', NULL, NULL, NULL, 1),
    ('2025-03-10', 'Expense', 59.20, 'Weekly groceries', 32, 2, 'EUR', 'None', NULL, NULL, NULL, 1);

-- Streaming
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-15', 'Expense', 13.99, 'Netflix', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-03-15', 'Expense', 9.99, 'Spotify', 38, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Transport
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-01', 'Expense', 30.00, 'Monthly transit pass', 35, 2, 'EUR', 'Auto', NULL, NULL, NULL, 1);

-- Transfers
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-03-06', 'Transfer', 500.00, 'Monthly savings', 47, 2, 'EUR', 'None', 500.00, 3, 'EUR', 1),
    ('2025-03-08', 'Transfer', 100.00, 'ATM withdrawal', 47, 2, 'EUR', 'None', 100.00, 1, 'EUR', 1);

-- Savings interest (cat 50, account 3)
INSERT INTO transactions (date, op, amount, description, category, account, currency, status, dest_amount, dest_account, dest_currency, owner) VALUES
    ('2025-01-31', 'Income', 8.50, 'Monthly interest', 50, 3, 'EUR', 'Auto', NULL, NULL, NULL, 1),
    ('2025-02-28', 'Income', 9.20, 'Monthly interest', 50, 3, 'EUR', 'Auto', NULL, NULL, NULL, 1);
