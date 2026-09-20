-- ==============================================================================
-- CloudShip — Flyway Migration V2: Git Repositories Integration Schema
-- ==============================================================================
-- Establishes the git_repositories table for Phase 2 (GitHub Integration).
-- Associates projects with version control repository metadata and connection status.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS git_repositories (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL DEFAULT 'GITHUB',
    repository_url VARCHAR(255) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    repository_name VARCHAR(100) NOT NULL,
    default_branch VARCHAR(100) NOT NULL DEFAULT 'main',
    connection_status VARCHAR(50) NOT NULL DEFAULT 'NOT_CONNECTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_git_repositories_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_git_repositories_status CHECK (connection_status IN ('CONNECTED', 'NOT_CONNECTED', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_git_repositories_project_id ON git_repositories(project_id);
CREATE INDEX IF NOT EXISTS idx_git_repositories_owner_name ON git_repositories(owner, repository_name);
