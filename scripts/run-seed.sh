#!/bin/bash
# Run seed data scripts
# Usage: ./scripts/run-seed.sh <seed_name|all>
#
# Examples:
#   ./scripts/run-seed.sh users           # Seed users only
#   ./scripts/run-seed.sh refresh_tokens  # Seed refresh tokens only
#   ./scripts/run-seed.sh all             # Seed all data
#   ./scripts/run-seed.sh                 # List available seeds

set -e

# Database configuration (matches application.yaml)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-java-spring-mololithic}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

SEED_DIR="src/main/resources/db/seed"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Show usage if no argument
if [ -z "$1" ]; then
    echo -e "${YELLOW}Available seed files:${NC}"
    echo ""
    for file in "$SEED_DIR"/*.sql; do
        if [ -f "$file" ]; then
            filename=$(basename "$file" .sql)
            echo "  - $filename"
        fi
    done
    echo ""
    echo "Usage: ./scripts/run-seed.sh <seed_name>"
    echo "Example: ./scripts/run-seed.sh users"
    echo "         ./scripts/run-seed.sh all"
    exit 0
fi

SEED_NAME="$1"
SEED_FILE="$SEED_DIR/${SEED_NAME}.sql"

# Check if seed file exists
if [ ! -f "$SEED_FILE" ]; then
    echo -e "${RED}Error: Seed file not found: $SEED_FILE${NC}"
    echo ""
    echo "Available seeds:"
    for file in "$SEED_DIR"/*.sql; do
        if [ -f "$file" ]; then
            filename=$(basename "$file" .sql)
            echo "  - $filename"
        fi
    done
    exit 1
fi

echo -e "${YELLOW}Running seed: $SEED_NAME${NC}"
echo "Database: $DB_NAME"
echo ""

# Run the seed file
if [ "$SEED_NAME" = "all" ]; then
    # For 'all', we need to run from the seed directory for \i to work
    cd "$SEED_DIR"
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "all.sql"
else
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SEED_FILE"
fi

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Seed '$SEED_NAME' completed successfully!${NC}"
else
    echo ""
    echo -e "${RED}✗ Seed '$SEED_NAME' failed${NC}"
    exit 1
fi
