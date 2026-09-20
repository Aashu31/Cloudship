/**
 * CloudShip — Application Controller
 * Handles user interactions, DOM updates, polling, and API integration.
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const backendStatusEl = document.getElementById('backend-status');
  const databaseStatusEl = document.getElementById('database-status');

  const metricProjectsCountEl = document.getElementById('metric-projects-count');
  const metricDeploymentsCountEl = document.getElementById('metric-deployments-count');

  const projectsTableBody = document.getElementById('projects-table-body');
  const projectsEmptyState = document.getElementById('projects-empty-state');
  const btnRefresh = document.getElementById('btn-refresh');

  const createProjectForm = document.getElementById('create-project-form');
  const projectNameInput = document.getElementById('project-name');
  const projectDescInput = document.getElementById('project-description');
  const projectRepoInput = document.getElementById('project-repo-url');
  const btnCreateProject = document.getElementById('btn-create-project');

  const deploymentsTableBody = document.getElementById('deployments-table-body');
  const deploymentsEmptyState = document.getElementById('deployments-empty-state');

  const notificationEl = document.getElementById('global-notification');

  // Modal Elements
  const modalOverlay = document.getElementById('project-details-modal');
  const modalCloseBtn = document.getElementById('modal-close-btn');
  const modalProjectName = document.getElementById('modal-project-name');
  const modalProjectId = document.getElementById('modal-project-id');
  const modalProjectDesc = document.getElementById('modal-project-desc');
  const modalProjectRepo = document.getElementById('modal-project-repo');
  const modalProjectCreated = document.getElementById('modal-project-created');
  const modalDeploymentsBody = document.getElementById('modal-deployments-body');
  const modalDeploymentsEmpty = document.getElementById('modal-deployments-empty');

  // --------------------------------------------------------------------------
  // Notifications
  // --------------------------------------------------------------------------
  function showNotification(message, type = 'success', duration = 4000) {
    notificationEl.textContent = message;
    notificationEl.className = `notification ${type}`;
    notificationEl.style.display = 'block';

    setTimeout(() => {
      notificationEl.style.display = 'none';
    }, duration);
  }

  // --------------------------------------------------------------------------
  // Health Status Probing
  // --------------------------------------------------------------------------
  async function checkHealth() {
    const health = await api.getHealth();

    // Backend indicator
    if (health.status === 'UP') {
      backendStatusEl.className = 'status-indicator connected';
      backendStatusEl.querySelector('.status-text').textContent = 'CONNECTED';
    } else {
      backendStatusEl.className = 'status-indicator disconnected';
      backendStatusEl.querySelector('.status-text').textContent = 'DISCONNECTED';
    }

    // Database indicator
    if (health.database === 'CONNECTED') {
      databaseStatusEl.className = 'status-indicator connected';
      databaseStatusEl.querySelector('.status-text').textContent = 'CONNECTED';
    } else if (health.database === 'DISCONNECTED') {
      databaseStatusEl.className = 'status-indicator disconnected';
      databaseStatusEl.querySelector('.status-text').textContent = 'DISCONNECTED';
    } else {
      databaseStatusEl.className = 'status-indicator unknown';
      databaseStatusEl.querySelector('.status-text').textContent = 'UNKNOWN';
    }
  }

  // --------------------------------------------------------------------------
  // Load Projects & Deployments
  // --------------------------------------------------------------------------
  async function loadProjects() {
    try {
      const projects = await api.getProjects();
      metricProjectsCountEl.textContent = projects.length;

      projectsTableBody.innerHTML = '';
      if (!projects || projects.length === 0) {
        projectsEmptyState.style.display = 'block';
        return;
      }

      projectsEmptyState.style.display = 'none';

      projects.forEach((proj) => {
        const tr = document.createElement('tr');

        // Name column
        const tdName = document.createElement('td');
        const spanName = document.createElement('span');
        spanName.className = 'project-name';
        spanName.textContent = proj.name;
        tdName.appendChild(spanName);
        if (proj.description) {
          const smallDesc = document.createElement('small');
          smallDesc.style.color = 'var(--text-muted)';
          smallDesc.textContent = proj.description;
          tdName.appendChild(document.createElement('br'));
          tdName.appendChild(smallDesc);
        }
        tr.appendChild(tdName);

        // Repo column
        const tdRepo = document.createElement('td');
        const aRepo = document.createElement('a');
        aRepo.className = 'project-url';
        aRepo.href = proj.repositoryUrl;
        aRepo.target = '_blank';
        aRepo.rel = 'noopener noreferrer';
        aRepo.textContent = proj.repositoryUrl;
        tdRepo.appendChild(aRepo);
        tr.appendChild(tdRepo);

        // Deployments count column
        const tdCount = document.createElement('td');
        tdCount.textContent = proj.deploymentsCount || 0;
        tr.appendChild(tdCount);

        // Created column
        const tdCreated = document.createElement('td');
        tdCreated.textContent = new Date(proj.createdAt).toLocaleDateString(undefined, {
          year: 'numeric', month: 'short', day: 'numeric',
        });
        tr.appendChild(tdCreated);

        // Actions column
        const tdActions = document.createElement('td');
        tdActions.style.display = 'flex';
        tdActions.style.gap = '0.5rem';

        const btnDetails = document.createElement('button');
        btnDetails.className = 'btn btn-secondary';
        btnDetails.textContent = 'Details';
        btnDetails.addEventListener('click', () => openProjectDetails(proj.id));
        tdActions.appendChild(btnDetails);

        const btnDelete = document.createElement('button');
        btnDelete.className = 'btn btn-danger-outline';
        btnDelete.textContent = 'Delete';
        btnDelete.addEventListener('click', () => deleteProject(proj.id, proj.name));
        tdActions.appendChild(btnDelete);

        tr.appendChild(tdActions);
        projectsTableBody.appendChild(tr);
      });
    } catch (err) {
      console.error('Failed to load projects:', err);
      showNotification(err.message, 'error');
    }
  }

  async function loadDeployments() {
    try {
      const deployments = await api.getAllDeployments();
      metricDeploymentsCountEl.textContent = deployments ? deployments.length : 0;

      deploymentsTableBody.innerHTML = '';
      if (!deployments || deployments.length === 0) {
        deploymentsEmptyState.style.display = 'block';
        return;
      }

      deploymentsEmptyState.style.display = 'none';

      deployments.forEach((dep) => {
        const tr = document.createElement('tr');

        const tdId = document.createElement('td');
        tdId.textContent = `#${dep.id}`;
        tr.appendChild(tdId);

        const tdProj = document.createElement('td');
        tdProj.textContent = dep.projectName || `Project #${dep.projectId}`;
        tr.appendChild(tdProj);

        const tdVer = document.createElement('td');
        tdVer.textContent = dep.version;
        tr.appendChild(tdVer);

        const tdStatus = document.createElement('td');
        const badge = document.createElement('span');
        badge.className = `status-indicator ${dep.status === 'SUCCESS' ? 'connected' : (dep.status === 'FAILED' ? 'disconnected' : 'unknown')}`;
        badge.textContent = dep.status;
        tdStatus.appendChild(badge);
        tr.appendChild(tdStatus);

        const tdTime = document.createElement('td');
        tdTime.textContent = new Date(dep.createdAt).toLocaleString();
        tr.appendChild(tdTime);

        deploymentsTableBody.appendChild(tr);
      });
    } catch (err) {
      console.error('Failed to load deployments:', err);
    }
  }

  // --------------------------------------------------------------------------
  // Create Project
  // --------------------------------------------------------------------------
  createProjectForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const name = projectNameInput.value.trim();
    const description = projectDescInput.value.trim();
    const repositoryUrl = projectRepoInput.value.trim();

    if (!name || !repositoryUrl) {
      showNotification('Project name and repository URL are required.', 'error');
      return;
    }

    btnCreateProject.disabled = true;
    btnCreateProject.textContent = 'Creating...';

    try {
      const newProject = await api.createProject({ name, description, repositoryUrl });
      showNotification(`Project '${newProject.name}' created successfully!`, 'success');
      createProjectForm.reset();
      await loadProjects();
    } catch (err) {
      showNotification(err.message, 'error');
    } finally {
      btnCreateProject.disabled = false;
      btnCreateProject.textContent = 'Create Project';
    }
  });

  // --------------------------------------------------------------------------
  // Delete Project
  // --------------------------------------------------------------------------
  async function deleteProject(id, name) {
    if (!confirm(`Are you sure you want to delete project '${name}'? This action cannot be undone.`)) {
      return;
    }

    try {
      await api.deleteProject(id);
      showNotification(`Project '${name}' deleted successfully.`, 'success');
      await loadProjects();
    } catch (err) {
      showNotification(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // Project Details Modal
  // --------------------------------------------------------------------------
  async function openProjectDetails(id) {
    try {
      const [project, deployments] = await Promise.all([
        api.getProjectById(id),
        api.getDeploymentsForProject(id).catch(() => []),
      ]);

      modalProjectName.textContent = project.name;
      modalProjectId.textContent = project.id;
      modalProjectDesc.textContent = project.description || 'No description provided';
      modalProjectRepo.textContent = project.repositoryUrl;
      modalProjectRepo.href = project.repositoryUrl;
      modalProjectCreated.textContent = new Date(project.createdAt).toLocaleString();

      modalDeploymentsBody.innerHTML = '';
      if (!deployments || deployments.length === 0) {
        modalDeploymentsEmpty.style.display = 'block';
      } else {
        modalDeploymentsEmpty.style.display = 'none';
        deployments.forEach((dep) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>#${dep.id}</td>
            <td><code>${dep.version}</code></td>
            <td><span class="status-indicator ${dep.status === 'SUCCESS' ? 'connected' : (dep.status === 'FAILED' ? 'disconnected' : 'unknown')}">${dep.status}</span></td>
            <td>${new Date(dep.createdAt).toLocaleString()}</td>
          `;
          modalDeploymentsBody.appendChild(tr);
        });
      }

      modalOverlay.classList.add('open');
    } catch (err) {
      showNotification(err.message, 'error');
    }
  }

  modalCloseBtn.addEventListener('click', () => {
    modalOverlay.classList.remove('open');
  });

  modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) {
      modalOverlay.classList.remove('open');
    }
  });

  btnRefresh.addEventListener('click', () => {
    checkHealth();
    loadProjects();
    loadDeployments();
  });

  // Initial Boot
  checkHealth();
  loadProjects();
  loadDeployments();

  // Polling loop for health checks (every 10s)
  setInterval(checkHealth, 10000);
});
