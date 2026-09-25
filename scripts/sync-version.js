/**
 * CloudShip — Canonical Version Synchronization Engine
 * Single Source of Truth: /VERSION
 *
 * Reads canonical semver from /VERSION and synchronizes:
 * - README.md (marked section)
 * - frontend/version.json
 * - frontend/js/version.js
 * - frontend/index.html (default placeholder)
 * - package.json
 * - backend/pom.xml
 * - backend/src/main/resources/application.yml
 *
 * Usage:
 *   node scripts/sync-version.js         (Sync all consumers)
 *   node scripts/sync-version.js --check (Verify all consumers match /VERSION)
 */

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');
const VERSION_FILE = path.join(ROOT_DIR, 'VERSION');
const README_FILE = path.join(ROOT_DIR, 'README.md');
const PACKAGE_JSON = path.join(ROOT_DIR, 'package.json');
const FRONTEND_VERSION_JSON = path.join(ROOT_DIR, 'frontend', 'version.json');
const FRONTEND_VERSION_JS = path.join(ROOT_DIR, 'frontend', 'js', 'version.js');
const FRONTEND_INDEX_HTML = path.join(ROOT_DIR, 'frontend', 'index.html');
const POM_XML = path.join(ROOT_DIR, 'backend', 'pom.xml');
const APP_YML = path.join(ROOT_DIR, 'backend', 'src', 'main', 'resources', 'application.yml');

// 1. Read Canonical Version
if (!fs.existsSync(VERSION_FILE)) {
  console.error(`[ERROR] Canonical VERSION file not found at: ${VERSION_FILE}`);
  process.exit(1);
}

const rawVersion = fs.readFileSync(VERSION_FILE, 'utf8').trim();
const semverRegex = /^\d+\.\d+\.\d+$/;

if (!semverRegex.test(rawVersion)) {
  console.error(`[ERROR] Invalid version format in VERSION: "${rawVersion}". Expected MAJOR.MINOR.PATCH (e.g. 8.0.0)`);
  process.exit(1);
}

const version = rawVersion;
const displayVersion = `v${version}`;
const majorVersion = version.split('.')[0];
const phaseName = `Version ${majorVersion} — Observability, Monitoring & Zero-Trust Control`;
const isCheckMode = process.argv.includes('--check');

console.log(`[CloudShip Version Engine] Canonical Version: ${version} (${displayVersion})`);
let hasMismatch = false;

function checkOrUpdate(filePath, currentContent, newContent, description) {
  if (currentContent === newContent) {
    console.log(`  [OK] ${description} is up to date (${version})`);
    return;
  }
  if (isCheckMode) {
    console.error(`  [MISMATCH] ${description} is out of sync with VERSION (${version})`);
    hasMismatch = true;
  } else {
    fs.writeFileSync(filePath, newContent, 'utf8');
    console.log(`  [UPDATED] ${description} -> ${version}`);
  }
}

// 2. Sync package.json
if (fs.existsSync(PACKAGE_JSON)) {
  const pkgContent = fs.readFileSync(PACKAGE_JSON, 'utf8');
  try {
    const pkg = JSON.parse(pkgContent);
    const oldVer = pkg.version;
    pkg.version = version;
    const newPkgContent = JSON.stringify(pkg, null, 2) + '\n';
    checkOrUpdate(PACKAGE_JSON, pkgContent, newPkgContent, 'package.json');
  } catch (err) {
    console.warn(`  [WARN] Failed to parse package.json: ${err.message}`);
  }
}

// 3. Sync frontend/version.json
const newVersionJson = JSON.stringify({
  version: version,
  displayVersion: displayVersion,
  name: "CloudShip",
  phase: phaseName,
  updatedAt: new Date().toISOString()
}, null, 2) + '\n';

const currVersionJson = fs.existsSync(FRONTEND_VERSION_JSON) ? fs.readFileSync(FRONTEND_VERSION_JSON, 'utf8') : '';
// Compare without updatedAt timestamp for stability
let jsonNeedsUpdate = true;
try {
  const parsed = JSON.parse(currVersionJson);
  if (parsed.version === version && parsed.displayVersion === displayVersion) {
    jsonNeedsUpdate = false;
  }
} catch (e) {}

if (jsonNeedsUpdate) {
  checkOrUpdate(FRONTEND_VERSION_JSON, currVersionJson, newVersionJson, 'frontend/version.json');
} else {
  console.log(`  [OK] frontend/version.json is up to date (${version})`);
}

// 4. Sync frontend/js/version.js
const newVersionJs = `/**
 * CloudShip — Canonical Version Metadata
 * AUTO-GENERATED from /VERSION. DO NOT EDIT DIRECTLY.
 * Run 'npm run version:sync' or 'node scripts/sync-version.js' to update.
 */
window.__CLOUDSHIP_VERSION__ = {
  version: "${version}",
  displayVersion: "${displayVersion}",
  name: "CloudShip",
  phase: "${phaseName}"
};
`;

const currVersionJs = fs.existsSync(FRONTEND_VERSION_JS) ? fs.readFileSync(FRONTEND_VERSION_JS, 'utf8') : '';
checkOrUpdate(FRONTEND_VERSION_JS, currVersionJs, newVersionJs, 'frontend/js/version.js');

