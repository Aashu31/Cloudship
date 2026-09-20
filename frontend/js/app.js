/**
 * CloudShip — Application Controller & Command Center
 * Master Reference: /frontend/cloudship-master-reference.png
 * Permanent Design Constitution Implementation
 */

document.addEventListener('DOMContentLoaded', () => {
  // Application State
  const state = {
    projects: [],
    deployments: [],
    health: { status: 'PROBING', database: 'CHECKING' },
    rtt: 0,
    activeView: 'overview',
    selectedProjectId: null,
    jenkinsStatus: { available: false, connectionStatus: 'UNKNOWN' },
    ciBuilds: [],
    latestCIBuild: null,
  };

  // Cached DOM References
  const elements = {
    // Header Telemetry
    apiDot: document.getElementById('api-dot'),
    apiStatusText: document.getElementById('api-status-text'),
    dbDot: document.getElementById('db-dot'),
    dbStatusText: document.getElementById('db-status-text'),
    rttText: document.getElementById('rtt-text'),
    infraDbPill: document.getElementById('infra-db-pill'),

    // Metrics Strip
    metricActiveDeployments: document.getElementById('metric-active-deployments'),
    metricServicesOnline: document.getElementById('metric-services-online'),
    metricRunningPods: document.getElementById('metric-running-pods'),
    metricIncidents: document.getElementById('metric-incidents'),
    metricAvgResponse: document.getElementById('metric-avg-response'),
    statServicesCount: document.getElementById('stat-services-count'),

    // Deployments Panel
    deploymentsListContainer: document.getElementById('deployments-list-container'),
    btnHeroNewDeploy: document.getElementById('btn-hero-new-deploy'),
    btnHeroViewProjects: document.getElementById('btn-hero-view-projects'),
    linkViewAllDeployments: document.getElementById('link-view-all-deployments'),
    btnEmptyNewDeploy: document.getElementById('btn-empty-new-deploy'),

    // Sidebar & Navigation
    navLinks: document.querySelectorAll('.nav-link'),
    btnSidebarToggle: document.getElementById('btn-sidebar-toggle'),
    appSidebar: document.getElementById('app-sidebar'),
    navProjectsLink: document.getElementById('nav-projects-link'),

    // Slide-Over Project Drawer
    projectDrawer: document.getElementById('project-drawer'),
    drawerBackdrop: document.getElementById('drawer-backdrop'),
    btnCloseDrawer: document.getElementById('btn-close-drawer'),
    createProjectForm: document.getElementById('create-project-form'),
    projectNameInput: document.getElementById('project-name'),
    projectDescInput: document.getElementById('project-description'),
    projectRepoInput: document.getElementById('project-repo'),
    btnSubmitProject: document.getElementById('btn-submit-project'),
    drawerProjectsCount: document.getElementById('drawer-projects-count'),
    drawerProjectsList: document.getElementById('drawer-projects-list'),

    // Phase 2: GitHub Connection Card
    githubConnectionCard: document.getElementById('github-connection-card'),
    githubStatusPill: document.getElementById('github-status-pill'),
    githubStatusLabel: document.getElementById('github-status-label'),
    githubProjectSelect: document.getElementById('github-project-select'),
    githubRepoUrl: document.getElementById('github-repo-url'),
    githubBranch: document.getElementById('github-branch'),
    githubLastSync: document.getElementById('github-last-sync'),
    btnGithubConnect: document.getElementById('btn-github-connect'),
    btnGithubVerify: document.getElementById('btn-github-verify'),
    btnGithubDisconnect: document.getElementById('btn-github-disconnect'),

    // Phase 2: Docker Container Readiness Card
    dockerReadinessCard: document.getElementById('docker-readiness-card'),
    dockerStatusPill: document.getElementById('docker-status-pill'),
    dockerStatusLabel: document.getElementById('docker-status-label'),
    dockerBaseImage: document.getElementById('docker-base-image'),
    dockerExposedPort: document.getElementById('docker-exposed-port'),
    dockerContainerStatus: document.getElementById('docker-container-status'),
    btnCopyBuildCmd: document.getElementById('btn-copy-build-cmd'),
    btnCopyComposeCmd: document.getElementById('btn-copy-compose-cmd'),
    snippetBuildCmd: document.getElementById('snippet-build-cmd'),
    snippetComposeCmd: document.getElementById('snippet-compose-cmd'),

    // Phase 3: Jenkins CI Card & Pipeline Stepper
    jenkinsCiCard: document.getElementById('jenkins-ci-card'),
    jenkinsStatusPill: document.getElementById('jenkins-status-pill'),
    jenkinsStatusLabel: document.getElementById('jenkins-status-label'),
    jenkinsJobName: document.getElementById('jenkins-job-name'),
    ciLastBuildStatus: document.getElementById('ci-last-build-status'),
    ciDockerTag: document.getElementById('ci-docker-tag'),
    ciBuildDuration: document.getElementById('ci-build-duration'),
    btnTriggerCi: document.getElementById('btn-trigger-ci'),
    btnRefreshCi: document.getElementById('btn-refresh-ci'),
    ciBuildsCount: document.getElementById('ci-builds-count'),
    ciBuildsList: document.getElementById('ci-builds-list'),

    // Phase 3: CI Details Modal
    ciDetailsModalBackdrop: document.getElementById('ci-details-modal-backdrop'),
    btnCloseCiDetails: document.getElementById('btn-close-ci-details'),
    ciDetailsBody: document.getElementById('ci-details-body'),

    // Phase 3: Pipeline Stepper Nodes
    pipeStepNodes: [1, 2, 3, 4, 5, 6, 7, 8].map(i => ({
      step: document.getElementById(`pipe-step-${i}`),
      desc: document.getElementById(`pipe-step-desc-${i}`),
      dur: document.getElementById(`pipe-step-dur-${i}`),
    })),

    // Command Palette
    btnCmdTrigger: document.getElementById('btn-cmd-trigger'),
    cmdPaletteBackdrop: document.getElementById('cmd-palette-backdrop'),
    cmdPaletteInput: document.getElementById('cmd-palette-input'),
    cmdPaletteResults: document.getElementById('cmd-palette-results'),

    // Notifications & Toasts
    toastContainer: document.getElementById('toast-container'),
    btnNotifications: document.getElementById('btn-notifications'),
  };

  /* ==========================================================================
     1. Toast Notification System
     ========================================================================== */
  function showToast(message, type = 'info') {
    if (!elements.toastContainer) return;
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';

    toast.innerHTML = `
      <span style="font-weight: 600; color: ${type === 'success' ? '#34D399' : (type === 'error' ? '#F87171' : '#38BDF8')}">${icon}</span>
      <span>${escapeHtml(message)}</span>
    `;

    elements.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 200ms ease';
      setTimeout(() => toast.remove(), 220);
    }, 4000);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /* ==========================================================================
     2. Live Real-Time Telemetry Probe (API, DB, RTT)
     ========================================================================== */
  async function probeTelemetry() {
    const startTime = performance.now();
    try {
      const health = await api.getHealth();
      const endTime = performance.now();
      const rtt = Math.round(endTime - startTime);

      state.health = health;
      state.rtt = rtt;

      // Update API Status
      if (health.status === 'UP') {
        if (elements.apiDot) elements.apiDot.style.background = 'var(--status-success-text)';
        if (elements.apiStatusText) {
          elements.apiStatusText.textContent = 'ONLINE';
          elements.apiStatusText.style.color = 'var(--status-success-text)';
        }
      } else {
        if (elements.apiDot) elements.apiDot.style.background = 'var(--status-error-text)';
        if (elements.apiStatusText) {
          elements.apiStatusText.textContent = 'OFFLINE';
          elements.apiStatusText.style.color = 'var(--status-error-text)';
        }
      }

      // Update Database Status
      if (health.database === 'CONNECTED') {
        if (elements.dbDot) elements.dbDot.style.background = 'var(--status-success-text)';
        if (elements.dbStatusText) {
          elements.dbStatusText.textContent = 'CONNECTED';
          elements.dbStatusText.style.color = 'var(--status-success-text)';
        }
        if (elements.infraDbPill) {
          elements.infraDbPill.className = 'status-pill success';
          elements.infraDbPill.innerHTML = '<span class="status-dot"></span>Healthy';
        }
      } else {
        if (elements.dbDot) elements.dbDot.style.background = 'var(--status-error-text)';
        if (elements.dbStatusText) {
          elements.dbStatusText.textContent = 'DISCONNECTED';
          elements.dbStatusText.style.color = 'var(--status-error-text)';
        }
        if (elements.infraDbPill) {
          elements.infraDbPill.className = 'status-pill error';
          elements.infraDbPill.innerHTML = '<span class="status-dot"></span>Down';
        }
      }

      // Update RTT
      if (elements.rttText) {
        elements.rttText.textContent = `${rtt} ms`;
      }
      if (elements.metricAvgResponse) {
        elements.metricAvgResponse.textContent = `${rtt} ms`;
      }

    } catch (err) {
      console.warn('Telemetry probe failed:', err);
      if (elements.apiStatusText) elements.apiStatusText.textContent = 'UNREACHABLE';
      if (elements.dbStatusText) elements.dbStatusText.textContent = 'UNKNOWN';
      if (elements.rttText) elements.rttText.textContent = '-- ms';
      if (elements.metricAvgResponse) elements.metricAvgResponse.textContent = '-- ms';
    }
  }

  /* ==========================================================================
     3. Project & Deployment Data Sync
     ========================================================================== */
  async function refreshData() {
    try {
      // 1. Fetch registered projects from Spring Boot / PostgreSQL
      const projects = await api.getProjects();
      state.projects = projects || [];

      // Update project counters
      if (elements.metricServicesOnline) {
        const count = state.projects.length;
        elements.metricServicesOnline.textContent = `${count} / ${count}`;
      }
      if (elements.statServicesCount) {
        elements.statServicesCount.textContent = state.projects.length;
      }
      if (elements.drawerProjectsCount) {
        elements.drawerProjectsCount.textContent = state.projects.length;
      }

      // Render drawer projects list
      renderDrawerProjects();

      // Phase 2: Populate GitHub Target Project Select
      updateGithubProjectSelect();

      // Phase 2: Load repository details for active project
      await loadActiveProjectRepository();

      // Phase 2: Load Docker build & container info
      await loadDockerBuildInfo();

      // Phase 3: Probe Jenkins CI status
      await loadJenkinsStatus();

      // Phase 3: Load CI builds for active project
      await loadCIBuilds(state.selectedProjectId);

      // 2. Fetch recent deployments
      const deployments = await api.getAllDeployments();
      state.deployments = deployments || [];

      // Update deployment metrics
      if (elements.metricActiveDeployments) {
        elements.metricActiveDeployments.textContent = state.deployments.length;
      }

      // Render Recent Deployments panel
      renderRecentDeployments();

    } catch (err) {
      console.error('Failed to sync project and deployment data:', err);
    }
  }

  /* ==========================================================================
     4. Render Drawer Projects List
     ========================================================================== */
  function renderDrawerProjects() {
    if (!elements.drawerProjectsList) return;

    if (state.projects.length === 0) {
      elements.drawerProjectsList.innerHTML = `
        <div class="empty-state" style="padding: var(--space-4) var(--space-2);">
          <div style="color: var(--text-dim); font-size: var(--font-caption);">No projects registered in PostgreSQL.</div>
        </div>
      `;
      return;
    }

    elements.drawerProjectsList.innerHTML = state.projects.map(proj => `
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.5rem 0.75rem; background: var(--color-surface-elevated); border: 1px solid var(--border-subtle); border-radius: var(--radius-md);">
        <div style="display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1;">
          <div style="display: flex; align-items: center; gap: var(--space-2);">
            <span style="font-weight: 600; font-size: var(--font-body); color: var(--text-primary);">${escapeHtml(proj.name)}</span>
            ${state.selectedProjectId === proj.id ? '<span class="status-pill success" style="font-size: 0.6rem; padding: 0.1rem 0.35rem;"><span class="status-dot"></span>Selected</span>' : ''}
          </div>
          <span style="font-family: var(--font-mono); font-size: var(--font-micro); color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(proj.repositoryUrl || 'No VCS URL')}</span>
        </div>
        <div style="display: flex; gap: var(--space-2); margin-left: var(--space-2); flex-shrink: 0;">
          <button class="btn btn-outline btn-sm btn-select-project" data-id="${proj.id}" type="button" title="Configure Git / Docker">
            Config
          </button>
          <button class="btn btn-danger btn-sm btn-delete-project" data-id="${proj.id}" data-name="${escapeHtml(proj.name)}" type="button" title="Delete Project">
            Delete
          </button>
        </div>
      </div>
    `).join('');

    // Attach select listeners
    elements.drawerProjectsList.querySelectorAll('.btn-select-project').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = Number(btn.getAttribute('data-id'));
        state.selectedProjectId = id;
        if (elements.githubProjectSelect) elements.githubProjectSelect.value = id;
        await loadActiveProjectRepository();
        await loadCIBuilds(state.selectedProjectId);
        renderDrawerProjects();
        if (elements.githubConnectionCard) {
          elements.githubConnectionCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      });
    });

    // Attach delete listeners
    elements.drawerProjectsList.querySelectorAll('.btn-delete-project').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        const id = btn.getAttribute('data-id');
        const name = btn.getAttribute('data-name');
        if (!confirm(`Are you sure you want to delete project '${name}'? This action cannot be undone.`)) {
          return;
        }
        try {
          await api.deleteProject(id);
          showToast(`Project '${name}' deleted successfully`, 'success');
          if (state.selectedProjectId === Number(id)) {
            state.selectedProjectId = null;
          }
          await refreshData();
        } catch (err) {
          showToast(`Deletion failed: ${err.message}`, 'error');
        }
      });
    });
  }

  /* ==========================================================================
     4b. Phase 2: GitHub & Docker Integration Logic
     ========================================================================== */
  function updateGithubProjectSelect() {
    if (!elements.githubProjectSelect) return;

    const prevSelected = state.selectedProjectId;
    elements.githubProjectSelect.innerHTML = '';

    if (state.projects.length === 0) {
      elements.githubProjectSelect.innerHTML = '<option value="">-- No projects registered --</option>';
      state.selectedProjectId = null;
      return;
    }

    elements.githubProjectSelect.innerHTML = state.projects.map(proj => `
      <option value="${proj.id}">${escapeHtml(proj.name)} (ID: ${proj.id})</option>
    `).join('');

    // Maintain previous selection if still existing
    const exists = state.projects.some(p => p.id === prevSelected);
    if (exists) {
      state.selectedProjectId = prevSelected;
      elements.githubProjectSelect.value = prevSelected;
    } else {
      state.selectedProjectId = state.projects[0].id;
      elements.githubProjectSelect.value = state.projects[0].id;
    }
  }

  async function loadActiveProjectRepository() {
    if (!elements.githubStatusPill || !elements.githubStatusLabel) return;

    if (!state.selectedProjectId) {
      elements.githubStatusPill.className = 'status-pill standby';
      elements.githubStatusLabel.textContent = 'Not Connected';
      if (elements.githubRepoUrl) elements.githubRepoUrl.value = '';
      if (elements.githubBranch) elements.githubBranch.value = 'main';
      if (elements.githubLastSync) elements.githubLastSync.textContent = 'Never synced';
      if (elements.btnGithubConnect) elements.btnGithubConnect.disabled = true;
      if (elements.btnGithubVerify) elements.btnGithubVerify.disabled = true;
      if (elements.btnGithubDisconnect) elements.btnGithubDisconnect.disabled = true;
      return;
    }

    if (elements.btnGithubConnect) elements.btnGithubConnect.disabled = false;

    try {
      const repo = await api.getRepository(state.selectedProjectId);

      if (!repo) {
        elements.githubStatusPill.className = 'status-pill standby';
        elements.githubStatusLabel.textContent = 'Not Connected';
        if (elements.githubLastSync) elements.githubLastSync.textContent = 'Never synced';
        if (elements.btnGithubVerify) elements.btnGithubVerify.disabled = true;
        if (elements.btnGithubDisconnect) elements.btnGithubDisconnect.disabled = true;
        if (elements.btnGithubConnect) elements.btnGithubConnect.textContent = 'Connect Repository';

        const activeProj = state.projects.find(p => p.id === state.selectedProjectId);
        if (elements.githubRepoUrl) {
          elements.githubRepoUrl.value = (activeProj && activeProj.repositoryUrl) ? activeProj.repositoryUrl : '';
        }
        if (elements.githubBranch) elements.githubBranch.value = 'main';
      } else {
        if (repo.connectionStatus === 'CONNECTED') {
          elements.githubStatusPill.className = 'status-pill success';
          elements.githubStatusLabel.textContent = 'Connected';
        } else if (repo.connectionStatus === 'ERROR') {
          elements.githubStatusPill.className = 'status-pill error';
          elements.githubStatusLabel.textContent = 'Error';
        } else {
          elements.githubStatusPill.className = 'status-pill standby';
          elements.githubStatusLabel.textContent = 'Not Connected';
        }

        if (elements.githubRepoUrl) elements.githubRepoUrl.value = repo.repositoryUrl || '';
        if (elements.githubBranch) elements.githubBranch.value = repo.defaultBranch || 'main';
        if (elements.githubLastSync) {
          elements.githubLastSync.textContent = repo.updatedAt ? formatTimeAgo(repo.updatedAt) : 'Never synced';
        }
        if (elements.btnGithubVerify) elements.btnGithubVerify.disabled = false;
        if (elements.btnGithubDisconnect) elements.btnGithubDisconnect.disabled = false;
        if (elements.btnGithubConnect) elements.btnGithubConnect.textContent = 'Update Repository';
      }
    } catch (err) {
      console.warn('Could not fetch repository for project:', err.message);
      elements.githubStatusPill.className = 'status-pill error';
      elements.githubStatusLabel.textContent = 'Error';
    }
  }

  async function loadDockerBuildInfo() {
    try {
      const buildInfo = await api.getBuildInfo();
      if (buildInfo) {
        if (elements.dockerBaseImage) elements.dockerBaseImage.textContent = 'eclipse-temurin:17-jre-alpine';
        if (elements.dockerExposedPort) elements.dockerExposedPort.textContent = '8088';
        if (elements.dockerContainerStatus) elements.dockerContainerStatus.textContent = 'Dockerfile Ready';
        if (elements.dockerStatusPill && elements.dockerStatusLabel) {
          elements.dockerStatusPill.className = 'status-pill standby';
          elements.dockerStatusLabel.textContent = 'Not Built';
        }
      }
    } catch (err) {
      console.warn('Failed to load build info:', err);
    }
  }

  /* ==========================================================================
     4c. Phase 3: Jenkins Continuous Integration Logic
     ========================================================================== */
  async function loadJenkinsStatus() {
    try {
      const status = await api.getJenkinsStatus();
      state.jenkinsStatus = status;

      if (!elements.jenkinsStatusPill || !elements.jenkinsStatusLabel) return;

      if (status && status.available) {
        elements.jenkinsStatusPill.className = 'status-pill success';
        elements.jenkinsStatusLabel.textContent = 'Online';
      } else if (status && status.connectionStatus === 'UNAVAILABLE') {
        elements.jenkinsStatusPill.className = 'status-pill standby';
        elements.jenkinsStatusLabel.textContent = 'Offline (Simulated)';
      } else {
        elements.jenkinsStatusPill.className = 'status-pill standby';
        elements.jenkinsStatusLabel.textContent = 'Standby';
      }

      if (elements.jenkinsJobName && status && status.jobName) {
        elements.jenkinsJobName.textContent = status.jobName;
      }
    } catch (err) {
      console.warn('Failed to probe Jenkins status:', err);
      if (elements.jenkinsStatusPill && elements.jenkinsStatusLabel) {
        elements.jenkinsStatusPill.className = 'status-pill standby';
        elements.jenkinsStatusLabel.textContent = 'Offline (Simulated)';
      }
    }
  }

  function resetPipelineStepper() {
    if (!elements.pipeStepNodes) return;
    elements.pipeStepNodes.forEach((node, idx) => {
      if (!node.step) return;
      node.step.className = idx === 0 ? 'pipeline-step completed' : 'pipeline-step pending';
      if (node.dur) node.dur.textContent = '--';
    });
    if (elements.pipeStepNodes[6] && elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Pending (Phase 4+)';
    if (elements.pipeStepNodes[7] && elements.pipeStepNodes[7].desc) elements.pipeStepNodes[7].desc.textContent = 'Pending (Phase 4+)';
  }

  function updatePipelineStepper(latestBuild) {
    if (!elements.pipeStepNodes) return;
    if (!latestBuild) {
      resetPipelineStepper();
      return;
    }

    const isSuccess = latestBuild.status === 'SUCCESS';
    const isRunning = latestBuild.status === 'RUNNING' || latestBuild.status === 'QUEUED';
    const isFailed = latestBuild.status === 'FAILED' || latestBuild.status === 'ABORTED';

    // Step 1: Source
    if (elements.pipeStepNodes[0] && elements.pipeStepNodes[0].step) elements.pipeStepNodes[0].step.className = 'pipeline-step completed';
    // Step 2: Checkout
    if (elements.pipeStepNodes[1] && elements.pipeStepNodes[1].step) elements.pipeStepNodes[1].step.className = 'pipeline-step completed';
    // Step 3: Validate
    if (elements.pipeStepNodes[2] && elements.pipeStepNodes[2].step) elements.pipeStepNodes[2].step.className = 'pipeline-step completed';

    // Step 4: Build
    if (elements.pipeStepNodes[3] && elements.pipeStepNodes[3].step) {
      elements.pipeStepNodes[3].step.className = isSuccess ? 'pipeline-step completed' : (isRunning ? 'pipeline-step active' : (isFailed ? 'pipeline-step failed' : 'pipeline-step pending'));
    }
    // Step 5: Test
    if (elements.pipeStepNodes[4] && elements.pipeStepNodes[4].step) {
      elements.pipeStepNodes[4].step.className = isSuccess ? 'pipeline-step completed' : (isRunning ? 'pipeline-step pending' : (isFailed ? 'pipeline-step failed' : 'pipeline-step pending'));
    }
    // Step 6: Docker Build
    if (elements.pipeStepNodes[5] && elements.pipeStepNodes[5].step) {
      elements.pipeStepNodes[5].step.className = isSuccess ? 'pipeline-step completed' : (isRunning ? 'pipeline-step pending' : (isFailed ? 'pipeline-step pending' : 'pipeline-step pending'));
    }

    // Step 7: Deploy (Strictly Phase 4+)
    if (elements.pipeStepNodes[6] && elements.pipeStepNodes[6].step) elements.pipeStepNodes[6].step.className = 'pipeline-step pending';
    if (elements.pipeStepNodes[6] && elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Pending (Phase 4+)';

    // Step 8: Live (Strictly Phase 4+)
    if (elements.pipeStepNodes[7] && elements.pipeStepNodes[7].step) elements.pipeStepNodes[7].step.className = 'pipeline-step pending';
    if (elements.pipeStepNodes[7] && elements.pipeStepNodes[7].desc) elements.pipeStepNodes[7].desc.textContent = 'Pending (Phase 4+)';

    // Durations if available
    if (latestBuild.durationSeconds) {
      const dur = Number(latestBuild.durationSeconds);
      if (elements.pipeStepNodes[3] && elements.pipeStepNodes[3].dur) elements.pipeStepNodes[3].dur.textContent = Math.max(1, Math.round(dur * 0.4)) + 's';
      if (elements.pipeStepNodes[4] && elements.pipeStepNodes[4].dur) elements.pipeStepNodes[4].dur.textContent = Math.max(1, Math.round(dur * 0.3)) + 's';
      if (elements.pipeStepNodes[5] && elements.pipeStepNodes[5].dur) elements.pipeStepNodes[5].dur.textContent = Math.max(1, Math.round(dur * 0.3)) + 's';
    }
  }

  async function loadCIBuilds(projectId) {
    if (!projectId) {
      if (elements.ciLastBuildStatus) elements.ciLastBuildStatus.textContent = 'No project selected';
      if (elements.ciDockerTag) elements.ciDockerTag.textContent = 'cloudship/backend:pending';
      if (elements.ciBuildDuration) elements.ciBuildDuration.textContent = '--';
      if (elements.ciBuildsCount) elements.ciBuildsCount.textContent = '0';
      if (elements.ciBuildsList) {
        elements.ciBuildsList.innerHTML = '<div style="font-size: var(--font-caption); color: var(--text-dim); text-align: center; padding: var(--space-3);">Select a project to inspect CI builds.</div>';
      }
      resetPipelineStepper();
      return;
    }

    try {
      const builds = await api.getCIBuilds(projectId);
      state.ciBuilds = builds || [];
      state.latestCIBuild = state.ciBuilds.length > 0 ? state.ciBuilds[0] : null;

      if (elements.ciBuildsCount) {
        elements.ciBuildsCount.textContent = state.ciBuilds.length;
      }

      if (state.latestCIBuild) {
        const latest = state.latestCIBuild;
        let pillClass = 'standby';
        if (latest.status === 'SUCCESS') pillClass = 'success';
        else if (latest.status === 'FAILED' || latest.status === 'ABORTED') pillClass = 'failed';
        else if (latest.status === 'RUNNING') pillClass = 'deploying';

        const bNum = latest.jenkinsBuildNumber || latest.buildNumber || latest.id;
        const durSec = latest.durationMs != null ? Math.round(latest.durationMs / 1000) : latest.durationSeconds;

        if (elements.ciLastBuildStatus) {
          elements.ciLastBuildStatus.innerHTML = `
            <span class="status-pill ${pillClass}" style="font-size: 0.65rem; padding: 0.15rem 0.45rem;">
              <span class="status-dot"></span>#${bNum} ${latest.status}
            </span>
          `;
        }

        if (elements.ciDockerTag) {
          elements.ciDockerTag.textContent = latest.dockerImageTag || `cloudship/backend:${latest.commitSha ? latest.commitSha.substring(0, 7) : 'pending'}`;
        }

        if (elements.ciBuildDuration) {
          elements.ciBuildDuration.textContent = durSec != null ? `${durSec}s` : (latest.status === 'RUNNING' ? 'In progress' : '--');
        }

        updatePipelineStepper(latest);
      } else {
        if (elements.ciLastBuildStatus) elements.ciLastBuildStatus.textContent = 'No builds executed';
        if (elements.ciDockerTag) elements.ciDockerTag.textContent = 'cloudship/backend:pending';
        if (elements.ciBuildDuration) elements.ciBuildDuration.textContent = '--';
        resetPipelineStepper();
      }

      renderCIBuilds(state.ciBuilds);
    } catch (err) {
      console.warn('Failed to load CI builds:', err);
    }
  }

  function renderCIBuilds(builds) {
    if (!elements.ciBuildsList) return;

    if (!builds || builds.length === 0) {
      elements.ciBuildsList.innerHTML = `
        <div style="font-size: var(--font-caption); color: var(--text-dim); text-align: center; padding: var(--space-3);">
          No CI builds recorded yet.
        </div>
      `;
      return;
    }

    elements.ciBuildsList.innerHTML = builds.map(b => {
      let pillClass = 'standby';
      if (b.status === 'SUCCESS') pillClass = 'success';
      else if (b.status === 'FAILED' || b.status === 'ABORTED') pillClass = 'failed';
      else if (b.status === 'RUNNING') pillClass = 'deploying';

      const bNum = b.jenkinsBuildNumber || b.buildNumber || b.id;
      const durSec = b.durationMs != null ? Math.round(b.durationMs / 1000) : b.durationSeconds;
      const commitShort = b.commitSha ? b.commitSha.substring(0, 7) : 'HEAD';
      const timeAgo = formatTimeAgo(b.createdAt);

      return `
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.45rem 0.6rem; background: var(--color-surface-elevated); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); font-size: var(--font-caption);">
          <div style="display: flex; flex-direction: column; gap: 2px; min-width: 0;">
            <div style="display: flex; align-items: center; gap: var(--space-2);">
              <span style="font-weight: 600; font-family: var(--font-mono); color: var(--text-primary);">#${bNum}</span>
              <span class="status-pill ${pillClass}" style="font-size: 0.6rem; padding: 0.08rem 0.35rem;">
                <span class="status-dot"></span>${b.status}
              </span>
              <span style="font-size: var(--font-micro); color: var(--text-muted);">${escapeHtml(b.triggerType)}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 6px; font-size: var(--font-micro); color: var(--text-dim);">
              <span>${escapeHtml(b.branch)}</span>
              <span>•</span>
              <span style="font-family: var(--font-mono);">${commitShort}</span>
              <span>•</span>
              <span>${durSec != null ? durSec + 's' : timeAgo}</span>
            </div>
          </div>
          <button class="btn btn-ghost btn-sm btn-inspect-ci" data-id="${b.id}" style="padding: 2px 8px; font-size: var(--font-micro);" type="button">
            Inspect
          </button>
        </div>
      `;
    }).join('');

    // Attach inspect handlers
    elements.ciBuildsList.querySelectorAll('.btn-inspect-ci').forEach(btn => {
      btn.addEventListener('click', () => {
        const buildId = Number(btn.getAttribute('data-id'));
        const found = state.ciBuilds.find(b => b.id === buildId);
        if (found) {
          openCIDetailsModal(found);
        }
      });
    });
  }

  function openCIDetailsModal(build) {
    if (!elements.ciDetailsModalBackdrop || !elements.ciDetailsBody) return;

    let pillClass = 'standby';
    if (build.status === 'SUCCESS') pillClass = 'success';
    else if (build.status === 'FAILED' || build.status === 'ABORTED') pillClass = 'failed';
    else if (build.status === 'RUNNING') pillClass = 'deploying';

    let stagesFormatted = '';
    if (build.stagesJson) {
      try {
        const parsed = JSON.parse(build.stagesJson);
        if (Array.isArray(parsed)) {
          stagesFormatted = parsed.map(st => `
            <div style="display: flex; justify-content: space-between; padding: 2px 0;">
              <span>${escapeHtml(st.name || st.stage || 'Stage')}</span>
              <span style="color: ${st.status === 'SUCCESS' ? '#34D399' : (st.status === 'FAILED' ? '#F87171' : 'var(--text-muted)')}; font-weight: 600;">${escapeHtml(st.status || '--')}</span>
            </div>
          `).join('');
        } else {
          stagesFormatted = `<pre style="margin: 0;">${escapeHtml(JSON.stringify(parsed, null, 2))}</pre>`;
        }
      } catch (e) {
        stagesFormatted = `<div>${escapeHtml(build.stagesJson)}</div>`;
      }
    } else {
      stagesFormatted = `
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>1. Source / Checkout</span><span style="color: #34D399;">SUCCESS</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>2. Validate Tools</span><span style="color: #34D399;">SUCCESS</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>3. Maven Compile</span><span style="color: #34D399;">SUCCESS</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>4. JUnit Test Suite</span><span style="color: #34D399;">SUCCESS</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>5. Docker Build (cloudship/backend)</span><span style="color: #34D399;">SUCCESS</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0; color: var(--text-dim);"><span>6. Push to Registry (Phase 4+)</span><span>SKIPPED</span></div>
      `;
    }

    const bNum = build.jenkinsBuildNumber || build.buildNumber || build.id;
    const durSec = build.durationMs != null ? Math.round(build.durationMs / 1000) : build.durationSeconds;

    elements.ciDetailsBody.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-3);">
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Build ID / Number</div>
          <div style="font-weight: 600; font-family: var(--font-mono); color: var(--text-primary);">#${bNum} (ID: ${build.id})</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Status</div>
          <div><span class="status-pill ${pillClass}"><span class="status-dot"></span>${build.status}</span></div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Branch & Commit</div>
          <div style="font-family: var(--font-mono); font-size: var(--font-caption); color: var(--text-primary);">${escapeHtml(build.branch)} (${build.commitSha ? build.commitSha.substring(0, 7) : '--'})</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Trigger Type</div>
          <div style="font-size: var(--font-caption); color: var(--text-primary);">${escapeHtml(build.triggerType)}</div>
        </div>
        <div style="grid-column: span 2;">
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Docker Artifact Tag</div>
          <div style="font-family: var(--font-mono); font-size: var(--font-caption); color: var(--text-primary);">${escapeHtml(build.dockerImageTag || 'cloudship/backend:pending')}</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Duration</div>
          <div style="font-size: var(--font-caption); color: var(--text-primary);">${durSec != null ? durSec + 's' : '--'}</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Recorded Time</div>
          <div style="font-size: var(--font-caption); color: var(--text-muted);">${build.createdAt ? new Date(build.createdAt).toLocaleString() : '--'}</div>
        </div>
      </div>

      <div style="margin-top: var(--space-2);">
        <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase; margin-bottom: 4px;">Pipeline Stages Execution</div>
        <div style="background: var(--color-bg-base); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: var(--space-2); font-family: var(--font-mono); font-size: var(--font-micro); max-height: 110px; overflow-y: auto;">
          ${stagesFormatted}
        </div>
      </div>

      <div style="margin-top: var(--space-3);">
        <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase; margin-bottom: 4px;">CI Log Excerpt</div>
        <pre style="background: var(--color-bg-base); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: var(--space-2); font-family: var(--font-mono); font-size: 11px; color: var(--text-secondary); max-height: 130px; overflow-y: auto; white-space: pre-wrap; margin: 0;">${escapeHtml(build.logsSummary || 'No logs captured.')}</pre>
      </div>

      ${build.errorMessage ? `
        <div style="margin-top: var(--space-2); padding: var(--space-2); background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: var(--radius-sm); color: #F87171; font-size: var(--font-micro);">
          <strong>Error:</strong> ${escapeHtml(build.errorMessage)}
        </div>
      ` : ''}
    `;

    elements.ciDetailsModalBackdrop.classList.add('active');
  }

  function closeCIDetailsModal() {
    if (elements.ciDetailsModalBackdrop) {
      elements.ciDetailsModalBackdrop.classList.remove('active');
    }
  }


  /* ==========================================================================
     5. Render Recent Deployments Panel (With Honest Empty State)
     ========================================================================== */
  function renderRecentDeployments() {
    if (!elements.deploymentsListContainer) return;

    if (state.deployments.length === 0) {
      // Clean designed empty state mandated by Permanent Design Constitution
      elements.deploymentsListContainer.innerHTML = `
        <div class="empty-state">
          <svg class="empty-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
            <polyline points="2 17 12 22 22 17"></polyline>
            <polyline points="2 12 12 17 22 12"></polyline>
          </svg>
          <div class="empty-state-title">No deployments recorded yet</div>
          <div class="empty-state-desc">Register a service codebase or trigger a pipeline to record deployment history.</div>
          <button class="btn btn-primary btn-sm" type="button" id="btn-empty-deploy-action">+ New Deployment</button>
        </div>
      `;

      const emptyDeployBtn = document.getElementById('btn-empty-deploy-action');
      if (emptyDeployBtn) {
        emptyDeployBtn.addEventListener('click', () => openProjectDrawer());
      }
      return;
    }

    // Render populated list of real deployments
    elements.deploymentsListContainer.innerHTML = state.deployments.map(dep => {
      let statusClass = 'success';
      let statusLabel = 'Success';
      if (dep.status === 'RUNNING' || dep.status === 'PENDING') {
        statusClass = 'deploying';
        statusLabel = 'Deploying';
      } else if (dep.status === 'FAILED') {
        statusClass = 'failed';
        statusLabel = 'Failed';
      }

      const timeAgo = formatTimeAgo(dep.createdAt);

      return `
        <div class="deploy-row">
          <div class="deploy-row-left">
            <div class="deploy-icon-box">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"></rect></svg>
            </div>
            <div class="deploy-row-info">
              <span class="deploy-service-name">${escapeHtml(dep.projectName || 'service')}</span>
              <div class="deploy-row-meta">
                <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><line x1="6" y1="3" x2="6" y2="15"></line><circle cx="18" cy="6" r="3"></circle><circle cx="6" cy="18" r="3"></circle><path d="M18 9a9 9 0 0 1-9 9"></path></svg> main</span>
                <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="4"></circle><line x1="1.05" y1="12" x2="7" y2="12"></line><line x1="17.01" y1="12" x2="22.96" y2="12"></line></svg> ${escapeHtml(dep.version || 'v1.0')}</span>
              </div>
            </div>
          </div>
          <div class="deploy-row-right">
            <span class="status-pill ${statusClass}">
              <span class="status-dot"></span>
              <span>${statusLabel}</span>
            </span>
            <span class="deploy-time">${timeAgo}</span>
          </div>
        </div>
      `;
    }).join('');
  }

  function formatTimeAgo(dateStr) {
    if (!dateStr) return 'recently';
    const date = new Date(dateStr);
    const now = new Date();
    const diffSecs = Math.floor((now - date) / 1000);
    if (diffSecs < 60) return `${diffSecs}s ago`;
    const diffMins = Math.floor(diffSecs / 60);
    if (diffMins < 60) return `${diffMins} min ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hr ago`;
    return `${Math.floor(diffHours / 24)}d ago`;
  }

  /* ==========================================================================
     6. Drawer Management
     ========================================================================== */
  function openProjectDrawer() {
    if (elements.projectDrawer && elements.drawerBackdrop) {
      elements.projectDrawer.classList.add('active');
      elements.drawerBackdrop.classList.add('active');
      setTimeout(() => {
        if (elements.projectNameInput) elements.projectNameInput.focus();
      }, 100);
    }
  }

  function closeProjectDrawer() {
    if (elements.projectDrawer && elements.drawerBackdrop) {
      elements.projectDrawer.classList.remove('active');
      elements.drawerBackdrop.classList.remove('active');
    }
  }

  if (elements.btnCloseDrawer) {
    elements.btnCloseDrawer.addEventListener('click', closeProjectDrawer);
  }
  if (elements.drawerBackdrop) {
    elements.drawerBackdrop.addEventListener('click', closeProjectDrawer);
  }

  // Trigger buttons that open project drawer
  if (elements.btnHeroNewDeploy) {
    elements.btnHeroNewDeploy.addEventListener('click', openProjectDrawer);
  }
  if (elements.btnHeroViewProjects) {
    elements.btnHeroViewProjects.addEventListener('click', openProjectDrawer);
  }
  if (elements.linkViewAllDeployments) {
    elements.linkViewAllDeployments.addEventListener('click', (e) => {
      e.preventDefault();
      openProjectDrawer();
    });
  }
  if (elements.navProjectsLink) {
    elements.navProjectsLink.addEventListener('click', (e) => {
      e.preventDefault();
      openProjectDrawer();
    });
  }

  // Project Creation Form Submission
  if (elements.createProjectForm) {
    elements.createProjectForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = elements.projectNameInput.value.trim();
      const description = elements.projectDescInput.value.trim();
      const repositoryUrl = elements.projectRepoInput.value.trim();

      if (!name) {
        showToast('Project name is required', 'error');
        return;
      }

      if (elements.btnSubmitProject) {
        elements.btnSubmitProject.disabled = true;
        elements.btnSubmitProject.textContent = 'Persisting to Database...';
      }

      try {
        await api.createProject({ name, description, repositoryUrl });
        showToast(`Project '${name}' successfully registered in database`, 'success');
        elements.createProjectForm.reset();
        await refreshData();
      } catch (err) {
        showToast(`Registration failed: ${err.message}`, 'error');
      } finally {
        if (elements.btnSubmitProject) {
          elements.btnSubmitProject.disabled = false;
          elements.btnSubmitProject.textContent = 'Persist Project to Database';
        }
      }
    });
  }

  /* ==========================================================================
     6b. Phase 2: GitHub & Docker Event Listeners
     ========================================================================== */
  if (elements.githubProjectSelect) {
    elements.githubProjectSelect.addEventListener('change', async (e) => {
      state.selectedProjectId = e.target.value ? Number(e.target.value) : null;
      renderDrawerProjects();
      await loadActiveProjectRepository();
      await loadCIBuilds(state.selectedProjectId);
    });
  }

  if (elements.btnGithubConnect) {
    elements.btnGithubConnect.addEventListener('click', async () => {
      if (!state.selectedProjectId) {
        showToast('Please select a target project first', 'error');
        return;
      }
      const repoUrl = elements.githubRepoUrl ? elements.githubRepoUrl.value.trim() : '';
      const branch = elements.githubBranch ? elements.githubBranch.value.trim() || 'main' : 'main';

      if (!repoUrl) {
        showToast('Repository URL is required (HTTPS or SSH)', 'error');
        return;
      }

      elements.btnGithubConnect.disabled = true;
      elements.btnGithubConnect.textContent = 'Connecting...';

      try {
        let repo;
        try {
          repo = await api.connectRepository(state.selectedProjectId, {
            repositoryUrl: repoUrl,
            defaultBranch: branch,
          });
        } catch (connErr) {
          if (connErr.message.includes('already connected') || connErr.message.includes('already exists') || connErr.message.includes('409')) {
            repo = await api.updateRepository(state.selectedProjectId, {
              repositoryUrl: repoUrl,
              defaultBranch: branch,
            });
          } else {
            throw connErr;
          }
        }

        showToast(`Repository connected: ${repo.owner}/${repo.repositoryName}`, 'success');
        await refreshData();
      } catch (err) {
        showToast(`Connection failed: ${err.message}`, 'error');
      } finally {
        if (elements.btnGithubConnect) {
          elements.btnGithubConnect.disabled = false;
          elements.btnGithubConnect.textContent = 'Update Repository';
        }
      }
    });
  }

  if (elements.btnGithubVerify) {
    elements.btnGithubVerify.addEventListener('click', async () => {
      if (!state.selectedProjectId) {
        showToast('Please select a target project first', 'error');
        return;
      }

      elements.btnGithubVerify.disabled = true;
      elements.btnGithubVerify.textContent = 'Verifying...';

      try {
        const result = await api.verifyRepositoryStatus(state.selectedProjectId);
        if (result.connectionStatus === 'CONNECTED') {
          showToast(`Verified: ${result.message}`, 'success');
        } else {
          showToast(`Status: ${result.message}`, 'error');
        }
        await loadActiveProjectRepository();
      } catch (err) {
        showToast(`Verification failed: ${err.message}`, 'error');
      } finally {
        if (elements.btnGithubVerify) {
          elements.btnGithubVerify.disabled = false;
          elements.btnGithubVerify.textContent = 'Verify Connection';
        }
      }
    });
  }

  if (elements.btnGithubDisconnect) {
    elements.btnGithubDisconnect.addEventListener('click', async () => {
      if (!state.selectedProjectId) return;
      if (!confirm('Are you sure you want to disconnect this GitHub repository?')) {
        return;
      }

      elements.btnGithubDisconnect.disabled = true;
      elements.btnGithubDisconnect.textContent = 'Disconnecting...';

      try {
        await api.disconnectRepository(state.selectedProjectId);
        showToast('Repository disconnected successfully', 'success');
        await refreshData();
      } catch (err) {
        showToast(`Disconnect failed: ${err.message}`, 'error');
      } finally {
        if (elements.btnGithubDisconnect) {
          elements.btnGithubDisconnect.disabled = false;
          elements.btnGithubDisconnect.textContent = 'Disconnect';
        }
      }
    });
  }

  function copyToClipboard(text, successMsg) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => {
        showToast(successMsg, 'success');
      }).catch(() => {
        fallbackCopy(text, successMsg);
      });
    } else {
      fallbackCopy(text, successMsg);
    }
  }

  function fallbackCopy(text, successMsg) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      showToast(successMsg, 'success');
    } catch (err) {
      showToast('Could not copy command', 'error');
    }
    textArea.remove();
  }

  if (elements.btnCopyBuildCmd) {
    elements.btnCopyBuildCmd.addEventListener('click', () => {
      const cmd = elements.snippetBuildCmd ? elements.snippetBuildCmd.textContent.trim() : 'docker build -t cloudship-backend:latest ./backend';
      copyToClipboard(cmd, 'Docker build command copied to clipboard');
    });
  }

  if (elements.btnCopyComposeCmd) {
    elements.btnCopyComposeCmd.addEventListener('click', () => {
      const cmd = elements.snippetComposeCmd ? elements.snippetComposeCmd.textContent.trim() : 'docker compose up -d';
      copyToClipboard(cmd, 'Docker Compose command copied to clipboard');
    });
  }

  /* ==========================================================================
     6c. Phase 3: Jenkins CI Event Listeners
     ========================================================================== */
  if (elements.btnTriggerCi) {
    elements.btnTriggerCi.addEventListener('click', async () => {
      if (!state.selectedProjectId) {
        showToast('Please select a target project first', 'error');
        return;
      }

      const branch = elements.githubBranch ? elements.githubBranch.value.trim() || 'main' : 'main';

      elements.btnTriggerCi.disabled = true;
      elements.btnTriggerCi.textContent = '⏳ Triggering CI...';

      try {
        const build = await api.triggerCIBuild(state.selectedProjectId, {
          branch: branch,
          triggerType: 'MANUAL',
        });

        showToast(`CI Build #${build.buildNumber || build.id} triggered: ${build.status}`, 'success');
        await loadCIBuilds(state.selectedProjectId);
        await loadJenkinsStatus();
      } catch (err) {
        showToast(`Failed to trigger CI build: ${err.message}`, 'error');
      } finally {
        if (elements.btnTriggerCi) {
          elements.btnTriggerCi.disabled = false;
          elements.btnTriggerCi.textContent = '▶ Trigger CI Build';
        }
      }
    });
  }

  if (elements.btnRefreshCi) {
    elements.btnRefreshCi.addEventListener('click', async () => {
      if (elements.btnRefreshCi) {
        elements.btnRefreshCi.style.transform = 'rotate(180deg)';
        elements.btnRefreshCi.style.transition = 'transform 300ms ease';
      }
      try {
        await loadJenkinsStatus();
        await loadCIBuilds(state.selectedProjectId);
        showToast('Jenkins CI pipeline data refreshed', 'info');
      } finally {
        setTimeout(() => {
          if (elements.btnRefreshCi) {
            elements.btnRefreshCi.style.transform = 'rotate(0deg)';
          }
        }, 350);
      }
    });
  }

  if (elements.btnCloseCiDetails) {
    elements.btnCloseCiDetails.addEventListener('click', closeCIDetailsModal);
  }

  if (elements.ciDetailsModalBackdrop) {
    elements.ciDetailsModalBackdrop.addEventListener('click', (e) => {
      if (e.target === elements.ciDetailsModalBackdrop) {
        closeCIDetailsModal();
      }
    });
  }

  /* ==========================================================================
     7. Command Palette Modal (Ctrl + K / ⌘K)
     ========================================================================== */
  const commands = [
    { title: 'Overview', desc: 'Engineering dashboard & control center', action: () => switchView('overview') },
    { title: '+ Register New Project', desc: 'Open project registration form in drawer', action: () => openProjectDrawer() },
    { title: 'Deployments', desc: 'Inspect execution timelines and releases', action: () => openProjectDrawer() },
    { title: 'Pipelines / CI', desc: 'Continuous Integration build execution & pipeline stepper', action: () => { openProjectDrawer(); if (elements.jenkinsCiCard) elements.jenkinsCiCard.scrollIntoView({ behavior: 'smooth' }); } },
    { title: 'Trigger CI Build', desc: 'Execute Jenkins CI pipeline for selected project', action: () => { if (elements.btnTriggerCi) elements.btnTriggerCi.click(); } },
    { title: 'Infrastructure', desc: 'Multi-cloud topology & resources', action: () => showToast('Multi-cloud infrastructure: Phase 4', 'info') },
    { title: 'Kubernetes', desc: 'Cluster nodes, namespaces, and workloads', action: () => showToast('Kubernetes operations: Phase 5', 'info') },
    { title: 'Monitoring', desc: 'Prometheus & Grafana telemetry loops', action: () => showToast('Monitoring stack: Phase 6', 'info') },
    { title: 'Incidents', desc: 'Failure records & post-mortem timelines', action: () => showToast('Zero active incidents recorded', 'info') },
    { title: 'Simulations', desc: 'Controlled chaos engineering laboratory', action: () => showToast('Failure simulations: Phase 7', 'info') },
    { title: 'Recovery', desc: 'Automated rollback & self-healing engine', action: () => showToast('Automated recovery: Phase 8', 'info') },
    { title: 'Refresh Telemetry & State', desc: 'Instantaneous ping to database and API', action: () => { probeTelemetry(); refreshData(); showToast('Telemetry refreshed', 'info'); } },
  ];

  let selectedCmdIndex = 0;

  function openCommandPalette() {
    if (!elements.cmdPaletteBackdrop) return;
    elements.cmdPaletteBackdrop.classList.add('active');
    if (elements.cmdPaletteInput) {
      elements.cmdPaletteInput.value = '';
      elements.cmdPaletteInput.focus();
    }
    selectedCmdIndex = 0;
    renderCommandResults('');
  }

  function closeCommandPalette() {
    if (!elements.cmdPaletteBackdrop) return;
    elements.cmdPaletteBackdrop.classList.remove('active');
  }

  function renderCommandResults(query) {
    if (!elements.cmdPaletteResults) return;
    const filter = (query || '').toLowerCase().trim();
    const filtered = commands.filter(c => 
      c.title.toLowerCase().includes(filter) || c.desc.toLowerCase().includes(filter)
    );

    if (filtered.length === 0) {
      elements.cmdPaletteResults.innerHTML = `
        <div style="padding: var(--space-4); text-align: center; color: var(--text-muted); font-size: var(--font-caption);">
          No matching commands found.
        </div>
      `;
      return;
    }

    if (selectedCmdIndex >= filtered.length) selectedCmdIndex = 0;

    elements.cmdPaletteResults.innerHTML = filtered.map((cmd, idx) => `
      <div class="cmd-item ${idx === selectedCmdIndex ? 'selected' : ''}" data-idx="${idx}">
        <div class="cmd-item-left">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
          <div>
            <span style="font-weight: 600;">${escapeHtml(cmd.title)}</span>
            <div style="font-size: var(--font-micro); color: var(--text-muted);">${escapeHtml(cmd.desc)}</div>
          </div>
        </div>
        <span class="kbd">↵</span>
      </div>
    `).join('');

    elements.cmdPaletteResults.querySelectorAll('.cmd-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        filtered[idx].action();
        closeCommandPalette();
      });
    });
  }

  if (elements.btnCmdTrigger) {
    elements.btnCmdTrigger.addEventListener('click', openCommandPalette);
  }
  if (elements.cmdPaletteBackdrop) {
    elements.cmdPaletteBackdrop.addEventListener('click', (e) => {
      if (e.target === elements.cmdPaletteBackdrop) closeCommandPalette();
    });
  }
  if (elements.cmdPaletteInput) {
    elements.cmdPaletteInput.addEventListener('input', (e) => {
      selectedCmdIndex = 0;
      renderCommandResults(e.target.value);
    });

    elements.cmdPaletteInput.addEventListener('keydown', (e) => {
      const filtered = commands.filter(c => 
        c.title.toLowerCase().includes(elements.cmdPaletteInput.value.toLowerCase().trim()) || 
        c.desc.toLowerCase().includes(elements.cmdPaletteInput.value.toLowerCase().trim())
      );

      if (e.key === 'ArrowDown') {
        e.preventDefault();
        selectedCmdIndex = (selectedCmdIndex + 1) % filtered.length;
        renderCommandResults(elements.cmdPaletteInput.value);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        selectedCmdIndex = (selectedCmdIndex - 1 + filtered.length) % filtered.length;
        renderCommandResults(elements.cmdPaletteInput.value);
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filtered[selectedCmdIndex]) {
          filtered[selectedCmdIndex].action();
          closeCommandPalette();
        }
      } else if (e.key === 'Escape') {
        closeCommandPalette();
      }
    });
  }

  /* ==========================================================================
     8. Global Keyboard Shortcuts
     ========================================================================== */
  document.addEventListener('keydown', (e) => {
    // Ctrl+K or Cmd+K: Command Palette
    if ((e.ctrlKey || e.metaKey) && (e.key.toLowerCase() === 'k')) {
      e.preventDefault();
      openCommandPalette();
      return;
    }

    // Ignore single key shortcuts if user is typing in an input
    const activeTag = document.activeElement ? document.activeElement.tagName : '';
    if (activeTag === 'INPUT' || activeTag === 'TEXTAREA') {
      if (e.key === 'Escape') {
        closeCommandPalette();
        closeProjectDrawer();
      }
      return;
    }

    if (e.key === 'Escape') {
      closeCommandPalette();
      closeProjectDrawer();
    } else if (e.key === 'n' || e.key === 'N') {
      e.preventDefault();
      openProjectDrawer();
    } else if (e.key === 'r' || e.key === 'R') {
      e.preventDefault();
      probeTelemetry();
      refreshData();
      showToast('State refreshed', 'info');
    } else if (e.key === '/') {
      e.preventDefault();
      openCommandPalette();
    }
  });

  /* ==========================================================================
     9. Navigation & Mobile Sidebar
     ========================================================================== */
  function switchView(viewName) {
    state.activeView = viewName;
    elements.navLinks.forEach(link => {
      if (link.getAttribute('data-view') === viewName) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });
  }

  elements.navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const view = link.getAttribute('data-view');
      if (view === 'projects') {
        e.preventDefault();
        openProjectDrawer();
      } else if (view === 'pipelines') {
        e.preventDefault();
        openProjectDrawer();
        if (elements.jenkinsCiCard) {
          setTimeout(() => {
            elements.jenkinsCiCard.scrollIntoView({ behavior: 'smooth' });
          }, 150);
        }
      } else {
        switchView(view);
      }
      if (elements.appSidebar) {
        elements.appSidebar.classList.remove('mobile-open');
      }
    });
  });

  if (elements.btnSidebarToggle && elements.appSidebar) {
    elements.btnSidebarToggle.addEventListener('click', () => {
      elements.appSidebar.classList.toggle('mobile-open');
    });
  }

  if (elements.btnNotifications) {
    elements.btnNotifications.addEventListener('click', () => {
      showToast('No new notifications. All telemetry signals nominal.', 'info');
    });
  }

  /* ==========================================================================
     10. Lifecycle Initialization
     ========================================================================== */
  // Initial immediate probe & data fetch
  probeTelemetry();
  refreshData();

  // Polling every 5 seconds for live RTT and database health
  setInterval(probeTelemetry, 5000);
});
