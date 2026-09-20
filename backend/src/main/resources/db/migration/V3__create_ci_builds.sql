-- ==============================================================================
-- CloudShip — Flyway Migration V3: Continuous Integration (CI) Builds Schema
-- ==============================================================================
-- Establishes the ci_builds table for Phase 3 (Jenkins CI Integration).
-- Records build lifecycle, commit metadata, Jenkins job details, and Docker tags.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS ci_builds (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    git_repository_id BIGINT,
    commit_sha VARCHAR(100),
    branch VARCHAR(100) NOT NULL DEFAULT 'main',
    trigger_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    commit_message VARCHAR(500),
    commit_author VARCHAR(100),
    jenkins_build_number INT,
    jenkins_job_name VARCHAR(100),
    docker_image_name VARCHAR(200),
    docker_image_tag VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_ci_builds_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_builds_git_repository FOREIGN KEY (git_repository_id) REFERENCES git_repositories(id) ON DELETE SET NULL,
    CONSTRAINT chk_ci_builds_status CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'ABORTED')),
    CONSTRAINT chk_ci_builds_trigger CHECK (trigger_type IN ('MANUAL', 'WEBHOOK'))
);

CREATE INDEX IF NOT EXISTS idx_ci_builds_project_id ON ci_builds(project_id);
CREATE INDEX IF NOT EXISTS idx_ci_builds_status ON ci_builds(status);
CREATE INDEX IF NOT EXISTS idx_ci_builds_created_at ON ci_builds(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ci_builds_commit_sha ON ci_builds(commit_sha);
