# CloudShip — Database Design & Migrations

**Document Version:** 1.0.0  
**Phase:** Phase 1 (Application Foundation)  
**Database Engine:** PostgreSQL 15+  
**Migration Tool:** Flyway (`flyway-core` + `flyway-database-postgresql`)  

---

## 1. Entity-Relationship Model (Phase 1)

```
┌─────────────────────────┐
│          users          │
├─────────────────────────┤
│ id: BIGSERIAL (PK)      │
│ name: VARCHAR(100)      │
│ email: VARCHAR(255) (UQ)│
│ created_at: TIMESTAMPTZ │
│ updated_at: TIMESTAMPTZ │
└─────────────────────────┘

┌─────────────────────────┐         ┌─────────────────────────┐
│        projects         │ 1     * │       deployments       │
├─────────────────────────┤─────────├─────────────────────────┤
│ id: BIGSERIAL (PK)      │         │ id: BIGSERIAL (PK)      │
│ name: VARCHAR(100) (UQ) │         │ project_id: BIGINT (FK) │
│ description: VARCHAR    │         │ version: VARCHAR(50)    │
│ repository_url: VARCHAR │         │ status: VARCHAR(20)     │
│ created_at: TIMESTAMPTZ │         │ created_at: TIMESTAMPTZ │
│ updated_at: TIMESTAMPTZ │         │ completed_at: TIMESTAMPTZ│
└─────────────────────────┘         └─────────────────────────┘
```

---

## 2. Table Schemas

### 2.1 `users`
Persists operator and developer accounts.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique synthetic identifier |
| `name` | `VARCHAR(100)` | `NOT NULL` | Display name of user |
| `email` | `VARCHAR(255)` | `NOT NULL, UNIQUE` | User email address |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT CURRENT_TIMESTAMP` | Record creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT CURRENT_TIMESTAMP` | Last updated timestamp |

**Indexes:**
- `idx_users_email` on `email`

---

### 2.2 `projects`
Stores managed software projects and target Git repositories.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique project identifier |
| `name` | `VARCHAR(100)` | `NOT NULL, UNIQUE` | Project slug/name |
| `description` | `VARCHAR(500)` | `NULL` | Brief service description |
| `repository_url` | `VARCHAR(255)` | `NOT NULL` | HTTP(S) or Git repository URL |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT CURRENT_TIMESTAMP` | Project registration timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT CURRENT_TIMESTAMP` | Project metadata update timestamp |

**Indexes:**
- `idx_projects_name` on `name`

---

### 2.3 `deployments`
Tracks execution status and history of deployments.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique deployment execution ID |
| `project_id` | `BIGINT` | `NOT NULL, FK -> projects(id) ON DELETE CASCADE` | Foreign key to parent project |
| `version` | `VARCHAR(50)` | `NOT NULL` | Release version or Git commit tag |
| `status` | `VARCHAR(20)` | `NOT NULL, CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED'))` | Deployment state |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL, DEFAULT CURRENT_TIMESTAMP` | Execution start timestamp |
| `completed_at` | `TIMESTAMPTZ` | `NULL` | Execution completion timestamp |

**Indexes:**
- `idx_deployments_project_id` on `project_id`
- `idx_deployments_status` on `status`
- `idx_deployments_created_at` on `created_at DESC`

---

## 3. Migration Strategy

- **Tool**: Flyway versioned migrations.
- **Location**: `database/migrations/` (repository copy) and `backend/src/main/resources/db/migration/` (runtime classpath).
- **Naming Rule**: `V<Version>__<Description>.sql` (e.g. `V1__init_schema.sql`).
- **Execution**: Automatically executed on Spring Boot startup before HikariCP exposes active connections to the application.
