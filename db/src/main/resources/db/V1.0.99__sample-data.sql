
INSERT INTO currencies VALUES
    ('USD', 'Dollar', '$'),
    ('EUR', 'Euro', '€'),
    ('GBP', 'Pound Sterling', '£'),
    ('ILS', 'Israeli New Shekel', '₪'),
    ('CHF', 'Swiss Franc', 'Fr');

INSERT INTO users (email, display_name, permissions) VALUE
    ('katastrofa42@gmail.com', 'Peter',
       '{"global": ["Basic", "ModifyOwnAccount", "ModifyAccount", "DeleteOwnAccount", "DeleteAccount"], "perAccount": {}, "default": ["Basic", "ModifyOwnCategory", "ModifyOwnMoneyAccount", "ModifyOwnTransactions", "ModifyCategory", "ModifyMoneyAccount", "ModifyTransactions", "DeleteCategory", "DeleteMoneyAccount", "DeleteTransactions"]}'
    );
