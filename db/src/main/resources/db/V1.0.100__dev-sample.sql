
INSERT INTO accounts (name, permalink, owner) VALUES
    ('Test', 'test', 1);
UPDATE users SET permissions = JSON_INSERT(permissions, '$.perAccount."1"', JSON_EXTRACT(permissions, '$.default'));
INSERT INTO categories (name, description, parent, account, owner) VALUES
    ('Test Cat', null, null, 1, 1),
    ('Test SubCat', null, 1, 1, 1),
    ('Other', 'Some other stuff', null, 1, 1),
    ('Other SubCat', null, 3, 1, 1),
    ('SubSubCat', 'WOW', 4, 1, 1);
INSERT INTO money_accounts (name, start_amount, currency, created, account, owner) VALUES
    ('Test Bank Account', 42.80, 'EUR', '2022-10-01', 1, 1),
    ('Test Cash', 20, 'EUR', '2022-10-01', 1, 1);

INSERT INTO transactions (date, type, amount, description, category, money_account, dest_amount, dest_money_account, owner) VALUES
    ('2022-10-12', 'Expense', 12, 'Expense 01', 2, 1, null, null, 1),
    ('2022-10-22', 'Expense', 40.20, 'Expense 02', 1, 2, null, null, 1),
    ('2022-10-16', 'Expense', 185.70, 'Expense 03', 1, 1, null, null, 1),
    ('2022-10-15', 'Transfer', 50, 'Transfer 01', 4, 1, 50, 2, 1),
    ('2022-10-03', 'Income', 2134, 'Income 01', 3, 1, null, null, 1),
    ('2022-10-07', 'Income', 55, 'Income 02', 5, 2, null, null, 1);
