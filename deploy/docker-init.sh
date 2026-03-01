#!/bin/bash
set -e

MIGRATIONS_DIR="/migrations"

# Run Flyway migrations in correct version order.
# We list files explicitly because alphabetical sorting breaks
# for multi-digit versions (e.g. V1.0.100 sorts before V1.0.3).
MIGRATION_FILES=(
  "V1.0.0__add-users.sql"
  "V1.0.1__add-accounts.sql"
  "V1.0.3__add_logins.sql"
  "V1.0.4__add-currency.sql"
  "V1.0.5__add-money-accounts.sql"
  "V1.0.6__add-categories.sql"
  "V1.0.7__add-transactions.sql"
  "V1.0.99__sample-data.sql"
  "V1.0.100__dev-sample.sql"
  "V1.1.0__add-money-account-currencies.sql"
  "V1.1.1__modify-transactions.sql"
  "V1.1.2__migrate-money-accounts.sql"
  "V1.1.99__sample-data.sql"
  "V1.2.0__rename-columns.sql"
  "V1.2.1__dev-seed-data.sql"
)

echo "Running database migrations..."
for file in "${MIGRATION_FILES[@]}"; do
  filepath="$MIGRATIONS_DIR/$file"
  if [ -f "$filepath" ]; then
    echo "  Applying: $file"
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" sftv2 < "$filepath"
  else
    echo "  SKIPPING (not found): $file"
  fi
done
echo "All migrations applied."
