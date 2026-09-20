/**
 * CloudShip — Permanent Application Controller ("Engineered Horizon")
 * Manages Shell state, Command Palette (⌘K), Drawers, Telemetry Probes,
 * Keyboard shortcuts, and API synchronization.
 */

document.addEventListener('DOMContentLoaded', () => {
  // --------------------------------------------------------------------------
  // DOM References
  // --------------------------------------------------------------------------
  // Telemetry & Status
  const backendStatusEl = document.getElementById('backend-status');
  const databaseStatusEl = document.getElementById('database-status');
  const latencyItemEl = document.getElementById('telemetry-latency-item');
  const latencyBadgeEl = document.getElementById('telemetry-latency');
  const metricLatencyVal = document.getElementById('metric-latency-val');

  // Metrics
  const metricProjectsCount = document.getElementById('metric-projects-count');
  const metricDeploymentsCount = document.getElementById('metric-deployments-count');
  const tabProjectsBadge = document.getElementById('tab-projects-badge');
  const tabDeploymentsBadge = document.getElementById('tab-deployments-badge');

  // Tabs
  const tabBtnProjects = document.getElementById('tab-btn-projects');
  const tabBtnDeployments = document.getElementById('tab-btn-deployments');
  const viewProjects = document.getElementById('view-projects');
  const viewDeployments = document.getElementById('view-deployments');

  // Projects View
  const projectsTbody = document.getElementById('projects-tbody');
  const projectsEmpty = document.getElementById('projects-empty');
  const projectsTableWrap = document.getElementById('projects-table-wrap');

  // Deployments View
  const deploymentsTbody = document.getElementById('deployments-tbody');
  const deploymentsEmpty = document.getElementById('deployments-empty');
  const deploymentsTableWrap = document.getElementById('deployments-table-wrap');

  // Create Project Drawer
  const createDrawerBackdrop = document.getElementById('create-drawer-backdrop');
  const btnCloseCreateDrawer = document.getElementById('btn-close-create-drawer');
  const btnCancelCreate = document.getElementById('btn-cancel-create');
  const btnQuickCreate = document.getElementById('btn-quick-create');
  const btnEmptyCreate = document.getElementById('btn-empty-create');
  const drawerCreateForm = document.getElementById('drawer-create-form');
  const inputProjectName = document.getElementById('input-project-name');
  const inputProjectDesc = document.getElementById('input-project-desc');
  const inputProjectRepo = document.getElementById('input-project-repo');
  const btnSubmitCreate = document.getElementById('btn-submit-create');

  // Project Details Drawer
  const detailsDrawerBackdrop = document.getElementById('details-drawer-backdrop');
  const btnCloseDetailsDrawer = document.getElementById('btn-close-details-drawer');
  const btnCloseDetailsBottom = document.getElementById('btn-close-details-bottom');
  const detailsProjectIdBadge = document.getElementById('details-project-id-badge');
  const detailsProjectName = document.getElementById('details-project-name');
  const detailsProjectDesc = document.getElementById('details-project-desc');
  const detailsProjectUrl = document.getElementById('details-project-url');
  const detailsProjectCreated = document.getElementById('details-project-created');
  const detailsDeploymentsTbody = document.getElementById('details-deployments-tbody');
  const detailsDeploymentsEmpty = document.getElementById('details-deployments-empty');

  // Command Palette
  const cmdBackdrop = document.getElementById('cmd-backdrop');
  const btnCmdTrigger = document.getElementById('btn-cmd-trigger');
  const cmdSearchInput = document.getElementById('cmd-search-input');
  const cmdListItems = document.getElementById('cmd-list-items');

  // Header & Global Actions
  const btnRefresh = document.getElementById('btn-refresh');
  const toastContainer = document.getElementById('toast-container');

  let activeProjects = [];
  let activeDeployments = [];

  // --------------------------------------------------------------------------
  // 1. Toast Notification Primitive
  // --------------------------------------------------------------------------
  function toast(message, type = 'info', duration = 3800) {
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.setAttribute('role', 'alert');

    const dot = document.createElement('span');
    dot.className = 'status-dot';
    el.appendChild(dot);

    const text = document.createElement('span');
    text.textContent = message;
    el.appendChild(text);

    toastContainer.appendChild(el);

    setTimeout(() => {
      el.style.opacity = '0';
      el.style.transform = 'translateY(8px)';
      el.style.transition = 'opacity 180ms ease, transform 180ms ease';
      setTimeout(() => el.remove(), 200);
    }, duration);
  }

  // --------------------------------------------------------------------------
  // 2. Health & Telemetry Probing (with RTT latency calculation)
  // --------------------------------------------------------------------------
  async function probeTelemetry() {
    const startTime = performance.now();
    try {
      const health = await api.getHealth();
      const rtt = Math.round(performance.now() - startTime);

      // Latency indicators
      latencyBadgeEl.textContent = `${rtt} ms`;
      latencyItemEl.style.display = 'inline-flex';
      metricLatencyVal.innerHTML = `${rtt}<span style="font-size: 1rem; font-weight: 500; color: var(--text-muted);">ms</span>`;

      // API status pill
      if (health.status === 'UP') {
        backendStatusEl.className = 'status-pill connected';
        backendStatusEl.querySelector('.status-text').textContent = 'ONLINE';
      } else {
        backendStatusEl.className = 'status-pill disconnected';
        backendStatusEl.querySelector('.status-text').textContent = 'OFFLINE';
      }

      // DB status pill
      if (health.database === 'CONNECTED') {
        databaseStatusEl.className = 'status-pill connected';
        databaseStatusEl.querySelector('.status-text').textContent = 'CONNECTED';
      } else if (health.database === 'DISCONNECTED') {
        databaseStatusEl.className = 'status-pill disconnected';
        databaseStatusEl.querySelector('.status-text').textContent = 'DISCONNECTED';
      } else {
        databaseStatusEl.className = 'status-pill degraded';
        databaseStatusEl.querySelector('.status-text').textContent = 'UNKNOWN';
      }
    } catch (err) {
      backendStatusEl.className = 'status-pill disconnected';
      backendStatusEl.querySelector('.status-text').textContent = 'UNREACHABLE';
      databaseStatusEl.className = 'status-pill degraded';
      databaseStatusEl.querySelector('.status-text').textContent = 'OFFLINE';
      metricLatencyVal.innerHTML = `--<span style="font-size: 1rem; font-weight: 500; color: var(--text-muted);">ms</span>`;
    }
  }

  // --------------------------------------------------------------------------
  // 3. Load & Render Projects
  // --------------------------------------------------------------------------
  async function loadProjects() {
    try {
      activeProjects = await api.getProjects();
      const count = activeProjects ? activeProjects.length : 0;
      metricProjectsCount.textContent = count;
      tabProjectsBadge.textContent = count;

      projectsTbody.innerHTML = '';
      if (!activeProjects || activeProjects.length === 0) {
        projectsTableWrap.style.display = 'none';
        projectsEmpty.style.display = 'block';
        return;
      }

      projectsTableWrap.style.display = 'block';
      projectsEmpty.style.display = 'none';

      const fragment = document.createDocumentFragment();

      activeProjects.forEach((project) => {
        const tr = document.createElement('tr');

        // Name & Description
        const tdName = document.createElement('td');
        const strong = document.createElement('strong');
        strong.style.color = 'var(--text-primary)';
        strong.style.display = 'block';
        strong.textContent = project.name;
        tdName.appendChild(strong);

        if (project.description) {
          const desc = document.createElement('span');
          desc.style.fontSize = 'var(--font-caption)';
          desc.style.color = 'var(--text-muted)';
          desc.textContent = project.description;
          tdName.appendChild(desc);
        }
        tr.appendChild(tdName);

        // Repo URL
        const tdRepo = document.createElement('td');
        const aRepo = document.createElement('a');
        aRepo.href = project.repositoryUrl;
        aRepo.target = '_blank';
        aRepo.rel = 'noopener noreferrer';
        aRepo.style.fontFamily = 'var(--font-mono)';
        aRepo.style.fontSize = 'var(--font-caption)';
        aRepo.style.color = 'var(--text-accent)';
        aRepo.style.textDecoration = 'none';
        aRepo.textContent = project.repositoryUrl;
        tdRepo.appendChild(aRepo);
        tr.appendChild(tdRepo);

        // Deployment Count
        const tdDeployments = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = 'badge';
        badge.textContent = `${project.deploymentsCount || 0} runs`;
        tdDeployments.appendChild(badge);
        tr.appendChild(tdDeployments);

        // Created Date
        const tdDate = document.createElement('td');
        tdDate.style.fontSize = 'var(--font-caption)';
        tdDate.textContent = new Date(project.createdAt).toLocaleDateString(undefined, {
          year: 'numeric', month: 'short', day: 'numeric',
        });
        tr.appendChild(tdDate);

        // Actions
        const tdActions = document.createElement('td');
        tdActions.style.textAlign = 'right';

        const btnInspect = document.createElement('button');
        btnInspect.className = 'btn btn-secondary btn-sm';
        btnInspect.type = 'button';
        btnInspect.textContent = 'Inspect';
        btnInspect.addEventListener('click', () => openProjectDetails(project.id));
        tdActions.appendChild(btnInspect);

        const btnDel = document.createElement('button');
        btnDel.className = 'btn btn-danger btn-sm';
        btnDel.type = 'button';
        btnDel.style.marginLeft = 'var(--space-2)';
        btnDel.textContent = 'Delete';
        btnDel.addEventListener('click', () => deleteProject(project.id, project.name));
        tdActions.appendChild(btnDel);

        tr.appendChild(tdActions);
        fragment.appendChild(tr);
      });

      projectsTbody.appendChild(fragment);
    } catch (err) {
      toast(`Failed to load projects: ${err.message}`, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // 4. Load & Render Deployments
  // --------------------------------------------------------------------------
  async function loadDeployments() {
    try {
      activeDeployments = await api.getAllDeployments();
      const count = activeDeployments ? activeDeployments.length : 0;
      metricDeploymentsCount.textContent = count;
      tabDeploymentsBadge.textContent = count;

      deploymentsTbody.innerHTML = '';
      if (!activeDeployments || activeDeployments.length === 0) {
        deploymentsTableWrap.style.display = 'none';
        deploymentsEmpty.style.display = 'block';
        return;
      }

      deploymentsTableWrap.style.display = 'block';
      deploymentsEmpty.style.display = 'none';

      const fragment = document.createDocumentFragment();

      activeDeployments.forEach((dep) => {
        const tr = document.createElement('tr');

        const tdId = document.createElement('td');
        tdId.style.fontFamily = 'var(--font-mono)';
        tdId.textContent = `#dep-${dep.id}`;
        tr.appendChild(tdId);

        const tdProj = document.createElement('td');
        tdProj.textContent = dep.projectName || `Project #${dep.projectId}`;
        tr.appendChild(tdProj);

        const tdVer = document.createElement('td');
        const code = document.createElement('code');
        code.className = 'kbd';
        code.textContent = dep.version;
        tdVer.appendChild(code);
        tr.appendChild(tdVer);

        const tdStatus = document.createElement('td');
        const pill = document.createElement('span');
        pill.className = `status-pill ${dep.status === 'SUCCESS' ? 'connected' : (dep.status === 'FAILED' ? 'disconnected' : 'degraded')}`;
        pill.innerHTML = `<span class="status-dot"></span>${dep.status}`;
        tdStatus.appendChild(pill);
        tr.appendChild(tdStatus);

        const tdTime = document.createElement('td');
        tdTime.style.fontSize = 'var(--font-caption)';
        tdTime.textContent = new Date(dep.createdAt).toLocaleString();
        tr.appendChild(tdTime);

        fragment.appendChild(tr);
      });

      deploymentsTbody.appendChild(fragment);
    } catch (err) {
      console.warn('Could not load deployments:', err.message);
    }
  }

  // --------------------------------------------------------------------------
  // 5. Drawer Controls (Create Project)
  // --------------------------------------------------------------------------
  function openCreateDrawer() {
    createDrawerBackdrop.classList.add('open');
    setTimeout(() => inputProjectName.focus(), 150);
  }

  function closeCreateDrawer() {
    createDrawerBackdrop.classList.remove('open');
    drawerCreateForm.reset();
    document.getElementById('err-project-name').style.display = 'none';
    document.getElementById('err-project-repo').style.display = 'none';
  }

  btnQuickCreate.addEventListener('click', openCreateDrawer);
  if (btnEmptyCreate) btnEmptyCreate.addEventListener('click', openCreateDrawer);
  btnCloseCreateDrawer.addEventListener('click', closeCreateDrawer);
  btnCancelCreate.addEventListener('click', closeCreateDrawer);

  createDrawerBackdrop.addEventListener('click', (e) => {
    if (e.target === createDrawerBackdrop) closeCreateDrawer();
  });

  // Handle Project Creation Form
  drawerCreateForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const name = inputProjectName.value.trim();
    const description = inputProjectDesc.value.trim();
    const repositoryUrl = inputProjectRepo.value.trim();

    if (!name || !repositoryUrl) {
      toast('Project name and repository URL are required.', 'error');
      return;
    }

    btnSubmitCreate.disabled = true;
    btnSubmitCreate.textContent = 'Initializing...';

    try {
      const created = await api.createProject({ name, description, repositoryUrl });
      toast(`Project '${created.name}' initialized successfully!`, 'success');
      closeCreateDrawer();
      await loadProjects();
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      btnSubmitCreate.disabled = false;
      btnSubmitCreate.textContent = 'Initialize Project';
    }
  });

  // --------------------------------------------------------------------------
  // 6. Drawer Controls (Project Details)
  // --------------------------------------------------------------------------
  async function openProjectDetails(id) {
    try {
      const [project, deployments] = await Promise.all([
        api.getProjectById(id),
        api.getDeploymentsForProject(id).catch(() => []),
      ]);

      detailsProjectIdBadge.textContent = `ID #${project.id}`;
      detailsProjectName.textContent = project.name;
      detailsProjectDesc.textContent = project.description || 'No description provided';
      detailsProjectUrl.textContent = project.repositoryUrl;
      detailsProjectUrl.href = project.repositoryUrl;
      detailsProjectCreated.textContent = new Date(project.createdAt).toLocaleString();

      detailsDeploymentsTbody.innerHTML = '';
      if (!deployments || deployments.length === 0) {
        detailsDeploymentsEmpty.style.display = 'block';
      } else {
        detailsDeploymentsEmpty.style.display = 'none';
        deployments.forEach((dep) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td style="font-family: var(--font-mono);">#${dep.id}</td>
            <td><span class="kbd">${dep.version}</span></td>
            <td>
              <span class="status-pill ${dep.status === 'SUCCESS' ? 'connected' : (dep.status === 'FAILED' ? 'disconnected' : 'degraded')}">
                <span class="status-dot"></span>${dep.status}
              </span>
            </td>
            <td style="font-size: var(--font-caption);">${new Date(dep.createdAt).toLocaleString()}</td>
          `;
          detailsDeploymentsTbody.appendChild(tr);
        });
      }

      detailsDrawerBackdrop.classList.add('open');
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  function closeDetailsDrawer() {
    detailsDrawerBackdrop.classList.remove('open');
  }

  btnCloseDetailsDrawer.addEventListener('click', closeDetailsDrawer);
  btnCloseDetailsBottom.addEventListener('click', closeDetailsDrawer);

  detailsDrawerBackdrop.addEventListener('click', (e) => {
    if (e.target === detailsDrawerBackdrop) closeDetailsDrawer();
  });

  // --------------------------------------------------------------------------
  // 7. Delete Project with Confirmation
  // --------------------------------------------------------------------------
  async function deleteProject(id, name) {
    if (!confirm(`Are you sure you want to delete service project '${name}'? This action cannot be undone.`)) {
      return;
    }

    try {
      await api.deleteProject(id);
      toast(`Project '${name}' deleted.`, 'info');
      await loadProjects();
    } catch (err) {
      toast(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // 8. Command Palette (⌘K) Controller
  // --------------------------------------------------------------------------
  function openCommandPalette() {
    cmdBackdrop.classList.add('open');
    cmdSearchInput.value = '';
    filterCommands('');
    setTimeout(() => cmdSearchInput.focus(), 100);
  }

  function closeCommandPalette() {
    cmdBackdrop.classList.remove('open');
  }

  btnCmdTrigger.addEventListener('click', openCommandPalette);

  cmdBackdrop.addEventListener('click', (e) => {
    if (e.target === cmdBackdrop) closeCommandPalette();
  });

  function filterCommands(query) {
    const q = query.toLowerCase();
    const items = cmdListItems.querySelectorAll('.cmd-item');
    items.forEach((item) => {
      const text = item.textContent.toLowerCase();
      item.style.display = text.includes(q) ? 'flex' : 'none';
    });
  }

  cmdSearchInput.addEventListener('input', (e) => {
    filterCommands(e.target.value);
  });

  cmdListItems.addEventListener('click', (e) => {
    const item = e.target.closest('.cmd-item');
    if (!item) return;

    const action = item.dataset.action;
    closeCommandPalette();

    switch (action) {
      case 'new-project':
        openCreateDrawer();
        break;
      case 'refresh':
        btnRefresh.click();
        break;
      case 'nav-projects':
        tabBtnProjects.click();
        break;
      case 'nav-deployments':
        tabBtnDeployments.click();
        break;
      case 'doc-design':
        window.open('file:///c:/Users/Aashu/Desktop/Cloudship/docs/design-system/design-principles.md', '_blank');
        break;
      case 'doc-api':
        window.open('file:///c:/Users/Aashu/Desktop/Cloudship/docs/api.md', '_blank');
        break;
    }
  });

  // --------------------------------------------------------------------------
  // 9. Global Keyboard Shortcuts
  // --------------------------------------------------------------------------
  window.addEventListener('keydown', (e) => {
    const activeEl = document.activeElement;
    const isTyping = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');

    // ⌘K or Ctrl+K -> Command Palette
    if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
      e.preventDefault();
      if (cmdBackdrop.classList.contains('open')) {
        closeCommandPalette();
      } else {
        openCommandPalette();
      }
      return;
    }

    // Escape -> Close active drawer / modal
    if (e.key === 'Escape') {
      if (cmdBackdrop.classList.contains('open')) closeCommandPalette();
      if (createDrawerBackdrop.classList.contains('open')) closeCreateDrawer();
      if (detailsDrawerBackdrop.classList.contains('open')) closeDetailsDrawer();
      return;
    }

    // If typing inside an input field, do not trigger single-letter shortcuts
    if (isTyping) return;

    // 'N' or 'n' -> New project drawer
    if (e.key === 'n' || e.key === 'N') {
      e.preventDefault();
      openCreateDrawer();
    }

    // 'R' or 'r' -> Refresh state
    if (e.key === 'r' || e.key === 'R') {
      e.preventDefault();
      btnRefresh.click();
    }

    // '/' -> Open command search
    if (e.key === '/') {
      e.preventDefault();
      openCommandPalette();
    }
  });

  // --------------------------------------------------------------------------
  // 10. Workspace Tab Switching
  // --------------------------------------------------------------------------
  tabBtnProjects.addEventListener('click', () => {
    tabBtnProjects.classList.add('active');
    tabBtnProjects.setAttribute('aria-selected', 'true');
    tabBtnDeployments.classList.remove('active');
    tabBtnDeployments.setAttribute('aria-selected', 'false');

    viewProjects.style.display = 'block';
    viewDeployments.style.display = 'none';
  });

  tabBtnDeployments.addEventListener('click', () => {
    tabBtnDeployments.classList.add('active');
    tabBtnDeployments.setAttribute('aria-selected', 'true');
    tabBtnProjects.classList.remove('active');
    tabBtnProjects.setAttribute('aria-selected', 'false');

    viewProjects.style.display = 'none';
    viewDeployments.style.display = 'block';
  });

  // Refresh Trigger
  btnRefresh.addEventListener('click', async () => {
    toast('Polling telemetry & synchronizing state...', 'info', 1500);
    await Promise.all([probeTelemetry(), loadProjects(), loadDeployments()]);
  });

  // --------------------------------------------------------------------------
  // 11. Initial Boot Sequence
  // --------------------------------------------------------------------------
  probeTelemetry();
  loadProjects();
  loadDeployments();

  // Polling loop every 12 seconds
  setInterval(probeTelemetry, 12000);
});
