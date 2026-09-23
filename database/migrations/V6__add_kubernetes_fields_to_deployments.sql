-- ==============================================================================
-- CloudShip — Flyway Migration V6: Azure Kubernetes Service (AKS) Integration
-- ==============================================================================
-- Extends the deployments table for Version 6 (Kubernetes / AKS Foundation).
-- Records Kubernetes cluster, namespace, deployment & service names, verified
-- container image reference (name, tag, digest), replicas, rollout progress,
-- error messages, and execution timestamps.
-- ==============================================================================

ALTER TABLE deployments
    ADD COLUMN IF NOT EXISTS ci_build_id BIGINT,
    ADD COLUMN IF NOT EXISTS cluster_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS namespace VARCHAR(100) DEFAULT 'default',
    ADD COLUMN IF NOT EXISTS deployment_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS service_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS image_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS image_tag VARCHAR(100),
    ADD COLUMN IF NOT EXISTS image_digest VARCHAR(255),
    ADD COLUMN IF NOT EXISTS replicas INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS ready_replicas INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_replicas INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_replicas INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rollout_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE;

-- Add foreign key constraint to ci_builds
ALTER TABLE deployments
    ADD CONSTRAINT fk_deployments_ci_build
    FOREIGN KEY (ci_build_id) REFERENCES ci_builds(id) ON DELETE SET NULL;

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_deployments_ci_build_id ON deployments(ci_build_id);
CREATE INDEX IF NOT EXISTS idx_deployments_cluster_name ON deployments(cluster_name);
CREATE INDEX IF NOT EXISTS idx_deployments_deployment_name ON deployments(deployment_name);
