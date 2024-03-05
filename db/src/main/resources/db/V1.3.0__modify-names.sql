RENAME TABLE accounts TO wallets;
RENAME TABLE money_accounts TO accounts;
RENAME TABLE money_account_currencies TO account_currencies;

ALTER TABLE accounts RENAME COLUMN account TO wallet;
ALTER TABLE categories RENAME COLUMN account TO wallet;
ALTER TABLE transactions RENAME COLUMN money_account TO account;
ALTER TABLE transactions RENAME COLUMN dest_money_account TO dest_account;
ALTER TABLE account_currencies RENAME COLUMN money_account TO account;
ALTER TABLE patrons RENAME COLUMN account TO wallet;
