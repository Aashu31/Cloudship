# ==============================================================================
# CloudShip — Local PostgreSQL Setup and Startup Script
# ==============================================================================
# Initializes and starts a local PostgreSQL cluster for CloudShip development
# without requiring root/administrative access or external Docker dependencies.
# ==============================================================================

$ErrorActionPreference = "Stop"

$PG_DATA = "$env:USERPROFILE\.cloudship_pgdata"
$PG_PORT = 5433
$PG_USER = "cloudship"
$PG_DB   = "cloudship_dev"

# Locate PostgreSQL binaries
$PG_BIN = "C:\Program Files\PostgreSQL\18\bin"
if (-not (Test-Path "$PG_BIN\initdb.exe")) {
    $PG_BIN = (Get-Command initdb.exe -ErrorAction SilentlyContinue).Source | Split-Path
}

if (-not $PG_BIN) {
    Write-Error "PostgreSQL binaries not found. Please install PostgreSQL or add to PATH."
    exit 1
}

Write-Host "Using PostgreSQL Binaries at: $PG_BIN"

# 1. Initialize Cluster if not exists
if (-not (Test-Path "$PG_DATA\PG_VERSION")) {
    Write-Host "Initializing local PostgreSQL cluster at $PG_DATA..."
    & "$PG_BIN\initdb.exe" -D "$PG_DATA" -U $PG_USER --auth-local=trust --auth-host=trust --encoding=UTF8
    Add-Content -Path "$PG_DATA\postgresql.conf" -Value "`nport = $PG_PORT`n"
    Write-Host "Database cluster initialized."
}

# 2. Check if server is already running
$status = & "$PG_BIN\pg_ctl.exe" -D "$PG_DATA" status 2>&1
if ($status -match "server is running") {
    Write-Host "PostgreSQL server is already running on port $PG_PORT."
} else {
    Write-Host "Starting PostgreSQL server on port $PG_PORT..."
    & "$PG_BIN\pg_ctl.exe" -D "$PG_DATA" -l "$PG_DATA\server.log" start
    Start-Sleep -Seconds 2
}

# 3. Create database if it doesn't exist
$dbExists = & "$PG_BIN\psql.exe" -h localhost -p $PG_PORT -U $PG_USER -lqt | Select-String "\b$PG_DB\b"
if (-not $dbExists) {
    Write-Host "Creating database '$PG_DB'..."
    & "$PG_BIN\createdb.exe" -h localhost -p $PG_PORT -U $PG_USER $PG_DB
    Write-Host "Database '$PG_DB' created successfully."
} else {
    Write-Host "Database '$PG_DB' is ready."
}

Write-Host "============================================================"
Write-Host " Local PostgreSQL is ready for CloudShip development!"
Write-Host " Host:     localhost"
Write-Host " Port:     $PG_PORT"
Write-Host " Database: $PG_DB"
Write-Host " User:     $PG_USER"
Write-Host "============================================================"
