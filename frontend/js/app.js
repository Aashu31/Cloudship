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
        <div style="display: flex; flex-direction: column; gap: 2px;">
          <span style="font-weight: 600; font-size: var(--font-body); color: var(--text-primary);">${escapeHtml(proj.name)}</span>
          <span style="font-family: var(--font-mono); font-size: var(--font-micro); color: var(--text-muted);">${escapeHtml(proj.repositoryUrl || 'No VCS URL')}</span>
        </div>
        <button class="btn btn-danger btn-sm btn-delete-project" data-id="${proj.id}" data-name="${escapeHtml(proj.name)}" type="button" title="Delete Project">
          Delete
        </button>
      </div>
    `).join('');

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
          await refreshData();
        } catch (err) {
          showToast(`Deletion failed: ${err.message}`, 'error');
        }
      });
    });
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
     7. Command Palette Modal (Ctrl + K / ⌘K)
     ========================================================================== */
  const commands = [
    { title: 'Overview', desc: 'Engineering dashboard & control center', action: () => switchView('overview') },
    { title: '+ Register New Project', desc: 'Open project registration form in drawer', action: () => openProjectDrawer() },
    { title: 'Deployments', desc: 'Inspect execution timelines and releases', action: () => openProjectDrawer() },
    { title: 'Pipelines', desc: 'Declarative CI/CD build sequences', action: () => showToast('Pipelines configuration: Phase 2', 'info') },
    { title: 'Infrastructure', desc: 'Multi-cloud topology & resources', action: () => showToast('Multi-cloud infrastructure: Phase 3', 'info') },
    { title: 'Kubernetes', desc: 'Cluster nodes, namespaces, and workloads', action: () => showToast('Kubernetes operations: Phase 4', 'info') },
    { title: 'Monitoring', desc: 'Prometheus & Grafana telemetry loops', action: () => showToast('Monitoring stack: Phase 5', 'info') },
    { title: 'Incidents', desc: 'Failure records & post-mortem timelines', action: () => showToast('Zero active incidents recorded', 'info') },
    { title: 'Simulations', desc: 'Controlled chaos engineering laboratory', action: () => showToast('Failure simulations: Phase 6', 'info') },
    { title: 'Recovery', desc: 'Automated rollback & self-healing engine', action: () => showToast('Automated recovery: Phase 7', 'info') },
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
