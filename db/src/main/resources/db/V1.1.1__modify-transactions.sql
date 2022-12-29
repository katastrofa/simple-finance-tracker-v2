
ALTER TABLE transactions ADD COLUMN currency VARCHAR(16) AFTER money_account;
ALTER TABLE transactions ADD COLUMN dest_currency VARCHAR(16) AFTER dest_money_account;


UPDATE transactions AS t
    JOIN money_accounts AS m
        ON t.money_account = m.id
    SET t.currency = m.currency;

UPDATE transactions AS t
    JOIN money_accounts AS m
        ON t.dest_money_account = m.id
    SET t.dest_currency = m.currency;

ALTER TABLE transactions CHANGE COLUMN currency currency VARCHAR(16) NOT NULL;

ALTER TABLE transactions ADD FOREIGN KEY (currency) REFERENCES currencies(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE transactions ADD FOREIGN KEY (dest_currency) REFERENCES currencies(id) ON UPDATE CASCADE ON DELETE CASCADE;
