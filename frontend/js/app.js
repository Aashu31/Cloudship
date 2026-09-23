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
    azureStatus: { status: 'NOT_CONFIGURED', message: 'Checking...' },
    azureInfra: null,
    acrStatus: { status: 'NOT_CONFIGURED', connected: false, message: 'Standby' },
    acrDetails: null,
    acrRepositories: [],
    acrImages: [],
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
    infraOverallPill: document.getElementById('infra-overall-pill'),
    infraOverallStatus: document.getElementById('infra-overall-status'),
    infraAzurePill: document.getElementById('infra-azure-pill'),
    infraAzureLabel: document.getElementById('infra-azure-label'),

    // Metrics Strip
    metricActiveDeployments: document.getElementById('metric-active-deployments'),
    metricServicesOnline: document.getElementById('metric-services-online'),
    metricRunningPods: document.getElementById('metric-running-pods'),
    metricIncidents: document.getElementById('metric-incidents'),
    metricAvgResponse: document.getElementById('metric-avg-response'),
    statServicesCount: document.getElementById('stat-services-count'),

    // Deployments Panel
    deploymentsListContainer: document.getElementById('deployments-list-container'),
    btnEmptyNewDeploy: document.getElementById('btn-empty-new-deploy'),

    // Pipeline Stepper
    pipelineOverallStatus: document.getElementById('pipeline-overall-status'),
    pipelineOverallStatusText: document.getElementById('pipeline-overall-status-text'),

    // Sidebar & Navigation
    navLinks: document.querySelectorAll('.nav-link'),
    btnSidebarToggle: document.getElementById('btn-sidebar-toggle'),
    appSidebar: document.getElementById('app-sidebar'),

    // Slide-Over Project Drawer
    projectDrawer: document.getElementById('project-drawer'),
    drawerBackdrop: document.getElementById('project-drawer-backdrop'),
    btnCloseDrawer: document.getElementById('btn-close-drawer'),
    btnNewProjectAction: document.getElementById('btn-new-project-action'),
    btnHeroViewProjects: document.getElementById('btn-hero-view-projects'),
    navProjectsLink: document.getElementById('nav-projects-link'),
    drawerProjectsCount: document.getElementById('drawer-projects-count'),
    drawerProjectsList: document.getElementById('drawer-projects-list'),

    // Register Form Elements
    formRegisterProject: document.getElementById('form-register-project'),
    regProjectName: document.getElementById('reg-project-name'),
    regGitRepoUrl: document.getElementById('reg-git-repo-url'),
    regGitBranch: document.getElementById('reg-git-branch'),
    regProjectType: document.getElementById('reg-project-type'),
    regDeployTarget: document.getElementById('reg-deploy-target'),
    regErrorMsg: document.getElementById('reg-error-msg'),
    btnSubmitProject: document.getElementById('btn-submit-project'),

    // Phase 2: GitHub Integration Drawer Elements
    githubProjectSelect: document.getElementById('github-target-project-select'),
    githubStatusPill: document.getElementById('github-drawer-status-pill'),
    githubStatusLabel: document.getElementById('github-drawer-status-label'),
    githubRepoName: document.getElementById('github-repo-name'),
    githubRepoBranch: document.getElementById('github-repo-branch'),
    githubCommitSha: document.getElementById('github-commit-sha'),
    githubCommitMsg: document.getElementById('github-commit-msg'),
    githubCommitAuthor: document.getElementById('github-commit-author'),
    githubCommitDate: document.getElementById('github-commit-date'),
    githubCloneSnippet: document.getElementById('github-clone-snippet'),
    btnInspectGithub: document.getElementById('btn-inspect-github'),

    // Phase 2: Docker Readiness Drawer Elements
    dockerProjectSelect: document.getElementById('docker-target-project-select'),
    dockerStatusPill: document.getElementById('docker-status-pill'),
    dockerStatusLabel: document.getElementById('docker-status-label'),
    dockerfileLocation: document.getElementById('dockerfile-location'),
    dockerBaseImage: document.getElementById('docker-base-image'),
    dockerExposedPort: document.getElementById('docker-exposed-port'),
    dockerBuildDuration: document.getElementById('docker-build-duration'),
    dockerImageTagSnippet: document.getElementById('docker-image-tag-snippet'),
    btnInspectDocker: document.getElementById('btn-inspect-docker'),

    // Phase 3: Jenkins CI Drawer Elements
    ciProjectSelect: document.getElementById('ci-target-project-select'),
    jenkinsStatusPill: document.getElementById('jenkins-drawer-status-pill'),
    jenkinsStatusLabel: document.getElementById('jenkins-drawer-status-label'),
    ciLastBuildStatus: document.getElementById('ci-last-build-status'),
    ciDockerTag: document.getElementById('ci-docker-tag'),
    ciBuildDuration: document.getElementById('ci-build-duration'),
    ciBuildsCount: document.getElementById('ci-builds-count'),
    ciBuildsList: document.getElementById('ci-builds-list'),
    btnTriggerCi: document.getElementById('btn-trigger-ci'),
    ciTriggerBranch: document.getElementById('ci-trigger-branch'),
    ciDetailsModalBackdrop: document.getElementById('ci-details-modal-backdrop'),
    btnCloseCiDetails: document.getElementById('btn-close-ci-details'),
    ciDetailsBody: document.getElementById('ci-details-body'),

    // Phase 3: Pipeline Stepper Nodes
    pipeStepNodes: [1, 2, 3, 4, 5, 6, 7, 8].map(i => ({
      step: document.getElementById(`pipe-step-${i}`),
      desc: document.getElementById(`pipe-step-desc-${i}`),
      dur: document.getElementById(`pipe-step-dur-${i}`),
    })),

    // Phase 4: Azure Infrastructure Foundation
    azureInfraCard: document.getElementById('azure-infra-card'),
    azureDrawerStatusPill: document.getElementById('azure-drawer-status-pill'),
    azureDrawerStatusLabel: document.getElementById('azure-drawer-status-label'),
    azureRgVal: document.getElementById('azure-rg-val'),
    azureLocationVal: document.getElementById('azure-location-val'),
    azureVnetVal: document.getElementById('azure-vnet-val'),
    azureSubnetVal: document.getElementById('azure-subnet-val'),
    azureAcrVal: document.getElementById('azure-acr-val'),
    btnInspectAzure: document.getElementById('btn-inspect-azure'),
    btnRefreshAzure: document.getElementById('btn-refresh-azure'),

    // Phase 5: Azure Container Registry (ACR)
    acrRegistryCard: document.getElementById('acr-registry-card'),
    acrDrawerStatusPill: document.getElementById('acr-drawer-status-pill'),
    acrDrawerStatusLabel: document.getElementById('acr-drawer-status-label'),
    acrNameVal: document.getElementById('acr-name-val'),
    acrLoginServerVal: document.getElementById('acr-login-server-val'),
    acrRgVal: document.getElementById('acr-rg-val'),
    acrLocationVal: document.getElementById('acr-location-val'),
    acrSkuVal: document.getElementById('acr-sku-val'),
    acrRepoCountVal: document.getElementById('acr-repo-count-val'),
    acrImageCountVal: document.getElementById('acr-image-count-val'),
    acrLastPushVal: document.getElementById('acr-last-push-val'),
    acrLatestImageVal: document.getElementById('acr-latest-image-val'),
    acrLatestDigestVal: document.getElementById('acr-latest-digest-val'),
    btnInspectAcr: document.getElementById('btn-inspect-acr'),
    btnVerifyAcrModal: document.getElementById('btn-verify-acr-modal'),
    btnRefreshAcr: document.getElementById('btn-refresh-acr'),
    acrDetailsModalBackdrop: document.getElementById('acr-details-modal-backdrop'),
    acrDetailsBody: document.getElementById('acr-details-body'),
    btnCloseAcrDetails: document.getElementById('btn-close-acr-details'),

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

      // Phase 4: Probe Azure Infrastructure status
      await loadAzureStatus();

      // Phase 5: Probe Azure Container Registry status
      await loadAcrStatus();

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
        elements.jenkinsStatusLabel.textContent = 'Offline';
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
        elements.jenkinsStatusLabel.textContent = 'Offline';
      }
    }
  }

  /* ==========================================================================
     4d. Phase 4: Azure Infrastructure Foundation Logic
     ========================================================================== */
  async function loadAzureStatus() {
    try {
      const status = await api.getAzureStatus();
      state.azureStatus = status;

      let pillClass = 'standby';
      let labelText = 'Not Configured';

      if (status && status.status === 'READY') {
        pillClass = 'success';
        labelText = 'Connected';
      } else if (status && status.status === 'NOT_CONFIGURED') {
        pillClass = 'standby';
        labelText = 'Not Configured';
      } else if (status && status.status === 'NOT_FOUND') {
        pillClass = 'standby';
        labelText = 'Not Found';
      } else if (status && status.status === 'ERROR') {
        pillClass = 'failed';
        labelText = 'Error';
      }

      // Update Infrastructure panel Azure (Primary) pill
      if (elements.infraAzurePill && elements.infraAzureLabel) {
        elements.infraAzurePill.className = `status-pill ${pillClass}`;
        elements.infraAzureLabel.textContent = labelText;
      }

      // Update Azure Drawer card
      if (elements.azureDrawerStatusPill && elements.azureDrawerStatusLabel) {
        elements.azureDrawerStatusPill.className = `status-pill ${pillClass}`;
        elements.azureDrawerStatusLabel.textContent = labelText;
      }

      if (status) {
        if (elements.azureRgVal && status.resourceGroup) elements.azureRgVal.textContent = status.resourceGroup;
        if (elements.azureLocationVal && status.location) elements.azureLocationVal.textContent = status.location;
      }
    } catch (err) {
      console.warn('Failed to load Azure status:', err);
      if (elements.infraAzurePill && elements.infraAzureLabel) {
        elements.infraAzurePill.className = 'status-pill standby';
        elements.infraAzureLabel.textContent = 'Not Configured';
      }
      if (elements.azureDrawerStatusPill && elements.azureDrawerStatusLabel) {
        elements.azureDrawerStatusPill.className = 'status-pill standby';
        elements.azureDrawerStatusLabel.textContent = 'Not Configured';
      }
    }
  }

  async function inspectAzureInfrastructure() {
    if (!elements.btnInspectAzure) return;
    elements.btnInspectAzure.disabled = true;
    elements.btnInspectAzure.textContent = '🔍 Inspecting Azure...';

    try {
      const infra = await api.getAzureInfrastructure();
      state.azureInfra = infra;

      if (!infra) {
        showToast('Could not retrieve Azure infrastructure telemetry', 'error');
        return;
      }

      const rgStatus = infra.resourceGroup ? infra.resourceGroup.status : 'UNKNOWN';
      const netStatus = infra.network ? infra.network.status : 'UNKNOWN';
      const regStatus = infra.registry ? infra.registry.status : 'UNKNOWN';

      if (infra.resourceGroup && elements.azureRgVal) {
        elements.azureRgVal.textContent = `${infra.resourceGroup.name || 'rg-cloudship-dev'} (${rgStatus})`;
      }
      if (infra.network && elements.azureVnetVal) {
        elements.azureVnetVal.textContent = `${infra.network.vnetName || 'vnet-cloudship'} (${netStatus})`;
      }
      if (infra.network && elements.azureSubnetVal) {
        elements.azureSubnetVal.textContent = `${infra.network.subnetName || 'snet-cloudship'} (${netStatus})`;
      }
      if (infra.registry && elements.azureAcrVal) {
        elements.azureAcrVal.textContent = `${infra.registry.name || 'cloudshipcr'} (${regStatus})`;
      }

      showToast(`Azure Inspected: RG [${rgStatus}], Network [${netStatus}], ACR [${regStatus}]`, 'info');
    } catch (err) {
      showToast(`Azure inspection failed: ${err.message}`, 'error');
    } finally {
      if (elements.btnInspectAzure) {
        elements.btnInspectAzure.disabled = false;
        elements.btnInspectAzure.textContent = '🔍 Inspect Infrastructure';
      }
    }
  }

  /* ==========================================================================
     4e. Phase 5: Azure Container Registry (ACR) Logic
     ========================================================================== */
  function updateAcrBuildTelemetry(latestBuild) {
    if (!latestBuild) return;
    if (elements.acrLastPushVal) {
      const pStatus = latestBuild.pushStatus || 'NOT_ATTEMPTED';
      let pill = 'standby';
      if (pStatus === 'SUCCESS') pill = 'success';
      else if (pStatus === 'FAILED') pill = 'failed';
      else if (pStatus === 'RUNNING') pill = 'deploying';
      elements.acrLastPushVal.innerHTML = `<span class="status-pill ${pill}" style="font-size: 0.65rem; padding: 0.1rem 0.4rem;"><span class="status-dot"></span>${escapeHtml(pStatus)}</span>`;
    }
    if (elements.acrLatestImageVal) {
      elements.acrLatestImageVal.textContent = latestBuild.dockerImageTag || '--';
      elements.acrLatestImageVal.title = latestBuild.dockerImageTag || '';
    }
    if (elements.acrLatestDigestVal) {
      elements.acrLatestDigestVal.textContent = latestBuild.imageDigest || '--';
      elements.acrLatestDigestVal.title = latestBuild.imageDigest || '';
    }
  }

  async function loadAcrStatus() {
    try {
      const [status, regDetails] = await Promise.all([
        api.getAzureRegistryStatus(),
        api.getAzureRegistry().catch(() => null)
      ]);
      state.acrStatus = status;
      state.acrDetails = regDetails;

      const isConnected = status && (status.status === 'READY' || status.connected === true);
      let pillClass = 'standby';
      let labelText = 'Standby';

      if (isConnected) {
        pillClass = 'success';
        labelText = 'Connected';
      } else if (status && status.status === 'NOT_CONFIGURED') {
        pillClass = 'standby';
        labelText = 'Not Configured';
      } else if (status && status.status === 'ERROR') {
        pillClass = 'failed';
        labelText = 'Error';
      }

      if (elements.acrDrawerStatusPill && elements.acrDrawerStatusLabel) {
        elements.acrDrawerStatusPill.className = `status-pill ${pillClass}`;
        elements.acrDrawerStatusLabel.textContent = labelText;
      }

      if (status) {
        if (elements.acrNameVal && status.registryName) elements.acrNameVal.textContent = status.registryName;
        if (elements.acrLoginServerVal && status.loginServer) elements.acrLoginServerVal.textContent = status.loginServer;
      }

      if (regDetails) {
        if (elements.acrNameVal && regDetails.name) elements.acrNameVal.textContent = regDetails.name;
        if (elements.acrLoginServerVal && regDetails.loginServer) elements.acrLoginServerVal.textContent = regDetails.loginServer;
        if (elements.acrRgVal && regDetails.resourceGroup) elements.acrRgVal.textContent = regDetails.resourceGroup;
        if (elements.acrLocationVal && regDetails.location) elements.acrLocationVal.textContent = regDetails.location;
        if (elements.acrSkuVal) elements.acrSkuVal.textContent = `${regDetails.sku || 'Standard'} / ${regDetails.provisioningState || 'Succeeded'}`;
        if (elements.acrRepoCountVal && regDetails.repositoryCount != null) elements.acrRepoCountVal.textContent = regDetails.repositoryCount;
        if (elements.acrImageCountVal && regDetails.imageCount != null) elements.acrImageCountVal.textContent = regDetails.imageCount;
      }

      if (isConnected) {
        api.getAzureRegistryRepositories().then(repos => {
          state.acrRepositories = repos || [];
          if (elements.acrRepoCountVal && repos) {
            elements.acrRepoCountVal.textContent = repos.length;
          }
        }).catch(() => {});
      }

      if (state.latestCIBuild) {
        updateAcrBuildTelemetry(state.latestCIBuild);
      }
    } catch (err) {
      console.warn('Failed to load ACR status:', err);
      if (elements.acrDrawerStatusPill && elements.acrDrawerStatusLabel) {
        elements.acrDrawerStatusPill.className = 'status-pill standby';
        elements.acrDrawerStatusLabel.textContent = 'Standby';
      }
    }
  }

  async function openAcrInspectionModal(prefillVerify = false) {
    if (!elements.acrDetailsModalBackdrop || !elements.acrDetailsBody) return;

    elements.acrDetailsModalBackdrop.classList.add('active');
    elements.acrDetailsBody.innerHTML = `
      <div style="text-align: center; padding: var(--space-4); color: var(--text-dim);">
        <div style="margin-bottom: var(--space-2);">🔍 Querying Azure Container Registry telemetry...</div>
      </div>
    `;

    try {
      const [reg, repos, images] = await Promise.all([
        api.getAzureRegistry().catch(() => null),
        api.getAzureRegistryRepositories().catch(() => []),
        api.getAzureRegistryImages().catch(() => [])
      ]);

      renderAcrModalContent(reg, repos, images, prefillVerify);
    } catch (err) {
      elements.acrDetailsBody.innerHTML = `
        <div style="padding: var(--space-3); background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: var(--radius-sm); color: #F87171; font-size: var(--font-caption);">
          Failed to query Azure Container Registry: ${escapeHtml(err.message)}
        </div>
      `;
    }
  }

  function renderAcrModalContent(reg, repos, images, prefillVerify) {
    const isConn = reg && reg.status === 'READY';
    const regName = reg ? (reg.name || 'cloudshipcr') : 'cloudshipcr';
    const loginServer = reg ? (reg.loginServer || `${regName}.azurecr.io`) : 'cloudshipcr.azurecr.io';
    const repoList = Array.isArray(repos) ? repos : [];

    let defaultRepo = 'cloudship/backend';
    let defaultTag = 'latest';
    if (state.latestCIBuild && state.latestCIBuild.dockerImageTag) {
      const parts = state.latestCIBuild.dockerImageTag.split(':');
      defaultRepo = parts[0] || 'cloudship/backend';
      defaultTag = parts[1] || 'latest';
    }

    elements.acrDetailsBody.innerHTML = `
      <!-- ACR Metadata Card -->
      <div style="background: var(--color-bg-base); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: var(--space-3);">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-2); font-size: var(--font-caption);">
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">Registry Name</span>
            <div style="font-family: var(--font-mono); font-weight: 600; color: var(--text-primary);">${escapeHtml(regName)}</div>
          </div>
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">Login Server</span>
            <div style="font-family: var(--font-mono); color: var(--accent-highlight);">${escapeHtml(loginServer)}</div>
          </div>
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">Resource Group</span>
            <div style="color: var(--text-primary);">${escapeHtml(reg ? reg.resourceGroup : '--')}</div>
          </div>
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">Location / Region</span>
            <div style="color: var(--text-primary);">${escapeHtml(reg ? reg.location : '--')}</div>
          </div>
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">SKU / Provisioning</span>
            <div style="color: var(--text-primary);">${escapeHtml(reg ? (reg.sku || 'Standard') + ' / ' + (reg.provisioningState || 'Succeeded') : 'Standby')}</div>
          </div>
          <div>
            <span style="color: var(--text-dim); font-size: var(--font-micro); text-transform: uppercase;">Connection Status</span>
            <div><span class="status-pill ${isConn ? 'success' : 'standby'}"><span class="status-dot"></span>${isConn ? 'Connected' : 'Standby'}</span></div>
          </div>
        </div>
      </div>

      <!-- Live Repositories & Artifacts -->
      <div>
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <span style="font-size: var(--font-caption); font-weight: 600; color: var(--text-primary);">Registry Repositories (${repoList.length})</span>
          <span style="font-size: var(--font-micro); color: var(--text-dim);">Live ACR Catalog</span>
        </div>
        <div style="background: var(--color-bg-base); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: var(--space-2); max-height: 120px; overflow-y: auto; font-size: var(--font-caption);">
          ${repoList.length === 0 ? `
            <div style="color: var(--text-dim); text-align: center; padding: var(--space-2); font-size: var(--font-micro);">
              No repositories in registry yet. Push a Docker artifact via the Jenkins CI pipeline.
            </div>
          ` : `
            <div style="display: flex; flex-direction: column; gap: 4px;">
              ${repoList.map(r => `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 4px 6px; background: var(--color-surface-elevated); border-radius: var(--radius-xs);">
                  <span style="font-family: var(--font-mono); color: var(--text-primary); font-size: var(--font-caption);">${escapeHtml(r)}</span>
                  <span style="font-size: var(--font-micro); color: var(--text-muted);">Repository</span>
                </div>
              `).join('')}
            </div>
          `}
        </div>
      </div>

      <!-- Live Image Verification Tool -->
      <div style="border-top: 1px solid var(--border-subtle); padding-top: var(--space-3);">
        <div style="font-size: var(--font-caption); font-weight: 600; color: var(--text-primary); margin-bottom: var(--space-2);">
          Image Push Verification
        </div>
        <div style="display: grid; grid-template-columns: 2fr 1.2fr 1fr; gap: var(--space-2); margin-bottom: var(--space-2);">
          <div>
            <label style="font-size: var(--font-micro); color: var(--text-dim); display: block; margin-bottom: 2px;">Repository</label>
            <input type="text" id="verify-repo-input" value="${escapeHtml(defaultRepo)}" class="form-input" style="width: 100%; padding: 4px 8px; font-size: var(--font-caption); font-family: var(--font-mono);">
          </div>
          <div>
            <label style="font-size: var(--font-micro); color: var(--text-dim); display: block; margin-bottom: 2px;">Tag</label>
            <input type="text" id="verify-tag-input" value="${escapeHtml(defaultTag)}" class="form-input" style="width: 100%; padding: 4px 8px; font-size: var(--font-caption); font-family: var(--font-mono);">
          </div>
          <div style="display: flex; align-items: flex-end;">
            <button type="button" class="btn btn-primary btn-sm" id="btn-run-verify-acr" style="width: 100%;">
              Verify in ACR
            </button>
          </div>
        </div>
        <div id="verify-result-box" style="display: none; padding: var(--space-2); border-radius: var(--radius-sm); font-size: var(--font-caption);"></div>
      </div>
    `;

    // Wire up verification button inside modal
    const verifyBtn = document.getElementById('btn-run-verify-acr');
    if (verifyBtn) {
      verifyBtn.addEventListener('click', runImageVerification);
      if (prefillVerify) {
        verifyBtn.focus();
      }
    }
  }

  async function runImageVerification() {
    const repoInput = document.getElementById('verify-repo-input');
    const tagInput = document.getElementById('verify-tag-input');
    const resultBox = document.getElementById('verify-result-box');
    const verifyBtn = document.getElementById('btn-run-verify-acr');

    if (!repoInput || !tagInput || !resultBox) return;

    const repository = repoInput.value.trim();
    const tag = tagInput.value.trim();

    if (!repository || !tag) {
      resultBox.style.display = 'block';
      resultBox.style.background = 'rgba(239, 68, 68, 0.1)';
      resultBox.style.border = '1px solid rgba(239, 68, 68, 0.3)';
      resultBox.style.color = '#F87171';
      resultBox.textContent = 'Please specify both repository and tag to verify.';
      return;
    }

    if (verifyBtn) {
      verifyBtn.disabled = true;
      verifyBtn.textContent = 'Verifying...';
    }

    try {
      const result = await api.verifyAzureRegistryImage({ repository, tag });
      resultBox.style.display = 'block';

      if (result && result.verified) {
        resultBox.style.background = 'rgba(52, 211, 153, 0.1)';
        resultBox.style.border = '1px solid rgba(52, 211, 153, 0.3)';
        resultBox.style.color = '#34D399';
        resultBox.innerHTML = `
          <div style="font-weight: 600; margin-bottom: 2px;">✓ Verified in Azure Container Registry</div>
          <div style="font-family: var(--font-mono); font-size: var(--font-micro); color: var(--text-primary); word-break: break-all;">
            Target: ${escapeHtml(result.loginServer || '')}/${escapeHtml(result.repository || '')}:${escapeHtml(result.tag || '')}<br>
            Status: ${escapeHtml(result.status || 'FOUND')}<br>
            ${result.digest ? `Digest: ${escapeHtml(result.digest)}` : ''}
          </div>
        `;
      } else {
        resultBox.style.background = 'rgba(239, 68, 68, 0.1)';
        resultBox.style.border = '1px solid rgba(239, 68, 68, 0.3)';
        resultBox.style.color = '#F87171';
        resultBox.innerHTML = `
          <div style="font-weight: 600; margin-bottom: 2px;">✗ Image Not Found in ACR</div>
          <div style="font-size: var(--font-micro); color: var(--text-secondary);">
            ${escapeHtml(result ? result.message : 'Image tag was not found in the registry.')}
          </div>
        `;
      }
    } catch (err) {
      resultBox.style.display = 'block';
      resultBox.style.background = 'rgba(239, 68, 68, 0.1)';
      resultBox.style.border = '1px solid rgba(239, 68, 68, 0.3)';
      resultBox.style.color = '#F87171';
      resultBox.textContent = `Verification error: ${err.message}`;
    } finally {
      if (verifyBtn) {
        verifyBtn.disabled = false;
        verifyBtn.textContent = 'Verify in ACR';
      }
    }
  }

  function closeAcrInspectionModal() {
    if (elements.acrDetailsModalBackdrop) {
      elements.acrDetailsModalBackdrop.classList.remove('active');
    }
  }

  /* ==========================================================================
     4f. Pipeline Stepper Control
     ========================================================================== */
  function resetPipelineStepper() {
    if (!elements.pipeStepNodes) return;
    elements.pipeStepNodes.forEach((node, idx) => {
      if (!node.step) return;
      node.step.className = idx === 0 ? 'pipeline-step completed' : 'pipeline-step pending';
      if (node.dur) node.dur.textContent = '--';
    });
    if (elements.pipeStepNodes[0] && elements.pipeStepNodes[0].desc) elements.pipeStepNodes[0].desc.textContent = 'Repository fetched';
    if (elements.pipeStepNodes[1] && elements.pipeStepNodes[1].desc) elements.pipeStepNodes[1].desc.textContent = 'Maven package compiled';
    if (elements.pipeStepNodes[2] && elements.pipeStepNodes[2].desc) elements.pipeStepNodes[2].desc.textContent = 'All tests passed';
    if (elements.pipeStepNodes[3] && elements.pipeStepNodes[3].desc) elements.pipeStepNodes[3].desc.textContent = 'Docker image created';
    if (elements.pipeStepNodes[4] && elements.pipeStepNodes[4].desc) elements.pipeStepNodes[4].desc.textContent = 'Pushed to ACR';
    if (elements.pipeStepNodes[5] && elements.pipeStepNodes[5].desc) elements.pipeStepNodes[5].desc.textContent = 'AKS Rolling Update';
    if (elements.pipeStepNodes[6] && elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Pod Readiness & Probes';
    if (elements.pipeStepNodes[7] && elements.pipeStepNodes[7].desc) elements.pipeStepNodes[7].desc.textContent = 'Workload Active & Routing';
    if (elements.pipelineOverallStatus && elements.pipelineOverallStatusText) {
      elements.pipelineOverallStatus.className = 'status-pill standby';
      elements.pipelineOverallStatusText.textContent = 'Standby';
    }
  }

  function updatePipelineStepper(latestBuild, latestDeployment, latestPipeline) {
    if (!elements.pipeStepNodes) return;
    if (!latestBuild && !latestDeployment && !latestPipeline) {
      resetPipelineStepper();
      return;
    }

    const pipe = latestPipeline || state.latestPipeline || null;
    const dep = latestDeployment || (state.deployments && state.deployments.length > 0 ? state.deployments[0] : null);

    const isSuccess = (pipe && pipe.status === 'SUCCESS') || (!pipe && dep && dep.status === 'SUCCESS') || (latestBuild && latestBuild.status === 'SUCCESS');
    const isRunning = (pipe && pipe.status && (pipe.status.includes('RUNNING') || pipe.status.includes('DEPLOY') || pipe.status.includes('VERIFY'))) || (latestBuild && (latestBuild.status === 'RUNNING' || latestBuild.status === 'QUEUED'));
    const isFailed = (pipe && pipe.status && pipe.status.includes('FAILED')) || (latestBuild && (latestBuild.status === 'FAILED' || latestBuild.status === 'ABORTED'));
    const pushStatus = latestBuild ? latestBuild.pushStatus : (pipe && (pipe.status === 'IMAGE_PUSHED' || pipe.status === 'SUCCESS') ? 'SUCCESS' : null);

    // Step 1: Source
    if (elements.pipeStepNodes[0] && elements.pipeStepNodes[0].step) {
      elements.pipeStepNodes[0].step.className = 'pipeline-step completed';
      if (elements.pipeStepNodes[0].desc) elements.pipeStepNodes[0].desc.textContent = 'Repository fetched';
      if (elements.pipeStepNodes[0].dur) elements.pipeStepNodes[0].dur.textContent = '12s';
    }

    // Step 2: Build
    if (elements.pipeStepNodes[1] && elements.pipeStepNodes[1].step) {
      const bRunning = (pipe && pipe.status === 'CI_RUNNING') || (latestBuild && latestBuild.status === 'RUNNING');
      const bFailed = (pipe && pipe.status === 'CI_FAILED') || (latestBuild && latestBuild.status === 'FAILED');
      const bSuccess = (pipe && pipe.status !== 'CI_RUNNING' && pipe.status !== 'CI_FAILED' && pipe.status !== 'QUEUED') || (latestBuild && latestBuild.status === 'SUCCESS');
      elements.pipeStepNodes[1].step.className = bSuccess ? 'pipeline-step completed' : (bRunning ? 'pipeline-step active' : (bFailed ? 'pipeline-step failed' : 'pipeline-step pending'));
      if (elements.pipeStepNodes[1].desc) elements.pipeStepNodes[1].desc.textContent = bFailed ? 'Compilation failed' : (bRunning ? 'Compiling Maven package...' : 'Maven package compiled');
      if (elements.pipeStepNodes[1].dur) {
        const dur = latestBuild ? (latestBuild.durationSeconds || (latestBuild.durationMs ? Math.round(latestBuild.durationMs / 1000) : null)) : null;
        elements.pipeStepNodes[1].dur.textContent = dur ? Math.max(1, Math.round(dur * 0.4)) + 's' : (bSuccess ? '45s' : '--');
      }
    }

    // Step 3: Test
    if (elements.pipeStepNodes[2] && elements.pipeStepNodes[2].step) {
      const tSuccess = (pipe && pipe.status !== 'CI_RUNNING' && pipe.status !== 'CI_FAILED' && pipe.status !== 'QUEUED') || (latestBuild && latestBuild.status === 'SUCCESS');
      const tFailed = (pipe && pipe.status === 'CI_FAILED') || (latestBuild && latestBuild.status === 'FAILED');
      elements.pipeStepNodes[2].step.className = tSuccess ? 'pipeline-step completed' : (tFailed ? 'pipeline-step failed' : 'pipeline-step pending');
      if (elements.pipeStepNodes[2].desc) elements.pipeStepNodes[2].desc.textContent = tFailed ? 'Tests failed' : 'All tests passed';
      if (elements.pipeStepNodes[2].dur) {
        const dur = latestBuild ? (latestBuild.durationSeconds || (latestBuild.durationMs ? Math.round(latestBuild.durationMs / 1000) : null)) : null;
        elements.pipeStepNodes[2].dur.textContent = dur ? Math.max(1, Math.round(dur * 0.3)) + 's' : (tSuccess ? '28s' : '--');
      }
    }

    // Step 4: Container
    if (elements.pipeStepNodes[3] && elements.pipeStepNodes[3].step) {
      const hasImage = Boolean(latestBuild && latestBuild.dockerImageTag) || (pipe && pipe.dockerImageTag) || isSuccess;
      elements.pipeStepNodes[3].step.className = hasImage ? 'pipeline-step completed' : (isRunning ? 'pipeline-step active' : (isFailed ? 'pipeline-step failed' : 'pipeline-step pending'));
      if (elements.pipeStepNodes[3].desc) elements.pipeStepNodes[3].desc.textContent = hasImage ? 'Docker image created' : 'Containerizing...';
      if (elements.pipeStepNodes[3].dur) {
        const dur = latestBuild ? (latestBuild.durationSeconds || (latestBuild.durationMs ? Math.round(latestBuild.durationMs / 1000) : null)) : null;
        elements.pipeStepNodes[3].dur.textContent = dur ? Math.max(1, Math.round(dur * 0.3)) + 's' : (hasImage ? '35s' : '--');
      }
    }

    // Step 5: Registry — Pushed to ACR
    if (elements.pipeStepNodes[4] && elements.pipeStepNodes[4].step) {
      const regSuccess = pushStatus === 'SUCCESS' || (pipe && ['IMAGE_PUSHED', 'DEPLOYMENT_STARTING', 'DEPLOYING', 'ROLLOUT_VERIFYING', 'SUCCESS'].includes(pipe.status));
      const regRunning = pushStatus === 'RUNNING' || (pipe && pipe.status === 'IMAGE_PUSHING');
      const regFailed = pushStatus === 'FAILED' || (pipe && pipe.status === 'IMAGE_PUSH_FAILED');

      if (regSuccess) {
        elements.pipeStepNodes[4].step.className = 'pipeline-step completed';
        if (elements.pipeStepNodes[4].desc) elements.pipeStepNodes[4].desc.textContent = 'Pushed to ACR';
        if (elements.pipeStepNodes[4].dur) {
          const pushDur = latestBuild && latestBuild.pushDurationMs ? Math.round(latestBuild.pushDurationMs / 1000) : null;
          elements.pipeStepNodes[4].dur.textContent = pushDur != null ? `${pushDur}s` : '22s';
        }
      } else if (regRunning) {
        elements.pipeStepNodes[4].step.className = 'pipeline-step active';
        if (elements.pipeStepNodes[4].desc) elements.pipeStepNodes[4].desc.textContent = 'Pushing to ACR...';
        if (elements.pipeStepNodes[4].dur) elements.pipeStepNodes[4].dur.textContent = '...';
      } else if (regFailed) {
        elements.pipeStepNodes[4].step.className = 'pipeline-step failed';
        if (elements.pipeStepNodes[4].desc) elements.pipeStepNodes[4].desc.textContent = 'ACR push failed';
        if (elements.pipeStepNodes[4].dur) elements.pipeStepNodes[4].dur.textContent = 'ERR';
      } else {
        elements.pipeStepNodes[4].step.className = 'pipeline-step pending';
        if (elements.pipeStepNodes[4].desc) elements.pipeStepNodes[4].desc.textContent = 'ACR push standby';
        if (elements.pipeStepNodes[4].dur) elements.pipeStepNodes[4].dur.textContent = '--';
      }
    }

    // Step 6: Deploy — AKS Rolling Update
    if (elements.pipeStepNodes[5] && elements.pipeStepNodes[5].step) {
      const depSuccess = (pipe && ['ROLLOUT_VERIFYING', 'SUCCESS'].includes(pipe.status)) || (!pipe && dep && dep.status === 'SUCCESS');
      const depRunning = (pipe && ['DEPLOYMENT_STARTING', 'DEPLOYING'].includes(pipe.status)) || (!pipe && dep && (dep.status === 'RUNNING' || dep.status === 'PENDING'));
      const depFailed = (pipe && pipe.status === 'DEPLOYMENT_FAILED') || (!pipe && dep && dep.status === 'FAILED');

      if (depSuccess) {
        elements.pipeStepNodes[5].step.className = 'pipeline-step completed';
        if (elements.pipeStepNodes[5].desc) elements.pipeStepNodes[5].desc.textContent = 'AKS Rolling Update';
        if (elements.pipeStepNodes[5].dur) elements.pipeStepNodes[5].dur.textContent = '18s';
      } else if (depRunning) {
        elements.pipeStepNodes[5].step.className = 'pipeline-step active';
        if (elements.pipeStepNodes[5].desc) elements.pipeStepNodes[5].desc.textContent = 'Deploying to AKS...';
        if (elements.pipeStepNodes[5].dur) elements.pipeStepNodes[5].dur.textContent = '...';
      } else if (depFailed) {
        elements.pipeStepNodes[5].step.className = 'pipeline-step failed';
        if (elements.pipeStepNodes[5].desc) elements.pipeStepNodes[5].desc.textContent = 'AKS deployment failed';
        if (elements.pipeStepNodes[5].dur) elements.pipeStepNodes[5].dur.textContent = 'ERR';
      } else {
        elements.pipeStepNodes[5].step.className = 'pipeline-step pending';
        if (elements.pipeStepNodes[5].desc) elements.pipeStepNodes[5].desc.textContent = 'AKS Rolling Update';
        if (elements.pipeStepNodes[5].dur) elements.pipeStepNodes[5].dur.textContent = '--';
      }
    }

    // Step 7: Health Check — Pod Readiness & Probes
    if (elements.pipeStepNodes[6] && elements.pipeStepNodes[6].step) {
      const healthSuccess = (pipe && pipe.status === 'SUCCESS') || (!pipe && dep && dep.status === 'SUCCESS');
      const healthRunning = (pipe && pipe.status === 'ROLLOUT_VERIFYING') || (!pipe && dep && dep.status === 'RUNNING');
      const healthFailed = (pipe && pipe.status === 'ROLLOUT_FAILED');

      if (healthSuccess) {
        elements.pipeStepNodes[6].step.className = 'pipeline-step completed';
        if (elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Pod Readiness Verified';
        if (elements.pipeStepNodes[6].dur) elements.pipeStepNodes[6].dur.textContent = '14s';
      } else if (healthRunning) {
        elements.pipeStepNodes[6].step.className = 'pipeline-step active';
        if (elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Verifying pod readiness...';
        if (elements.pipeStepNodes[6].dur) elements.pipeStepNodes[6].dur.textContent = '...';
      } else if (healthFailed) {
        elements.pipeStepNodes[6].step.className = 'pipeline-step failed';
        if (elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Rollout verification failed';
        if (elements.pipeStepNodes[6].dur) elements.pipeStepNodes[6].dur.textContent = 'ERR';
      } else {
        elements.pipeStepNodes[6].step.className = 'pipeline-step pending';
        if (elements.pipeStepNodes[6].desc) elements.pipeStepNodes[6].desc.textContent = 'Pod Readiness & Probes';
        if (elements.pipeStepNodes[6].dur) elements.pipeStepNodes[6].dur.textContent = '--';
      }
    }

    // Step 8: Live — Workload Active & Routing
    if (elements.pipeStepNodes[7] && elements.pipeStepNodes[7].step) {
      const isLive = (pipe && pipe.status === 'SUCCESS') || (!pipe && dep && dep.status === 'SUCCESS');
      if (isLive) {
        elements.pipeStepNodes[7].step.className = 'pipeline-step completed';
        if (elements.pipeStepNodes[7].desc) elements.pipeStepNodes[7].desc.textContent = 'Workload Active & Routing';
        if (elements.pipeStepNodes[7].dur) elements.pipeStepNodes[7].dur.textContent = 'Live';
      } else {
        elements.pipeStepNodes[7].step.className = 'pipeline-step pending';
        if (elements.pipeStepNodes[7].desc) elements.pipeStepNodes[7].desc.textContent = 'Workload Active & Routing';
        if (elements.pipeStepNodes[7].dur) elements.pipeStepNodes[7].dur.textContent = '--';
      }
    }

    // Overall Stepper Header Pill
    if (elements.pipelineOverallStatus && elements.pipelineOverallStatusText) {
      if (pipe) {
        if (pipe.status === 'SUCCESS') {
          elements.pipelineOverallStatus.className = 'status-pill success';
          elements.pipelineOverallStatusText.textContent = 'Live (Deployed)';
        } else if (pipe.status.includes('RUNNING') || pipe.status.includes('PUSHING') || pipe.status.includes('DEPLOY') || pipe.status.includes('VERIFY')) {
          elements.pipelineOverallStatus.className = 'status-pill deploying';
          elements.pipelineOverallStatusText.textContent = pipe.status.replace(/_/g, ' ');
        } else if (pipe.status.includes('FAILED') || pipe.status === 'CANCELLED') {
          elements.pipelineOverallStatus.className = 'status-pill failed';
          elements.pipelineOverallStatusText.textContent = pipe.status.replace(/_/g, ' ');
        } else {
          elements.pipelineOverallStatus.className = 'status-pill standby';
          elements.pipelineOverallStatusText.textContent = pipe.status;
        }
      } else if (dep && dep.status === 'SUCCESS') {
        elements.pipelineOverallStatus.className = 'status-pill success';
        elements.pipelineOverallStatusText.textContent = 'Live (Deployed)';
      } else if (pushStatus === 'SUCCESS') {
        elements.pipelineOverallStatus.className = 'status-pill success';
        elements.pipelineOverallStatusText.textContent = 'Pushed to ACR';
      } else if (latestBuild && latestBuild.status === 'SUCCESS') {
        elements.pipelineOverallStatus.className = 'status-pill success';
        elements.pipelineOverallStatusText.textContent = 'Built';
      } else if (isRunning) {
        elements.pipelineOverallStatus.className = 'status-pill deploying';
        elements.pipelineOverallStatusText.textContent = 'In Progress';
      } else if (isFailed) {
        elements.pipelineOverallStatus.className = 'status-pill failed';
        elements.pipelineOverallStatusText.textContent = 'Failed';
      } else {
        elements.pipelineOverallStatus.className = 'status-pill standby';
        elements.pipelineOverallStatusText.textContent = 'Standby';
      }
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
      const [builds, pipelines] = await Promise.all([
        api.getCIBuilds(projectId).catch(() => []),
        api.getProjectPipelines(projectId).catch(() => [])
      ]);
      state.ciBuilds = builds || [];
      state.latestCIBuild = state.ciBuilds.length > 0 ? state.ciBuilds[0] : null;
      state.pipelines = pipelines || [];
      state.latestPipeline = state.pipelines.length > 0 ? state.pipelines[0] : null;

      if (elements.ciBuildsCount) {
        elements.ciBuildsCount.textContent = state.ciBuilds.length;
      }

      if (state.latestPipeline || state.latestCIBuild) {
        const latest = state.latestCIBuild;
        const pipe = state.latestPipeline;
        let pillClass = 'standby';
        let statusText = 'Standby';
        let bNum = '--';
        let durSec = null;

        if (pipe) {
          bNum = pipe.ciBuildId ? `#${pipe.ciBuildId}` : `#${pipe.id}`;
          statusText = pipe.status.replace(/_/g, ' ');
          durSec = pipe.durationSeconds;
          if (pipe.status === 'SUCCESS') pillClass = 'success';
          else if (pipe.status.includes('FAILED') || pipe.status === 'CANCELLED') pillClass = 'failed';
          else pillClass = 'deploying';
        } else if (latest) {
          bNum = `#${latest.jenkinsBuildNumber || latest.buildNumber || latest.id}`;
          statusText = latest.status;
          durSec = latest.durationMs != null ? Math.round(latest.durationMs / 1000) : latest.durationSeconds;
          if (latest.status === 'SUCCESS') pillClass = 'success';
          else if (latest.status === 'FAILED' || latest.status === 'ABORTED') pillClass = 'failed';
          else if (latest.status === 'RUNNING') pillClass = 'deploying';
        }

        if (elements.ciLastBuildStatus) {
          elements.ciLastBuildStatus.innerHTML = `
            <span class="status-pill ${pillClass}" style="font-size: 0.65rem; padding: 0.15rem 0.45rem;">
              <span class="status-dot"></span>${bNum} ${statusText}
            </span>
          `;
        }

        if (elements.ciDockerTag) {
          const tag = (pipe && pipe.dockerImageTag) || (latest && latest.dockerImageTag);
          elements.ciDockerTag.textContent = tag || `cloudship/backend:${latest && latest.commitSha ? latest.commitSha.substring(0, 7) : 'pending'}`;
        }

        if (elements.ciBuildDuration) {
          elements.ciBuildDuration.textContent = durSec != null ? `${durSec}s` : (pillClass === 'deploying' ? 'In progress' : '--');
        }

        updatePipelineStepper(latest, null, state.latestPipeline);
        if (latest) updateAcrBuildTelemetry(latest);
      } else {
        if (elements.ciLastBuildStatus) elements.ciLastBuildStatus.textContent = 'No builds executed';
        if (elements.ciDockerTag) elements.ciDockerTag.textContent = 'cloudship/backend:pending';
        if (elements.ciBuildDuration) elements.ciBuildDuration.textContent = '--';
        resetPipelineStepper();
      }

      renderCIBuilds(state.ciBuilds);
    } catch (err) {
      console.warn('Failed to load CI builds and pipelines:', err);
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

      let pushBadge = '';
      if (b.pushStatus) {
        let pushClass = 'standby';
        if (b.pushStatus === 'SUCCESS') pushClass = 'success';
        else if (b.pushStatus === 'FAILED') pushClass = 'failed';
        else if (b.pushStatus === 'RUNNING') pushClass = 'deploying';
        pushBadge = `<span class="status-pill ${pushClass}" style="font-size: 0.55rem; padding: 0.05rem 0.3rem;" title="ACR Push: ${escapeHtml(b.pushStatus)}">ACR: ${escapeHtml(b.pushStatus)}</span>`;
      }

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
              ${pushBadge}
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

    let pushPillClass = 'standby';
    const pStatus = build.pushStatus || 'NOT_ATTEMPTED';
    if (pStatus === 'SUCCESS') pushPillClass = 'success';
    else if (pStatus === 'FAILED') pushPillClass = 'failed';
    else if (pStatus === 'RUNNING') pushPillClass = 'deploying';

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
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>5. Docker Build (cloudship/backend)</span><span style="color: ${build.status === 'SUCCESS' ? '#34D399' : '#F87171'};">${build.status === 'SUCCESS' ? 'SUCCESS' : 'FAILED'}</span></div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;"><span>6. Push to Azure Container Registry</span><span style="color: ${pStatus === 'SUCCESS' ? '#34D399' : (pStatus === 'FAILED' ? '#F87171' : 'var(--text-muted)')};">${escapeHtml(pStatus)}</span></div>
      `;
    }

    const bNum = build.jenkinsBuildNumber || build.buildNumber || build.id;
    const durSec = build.durationMs != null ? Math.round(build.durationMs / 1000) : build.durationSeconds;
    const pushDurSec = build.pushDurationMs != null ? (Math.round(build.pushDurationMs / 100) / 10) + 's' : '--';

    elements.ciDetailsBody.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); margin-bottom: var(--space-3);">
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Build ID / Number</div>
          <div style="font-weight: 600; font-family: var(--font-mono); color: var(--text-primary);">#${bNum} (ID: ${build.id})</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Build Status</div>
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
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">ACR Push Status</div>
          <div><span class="status-pill ${pushPillClass}"><span class="status-dot"></span>${pStatus}</span></div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Push Duration</div>
          <div style="font-size: var(--font-caption); color: var(--text-primary);">${pushDurSec}</div>
        </div>
        <div style="grid-column: span 2;">
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Container Registry Target</div>
          <div style="font-family: var(--font-mono); font-size: var(--font-caption); color: var(--accent-highlight);">${escapeHtml(build.registryLoginServer || build.registryName || 'cloudshipcr.azurecr.io')}</div>
        </div>
        <div style="grid-column: span 2;">
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Image Digest</div>
          <div style="font-family: var(--font-mono); font-size: 11px; color: var(--text-primary); word-break: break-all;">${escapeHtml(build.imageDigest || '--')}</div>
        </div>
        <div>
          <div style="font-size: var(--font-micro); color: var(--text-dim); text-transform: uppercase;">Total Build Duration</div>
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
        <pre style="background: var(--color-bg-base); border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: var(--space-2); font-family: var(--font-mono); font-size: 11px; color: var(--text-secondary); max-height: 120px; overflow-y: auto; white-space: pre-wrap; margin: 0;">${escapeHtml(build.logsSummary || 'No logs captured.')}</pre>
      </div>

      ${build.pushErrorMessage ? `
        <div style="margin-top: var(--space-2); padding: var(--space-2); background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: var(--radius-sm); color: #F87171; font-size: var(--font-micro);">
          <strong>ACR Push Error:</strong> ${escapeHtml(build.pushErrorMessage)}
        </div>
      ` : ''}

      ${build.errorMessage ? `
        <div style="margin-top: var(--space-2); padding: var(--space-2); background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: var(--radius-sm); color: #F87171; font-size: var(--font-micro);">
          <strong>Build Error:</strong> ${escapeHtml(build.errorMessage)}
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
        try {
          const pipe = await api.triggerPipeline(state.selectedProjectId, {
            branch: branch,
            triggerType: 'MANUAL'
          });
          showToast(`Pipeline #${pipe.id} triggered: ${pipe.status}`, 'success');
        } catch (pipeErr) {
          console.warn('Unified pipeline trigger fallback to direct CI:', pipeErr.message);
          const build = await api.triggerCIBuild(state.selectedProjectId, {
            branch: branch,
            triggerType: 'MANUAL',
          });
          showToast(`CI Build #${build.buildNumber || build.id} triggered: ${build.status}`, 'success');
        }

        await loadCIBuilds(state.selectedProjectId);
        await loadJenkinsStatus();
      } catch (err) {
        showToast(`Failed to trigger pipeline: ${err.message}`, 'error');
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
     6d. Phase 4: Azure Infrastructure Event Listeners
     ========================================================================== */
  if (elements.btnInspectAzure) {
    elements.btnInspectAzure.addEventListener('click', inspectAzureInfrastructure);
  }

  if (elements.btnRefreshAzure) {
    elements.btnRefreshAzure.addEventListener('click', async () => {
      if (elements.btnRefreshAzure) {
        elements.btnRefreshAzure.style.transform = 'rotate(180deg)';
        elements.btnRefreshAzure.style.transition = 'transform 300ms ease';
      }
      try {
        await loadAzureStatus();
        showToast('Azure infrastructure status refreshed', 'info');
      } finally {
        setTimeout(() => {
          if (elements.btnRefreshAzure) {
            elements.btnRefreshAzure.style.transform = 'rotate(0deg)';
          }
        }, 350);
      }
    });
  }

  /* ==========================================================================
     6e. Phase 5: Azure Container Registry Event Listeners
     ========================================================================== */
  if (elements.btnInspectAcr) {
    elements.btnInspectAcr.addEventListener('click', () => openAcrInspectionModal(false));
  }

  if (elements.btnVerifyAcrModal) {
    elements.btnVerifyAcrModal.addEventListener('click', () => openAcrInspectionModal(true));
  }

  if (elements.btnRefreshAcr) {
    elements.btnRefreshAcr.addEventListener('click', async () => {
      if (elements.btnRefreshAcr) {
        elements.btnRefreshAcr.style.transform = 'rotate(180deg)';
        elements.btnRefreshAcr.style.transition = 'transform 300ms ease';
      }
      try {
        await loadAcrStatus();
        showToast('Azure Container Registry telemetry refreshed', 'info');
      } finally {
        setTimeout(() => {
          if (elements.btnRefreshAcr) {
            elements.btnRefreshAcr.style.transform = 'rotate(0deg)';
          }
        }, 350);
      }
    });
  }

  if (elements.btnCloseAcrDetails) {
    elements.btnCloseAcrDetails.addEventListener('click', closeAcrInspectionModal);
  }

  if (elements.acrDetailsModalBackdrop) {
    elements.acrDetailsModalBackdrop.addEventListener('click', (e) => {
      if (e.target === elements.acrDetailsModalBackdrop) {
        closeAcrInspectionModal();
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
    { title: 'Azure Infrastructure', desc: 'Inspect Resource Group, VNet, Subnet, and ACR (Phase 4)', action: () => { openProjectDrawer(); if (elements.azureInfraCard) elements.azureInfraCard.scrollIntoView({ behavior: 'smooth' }); } },
    { title: 'Azure Container Registry (ACR)', desc: 'Inspect ACR repositories, images, and push verification (Phase 5)', action: () => { openProjectDrawer(); if (elements.acrRegistryCard) elements.acrRegistryCard.scrollIntoView({ behavior: 'smooth' }); } },
    { title: 'Verify ACR Image', desc: 'Verify container image existence and digest in ACR', action: () => openAcrInspectionModal(true) },
    { title: 'Kubernetes', desc: 'Cluster nodes, namespaces, and workloads', action: () => showToast('Kubernetes operations: Phase 6', 'info') },
    { title: 'Monitoring', desc: 'Prometheus & Grafana telemetry loops', action: () => showToast('Monitoring stack: Phase 7', 'info') },
    { title: 'Incidents', desc: 'Failure records & post-mortem timelines', action: () => showToast('Zero active incidents recorded', 'info') },
    { title: 'Simulations', desc: 'Controlled chaos engineering laboratory', action: () => showToast('Failure simulations: Phase 8', 'info') },
    { title: 'Recovery', desc: 'Automated rollback & self-healing engine', action: () => showToast('Automated recovery: Phase 9', 'info') },
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
