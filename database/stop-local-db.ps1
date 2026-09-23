# ==============================================================================
# CloudShip — Local PostgreSQL Stop Script
# ==============================================================================
# Safely stops the local PostgreSQL server running on port 5433 for CloudShip.
# ==============================================================================

$ErrorActionPreference = "SilentlyContinue"

$PG_DATA = "$env:USERPROFILE\.cloudship_pgdata"
$PG_PORT = 5433

$PG_BIN = "C:\Program Files\PostgreSQL\18\bin"
if (-not (Test-Path "$PG_BIN\pg_ctl.exe")) {
    $PG_BIN = (Get-Command pg_ctl.exe -ErrorAction SilentlyContinue).Source | Split-Path
}

if (-not $PG_BIN) {
    Write-Error "PostgreSQL binaries not found."
    exit 1
}

$status = & "$PG_BIN\pg_ctl.exe" -D "$PG_DATA" status 2>&1
if ($status -match "server is running") {
    Write-Host "Stopping PostgreSQL server on port $PG_PORT..."
    & "$PG_BIN\pg_ctl.exe" -D "$PG_DATA" -m fast stop
    Write-Host "PostgreSQL server stopped successfully."
} else {
    Write-Host "PostgreSQL server is not currently running."
}
