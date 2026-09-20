# CloudShip — Database Persistence & Migrations

## Overview
This directory manages database schema definitions, initialization scripts, and Flyway versioned migrations for the CloudShip PostgreSQL database.

## Technology Stack
- **Database**: PostgreSQL 15+
- **Migration Tool**: Flyway (`flyway-core`)
- **Connection Pool**: HikariCP (configured inside Spring Boot backend)

## Directory Structure
```text
database/
├── migrations/
│   ├── V1__init_schema.sql         # Initial tables: deployments, incidents, health_snapshots
│   └── V2__add_audit_indexes.sql   # Performance and search indexes
├── docker/
│   └── init-db.sql                 # Optional local dev container database bootstrap
└── README.md
```

## Migration Naming Convention
Flyway scripts must follow the strict versioning standard:
`V<Version>__<Description>.sql` (e.g., `V1__init_schema.sql`).

## Phase Status
- **Current State**: Phase 0 Scaffold.
- **Next Action**: Implement initial schema DDL in **Phase 1**.
