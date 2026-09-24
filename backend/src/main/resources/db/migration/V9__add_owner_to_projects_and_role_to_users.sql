-- ==============================================================================
-- CloudShip — Flyway Migration V9: Add Owner to Projects and Role to Users
-- ==============================================================================
-- Establishes user role management and links projects to an owner for
-- Zero-Trust authentication and server-side authorization (IDOR prevention).
-- ==============================================================================

-- 1. Add role column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'USER' NOT NULL;

-- 2. Add owner_id column to projects table
ALTER TABLE projects ADD COLUMN IF NOT EXISTS owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- 3. Create index on owner_id for efficient query filtering
CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON projects(owner_id);

-- 4. If any projects exist and at least one user exists, link unassigned projects to the first user
DO $$
DECLARE
    first_user_id BIGINT;
BEGIN
    SELECT id INTO first_user_id FROM users ORDER BY id ASC LIMIT 1;
    IF first_user_id IS NOT NULL THEN
        UPDATE projects SET owner_id = first_user_id WHERE owner_id IS NULL;
    END IF;
END $$;
