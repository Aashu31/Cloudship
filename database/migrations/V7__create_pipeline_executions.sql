-- ==============================================================================
-- CloudShip — Flyway Migration V7: Full CI/CD Pipeline Orchestration
-- ==============================================================================
-- Creates pipeline_executions table to track the unified end-to-end lifecycle:
-- Developer -> GitHub -> Jenkins -> Checkout -> Build -> Test -> Docker Build
-- -> Azure Container Registry -> Image Verification -> Azure Kubernetes Service
-- -> Kubernetes Deployment -> Rollout Verification -> Success/Failure.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS pipeline_executions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    ci_build_id BIGINT,
    deployment_id BIGINT,
    status VARCHAR(50) NOT NULL,
    branch VARCHAR(255),
    commit_sha VARCHAR(100),
    commit_message TEXT,
    commit_author VARCHAR(255),
    trigger_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    image_name VARCHAR(255),
    image_tag VARCHAR(100),
    image_digest VARCHAR(255),
    cluster_name VARCHAR(100),
    namespace VARCHAR(100) DEFAULT 'default',
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pipeline_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_ci_build FOREIGN KEY (ci_build_id) REFERENCES ci_builds(id) ON DELETE SET NULL,
    CONSTRAINT fk_pipeline_deployment FOREIGN KEY (deployment_id) REFERENCES deployments(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_pipeline_project_id ON pipeline_executions(project_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_status ON pipeline_executions(status);
CREATE INDEX IF NOT EXISTS idx_pipeline_created_at ON pipeline_executions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pipeline_ci_build_id ON pipeline_executions(ci_build_id);
CREATE INDEX IF NOT EXISTS idx_pipeline_deployment_id ON pipeline_executions(deployment_id);
