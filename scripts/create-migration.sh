#!/bin/bash

# Usage: ./scripts/create-migration.sh action_name
# Example: ./scripts/create-migration.sh add_phone_to_users
# Creates: src/main/resources/db/migration/20260205143022__add_phone_to_users.sql

if [ -z "$1" ]; then
    echo "Usage: ./scripts/create-migration.sh <action_name>"
    echo "Example: ./scripts/create-migration.sh add_phone_to_users"
    exit 1
fi

ACTION_NAME="$1"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
MIGRATION_DIR="src/main/resources/db/migration"
FILE_NAME="${TIMESTAMP}__${ACTION_NAME}.sql"
FILE_PATH="${MIGRATION_DIR}/${FILE_NAME}"

# Create migration directory if it doesn't exist
mkdir -p "$MIGRATION_DIR"

# Create migration file with template
cat > "$FILE_PATH" << EOF
-- ${FILE_NAME}
-- Description: TODO - Add description

-- Write your SQL here

EOF

echo "Created migration: $FILE_PATH"
echo ""
echo "Edit the file and add your SQL statements."
