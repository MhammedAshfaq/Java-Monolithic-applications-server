-- Seed data for users table
-- Run: ./scripts/run-seed.sh users
-- Password for all users: password123

INSERT INTO users (id, name, email, password, role, status, created_at) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Admin User', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6', 'ADMIN', 'ACTIVE', NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 'John Doe', 'john@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6', 'USER', 'ACTIVE', NOW()),
    ('550e8400-e29b-41d4-a716-446655440003', 'Jane Smith', 'jane@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6', 'USER', 'ACTIVE', NOW()),
    ('550e8400-e29b-41d4-a716-446655440004', 'Moderator', 'mod@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6', 'MODERATOR', 'ACTIVE', NOW()),
    ('550e8400-e29b-41d4-a716-446655440005', 'Inactive User', 'inactive@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye6VCLx1dO6/X.SqKQ7sP/IYUOQU3p9u6', 'USER', 'INACTIVE', NOW())
ON CONFLICT (email) DO NOTHING;

-- Summary:
-- | Email               | Password    | Role      | Status   |
-- |---------------------|-------------|-----------|----------|
-- | admin@example.com   | password123 | ADMIN     | ACTIVE   |
-- | john@example.com    | password123 | USER      | ACTIVE   |
-- | jane@example.com    | password123 | USER      | ACTIVE   |
-- | mod@example.com     | password123 | MODERATOR | ACTIVE   |
-- | inactive@example.com| password123 | USER      | INACTIVE |