// 5. Sync README.md marker
if (fs.existsSync(README_FILE)) {
  const readmeContent = fs.readFileSync(README_FILE, 'utf8');
  const markerRegex = /<!-- CLOUDSHIP_CURRENT_VERSION_START -->[\s\S]*?<!-- CLOUDSHIP_CURRENT_VERSION_END -->/;
  
  const replacementMarker = `<!-- CLOUDSHIP_CURRENT_VERSION_START -->
[![CloudShip Version](https://img.shields.io/badge/Version-${displayVersion}-amber.svg)](VERSION)
[![Phase](https://img.shields.io/badge/Phase-Version%20${majorVersion}%20(Observability%20%26%20Zero--Trust)-success.svg)](docs/observability.md)
[![Status](https://img.shields.io/badge/Status-Operational-brightgreen.svg)](https://cloudship-ten.vercel.app/)

**Current Version:** \`${displayVersion}\` (${phaseName})
<!-- CLOUDSHIP_CURRENT_VERSION_END -->`;

  if (markerRegex.test(readmeContent)) {
    const newReadme = readmeContent.replace(markerRegex, replacementMarker);
    checkOrUpdate(README_FILE, readmeContent, newReadme, 'README.md (marked section)');
  } else {
    // Insert marker below title
    const lines = readmeContent.split('\n');
    let insertIdx = 1;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].startsWith('# ')) {
        insertIdx = i + 1;
        break;
      }
    }
    lines.splice(insertIdx, 0, '\n' + replacementMarker + '\n');
    const newReadme = lines.join('\n');
    checkOrUpdate(README_FILE, readmeContent, newReadme, 'README.md (inserted marked section)');
  }
}

// 6. Sync backend/pom.xml
if (fs.existsSync(POM_XML)) {
  const pomContent = fs.readFileSync(POM_XML, 'utf8');
  // Target the root project version, immediately after artifactId cloudship-backend
  const pomRegex = /(<artifactId>cloudship-backend<\/artifactId>\s*<version>)[^<]+(<\/version>)/;
  if (pomRegex.test(pomContent)) {
    const newPom = pomContent.replace(pomRegex, `$1${version}$2`);
    checkOrUpdate(POM_XML, pomContent, newPom, 'backend/pom.xml');
  }
}

// 7. Sync backend/src/main/resources/application.yml
if (fs.existsSync(APP_YML)) {
  const ymlContent = fs.readFileSync(APP_YML, 'utf8');
  const ymlRegex = /(version:\s*\$\{APP_VERSION:)[^}]+(\})/;
  if (ymlRegex.test(ymlContent)) {
    const newYml = ymlContent.replace(ymlRegex, `$1${version}$2`);
    checkOrUpdate(APP_YML, ymlContent, newYml, 'backend application.yml (cloudship.version)');
  } else if (/cloudship:/.test(ymlContent)) {
    // Add version under cloudship:
    const newYml = ymlContent.replace(/cloudship:\n/, `cloudship:\n  version: \${APP_VERSION:${version}}\n`);
    checkOrUpdate(APP_YML, ymlContent, newYml, 'backend application.yml (added cloudship.version)');
  }
}

// 8. Sync frontend/index.html default placeholder
if (fs.existsSync(FRONTEND_INDEX_HTML)) {
  let htmlContent = fs.readFileSync(FRONTEND_INDEX_HTML, 'utf8');
  let modified = false;

  // Sidebar teaser version badge: <span ... id="sidebar-version-badge">...</span>
  const sidebarRegex = /(<span[^>]*id="sidebar-version-badge"[^>]*>)[^<]*(<\/span>)/;
  if (sidebarRegex.test(htmlContent)) {
    const updated = htmlContent.replace(sidebarRegex, `$1${displayVersion}$2`);
    if (updated !== htmlContent) {
      htmlContent = updated;
      modified = true;
    }
  }

  // Cockpit kicker: <div class="cockpit-kicker" id="cockpit-kicker-version">...</div>
  const kickerRegex = /(<div[^>]*id="cockpit-kicker-version"[^>]*>)[^<]*(<\/div>)/;
  if (kickerRegex.test(htmlContent)) {
    const updated = htmlContent.replace(kickerRegex, `$1ENGINEERING CONTROL ENVIRONMENT // OS ${displayVersion}$2`);
    if (updated !== htmlContent) {
      htmlContent = updated;
      modified = true;
    }
  }

  if (modified) {
    checkOrUpdate(FRONTEND_INDEX_HTML, fs.readFileSync(FRONTEND_INDEX_HTML, 'utf8'), htmlContent, 'frontend/index.html placeholders');
  } else {
    console.log(`  [OK] frontend/index.html placeholders are up to date`);
  }
}

if (isCheckMode && hasMismatch) {
  console.error('\n[CHECK FAILED] One or more version consumers are out of sync. Run "node scripts/sync-version.js" to fix.');
  process.exit(1);
} else {
  console.log('\n[SUCCESS] CloudShip version synchronization complete!');
}
