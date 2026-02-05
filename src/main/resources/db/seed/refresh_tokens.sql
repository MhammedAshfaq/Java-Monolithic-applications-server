-- Seed data for refresh_tokens table
-- Run: ./scripts/run-seed.sh refresh_tokens
-- Requires: users seed to be run first

INSERT INTO refresh_tokens (id, user_id, token, expires_at, created_at, revoked) VALUES
    ('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'admin-refresh-token-sample-12345', NOW() + INTERVAL '7 days', NOW(), false),
    ('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'john-refresh-token-sample-12345', NOW() + INTERVAL '7 days', NOW(), false),
    ('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'jane-refresh-token-sample-12345', NOW() + INTERVAL '7 days', NOW(), false)
ON CONFLICT (token) DO NOTHING;
