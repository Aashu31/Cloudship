-- ==============================================================================
-- CloudShip — Flyway Migration V5: Azure Container Registry (ACR) Integration
-- ==============================================================================
-- Extends the ci_builds table for Version 5 (ACR Integration).
-- Records ACR registry metadata, image push status, timestamps, duration,
-- immutable image digest, and push failure error information.
-- ==============================================================================

ALTER TABLE ci_builds
    ADD COLUMN IF NOT EXISTS registry_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS registry_login_server VARCHAR(200),
    ADD COLUMN IF NOT EXISTS push_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN IF NOT EXISTS push_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS push_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS push_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS push_error_message VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS image_digest VARCHAR(200);

-- Ensure push status is constrained to valid lifecycle states
ALTER TABLE ci_builds
    DROP CONSTRAINT IF EXISTS chk_ci_builds_push_status;

ALTER TABLE ci_builds
    ADD CONSTRAINT chk_ci_builds_push_status
    CHECK (push_status IN ('NOT_STARTED', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'));

-- Performance index for filtering/querying builds by push status
CREATE INDEX IF NOT EXISTS idx_ci_builds_push_status ON ci_builds(push_status);
CREATE INDEX IF NOT EXISTS idx_ci_builds_image_digest ON ci_builds(image_digest);
