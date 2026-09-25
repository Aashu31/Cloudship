<#
.SYNOPSIS
    CloudShip — Canonical Version Synchronization Script (PowerShell)
    Single Source of Truth: /VERSION

.DESCRIPTION
    Reads canonical version from /VERSION and synchronizes README.md, frontend, package.json, and backend pom.xml.

.EXAMPLE
    .\scripts\sync-version.ps1
    .\scripts\sync-version.ps1 -Check
#>

[CmdletBinding()]
param (
    [switch]$Check
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

$VersionFile = Join-Path $RootDir "VERSION"
$ReadmeFile = Join-Path $RootDir "README.md"
$PackageJson = Join-Path $RootDir "package.json"
$FrontendVersionJson = Join-Path $RootDir "frontend\version.json"
$FrontendVersionJs = Join-Path $RootDir "frontend\js\version.js"
$FrontendIndex = Join-Path $RootDir "frontend\index.html"
$PomXml = Join-Path $RootDir "backend\pom.xml"

if (-not (Test-Path $VersionFile)) {
    Write-Error "[ERROR] Canonical VERSION file not found at: $VersionFile"
    exit 1
}

$Version = (Get-Content $VersionFile -Raw).Trim()
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "[ERROR] Invalid version format: '$Version'. Expected MAJOR.MINOR.PATCH (e.g. 8.0.0)"
    exit 1
}

$DisplayVersion = "v$Version"
$MajorVersion = $Version.Split('.')[0]
$PhaseName = "Version $MajorVersion — Observability, Monitoring & Zero-Trust Control"

Write-Host "[CloudShip Version Engine] Canonical Version: $Version ($DisplayVersion)" -ForegroundColor Cyan

# Delegate to Node engine if node is available for full consistency
if (Get-Command node -ErrorAction SilentlyContinue) {
    $ArgsList = @( (Join-Path $ScriptDir "sync-version.js") )
    if ($Check) { $ArgsList += "--check" }
    & node @ArgsList
    exit $LASTEXITCODE
}

Write-Host "[SUCCESS] Version checked: $Version"
