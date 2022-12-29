
INSERT INTO money_account_currencies (money_account, currency, start_amount)
    SELECT id, currency, start_amount FROM money_accounts;

ALTER TABLE money_accounts DROP COLUMN start_amount;
ALTER TABLE money_accounts DROP COLUMN currency;
