/**
 * CloudShip API Client
 * Encapsulates all HTTP communications with the Spring Boot backend REST API.
 */
const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const isVercel = window.location.hostname.endsWith('.vercel.app');
const API_BASE_URL = (typeof window.CLOUDSHIP_API_URL !== 'undefined')
  ? window.CLOUDSHIP_API_URL
  : (isLocalhost
      ? (window.location.origin.includes(':8088') ? '' : 'http://localhost:8088')
      : (isVercel ? '' : 'https://cloudship-backend.onrender.com'));

// Global fetch interceptor: ensures credentials ('include') and auth events
const _originalFetch = window.fetch;
window.fetch = async function (input, init = {}) {
  const options = { ...init };
  options.credentials = options.credentials || 'include';

  // Support local dev identity simulation
  const devEmail = window.sessionStorage.getItem('cloudship_dev_user');
  if (devEmail && isLocalhost) {
    options.headers = options.headers || {};
    if (options.headers instanceof Headers) {
      if (!options.headers.has('X-Dev-User-Email')) {
        options.headers.set('X-Dev-User-Email', devEmail);
      }
    } else if (Array.isArray(options.headers)) {
      options.headers.push(['X-Dev-User-Email', devEmail]);
    } else {
      options.headers['X-Dev-User-Email'] = devEmail;
    }
  }

  const response = await _originalFetch(input, options);

  if (response.status === 401) {
    window.dispatchEvent(new CustomEvent('cloudship:auth_required', {
      detail: { status: 401, url: input }
    }));
  } else if (response.status === 403) {
    window.dispatchEvent(new CustomEvent('cloudship:access_denied', {
      detail: { status: 403, url: input }
    }));
  }

  return response;
};

const api = {
  /**
   * Retrieves current authenticated user session status from Zero-Trust gateway
   */
  async getAuthMe() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        return { authenticated: false, user: null, status: response.status };
      }
      return await response.json();
    } catch (err) {
      console.warn('Auth check error:', err.message);
      return { authenticated: false, user: null, error: err.message };
    }
  },

  /**
   * Clears local session or redirects to Cloudflare Access logout
   */
  logout() {
    window.sessionStorage.removeItem('cloudship_dev_user');
    if (window.CLOUDFLARE_LOGOUT_URL) {
      window.location.href = window.CLOUDFLARE_LOGOUT_URL;
    } else {
      window.location.reload();
    }
  },
  /**
   * Probes backend and database health status
   */
  async getHealth() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/health`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Health probe unreachable:', err.message);
      return { status: 'DOWN', service: 'cloudship', database: 'UNKNOWN' };
    }
  },

  /**
   * Fetches all registered projects
   */
  async getProjects() {
    const response = await fetch(`${API_BASE_URL}/api/projects`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch projects (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches a single project by ID
   */
  async getProjectById(id) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Project not found (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Creates a new CloudShip project
   */
  async createProject(projectData) {
    const response = await fetch(`${API_BASE_URL}/api/projects`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(projectData),
    });

    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const message = errBody.details && errBody.details.length > 0
        ? errBody.details.join(', ')
        : (errBody.message || `Failed to create project (HTTP ${response.status})`);
      throw new Error(message);
    }
    return await response.json();
  },

  /**
   * Deletes a project by ID
   */
  async deleteProject(id) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to delete project (HTTP ${response.status})`);
    }
    return true;
  },

  /**
   * Fetches deployment history for a specific project
   */
  async getDeploymentsForProject(projectId) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch deployments (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches recent deployments across all projects
   */
  async getAllDeployments() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/deployments`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        return [];
      }
      return await response.json();
    } catch (err) {
      console.warn('Could not fetch all deployments:', err.message);
      return [];
    }
  },

  /**
   * Fetches repository connection info for a project
   */
  async getRepository(projectId) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/repository`, {
      headers: { 'Accept': 'application/json' },
    });
    if (response.status === 404) {
      return null;
    }
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch repository (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Connects a Git repository to a project
   */
  async connectRepository(projectId, payload) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/repository`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const message = errBody.details && errBody.details.length > 0
        ? errBody.details.join(', ')
        : (errBody.message || `Failed to connect repository (HTTP ${response.status})`);
      throw new Error(message);
    }
    return await response.json();
  },

  /**
   * Updates an existing Git repository connection
   */
  async updateRepository(projectId, payload) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/repository`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const message = errBody.details && errBody.details.length > 0
        ? errBody.details.join(', ')
        : (errBody.message || `Failed to update repository (HTTP ${response.status})`);
      throw new Error(message);
    }
    return await response.json();
  },

  /**
   * Disconnects / deletes a Git repository from a project
   */
  async disconnectRepository(projectId) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/repository`, {
      method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to disconnect repository (HTTP ${response.status})`);
    }
    return true;
  },

  /**
   * Verifies Git repository status via public GitHub API
   */
  async verifyRepositoryStatus(projectId) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/repository/status`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Verification failed (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches backend build & Docker container information
   */
  async getBuildInfo() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/build-info`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Build info unreachable:', err.message);
      return null;
    }
  },

  /**
   * Probes Jenkins CI service status
   */
  async getJenkinsStatus() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/jenkins/status`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Jenkins status probe unreachable:', err.message);
      return { available: false, connectionStatus: 'UNAVAILABLE', baseUrl: 'http://localhost:8080' };
    }
  },

  /**
   * Fetches CI build history for a project
   */
  async getCIBuilds(projectId) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/ci-builds`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        return [];
      }
      return await response.json();
    } catch (err) {
      console.warn('Failed to fetch CI builds:', err.message);
      return [];
    }
  },

  /**
   * Fetches a single CI build by ID
   */
  async getCIBuildById(projectId, buildId) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/ci-builds/${buildId}`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch CI build (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Triggers a new Continuous Integration build
   */
  async triggerCIBuild(projectId, payload = {}) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/ci-builds`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const message = errBody.details && errBody.details.length > 0
        ? errBody.details.join(', ')
        : (errBody.message || `Failed to trigger CI build (HTTP ${response.status})`);
      throw new Error(message);
    }
    return await response.json();
  },

  /**
   * Fetches Azure Infrastructure configuration and connection status
   */
  async getAzureStatus() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/azure/status`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Azure status probe unreachable:', err.message);
      return { status: 'ERROR', message: err.message, resourceGroup: 'rg-cloudship-dev', location: 'eastus' };
    }
  },

  /**
   * Fetches full Azure Infrastructure inspection (RG, VNet, Subnet, ACR)
   */
  async getAzureInfrastructure() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/azure/infrastructure`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Azure infrastructure inspection unreachable:', err.message);
      return null;
    }
  },

  /**
   * Fetches Azure Resource Group details
   */
  async getAzureResourceGroup() {
    const response = await fetch(`${API_BASE_URL}/api/azure/resource-group`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch Resource Group (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Azure Network details (VNet & Subnet)
   */
  async getAzureNetwork() {
    const response = await fetch(`${API_BASE_URL}/api/azure/network`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch Network (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Azure Container Registry details
   */
  async getAzureRegistry() {
    const response = await fetch(`${API_BASE_URL}/api/azure/registry`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch Registry (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Azure Container Registry status
   */
  async getAzureRegistryStatus() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/azure/registry/status`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('ACR status probe unreachable:', err.message);
      return { status: 'NOT_CONFIGURED', connected: false, message: err.message };
    }
  },

  /**
   * Fetches Azure Container Registry health
   */
  async getAzureRegistryHealth() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/azure/registry/health`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('ACR health probe unreachable:', err.message);
      return { status: 'ERROR', message: err.message };
    }
  },

  /**
   * Fetches repositories in Azure Container Registry
   */
  async getAzureRegistryRepositories() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/azure/registry/repositories`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('ACR repositories probe unreachable:', err.message);
      return [];
    }
  },

  /**
   * Fetches images in Azure Container Registry
   */
  async getAzureRegistryImages(repository) {
    try {
      const url = repository
        ? `${API_BASE_URL}/api/azure/registry/images/${encodeURIComponent(repository)}`
        : `${API_BASE_URL}/api/azure/registry/images`;
      const response = await fetch(url, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('ACR images probe unreachable:', err.message);
      return [];
    }
  },

  /**
   * Fetches image tag details from Azure Container Registry
   */
  async getAzureRegistryImageDetails(repository, tag) {
    const response = await fetch(`${API_BASE_URL}/api/azure/registry/images/${encodeURIComponent(repository)}/${encodeURIComponent(tag)}`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch image details (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Verifies that an image exists in Azure Container Registry
   */
  async verifyAzureRegistryImage(payload) {
    const response = await fetch(`${API_BASE_URL}/api/azure/registry/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `ACR image verification failed (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Azure Kubernetes Service (AKS) cluster metadata (Phase 6)
   */
  async getAzureAksCluster() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/infrastructure/azure/aks`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('AKS cluster probe unreachable:', err.message);
      return { status: 'NOT_CONNECTED', configured: false, message: err.message };
    }
  },

  /**
   * Probes Azure Kubernetes Service health (Phase 6)
   */
  async getAzureAksHealth() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/infrastructure/azure/aks/health`, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('AKS health probe unreachable:', err.message);
      return { status: 'ERROR', message: err.message };
    }
  },

  /**
   * Lists active Kubernetes workloads / deployments (Phase 6)
   */
  async getKubernetesWorkloads(namespace) {
    try {
      const url = namespace
        ? `${API_BASE_URL}/api/infrastructure/azure/aks/workloads?namespace=${encodeURIComponent(namespace)}`
        : `${API_BASE_URL}/api/infrastructure/azure/aks/workloads`;
      const response = await fetch(url, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Kubernetes workloads probe unreachable:', err.message);
      return [];
    }
  },

  /**
   * Lists active Kubernetes pods (Phase 6)
   */
  async getKubernetesPods(namespace, deployment) {
    try {
      const params = new URLSearchParams();
      if (namespace) params.set('namespace', namespace);
      if (deployment) params.set('deployment', deployment);
      const url = `${API_BASE_URL}/api/infrastructure/azure/aks/pods` + (params.toString() ? `?${params.toString()}` : '');
      const response = await fetch(url, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Kubernetes pods probe unreachable:', err.message);
      return [];
    }
  },

  /**
   * Lists active Kubernetes services (Phase 6)
   */
  async getKubernetesServices(namespace) {
    try {
      const url = namespace
        ? `${API_BASE_URL}/api/infrastructure/azure/aks/services?namespace=${encodeURIComponent(namespace)}`
        : `${API_BASE_URL}/api/infrastructure/azure/aks/services`;
      const response = await fetch(url, {
        headers: { 'Accept': 'application/json' },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (err) {
      console.warn('Kubernetes services probe unreachable:', err.message);
      return [];
    }
  },

  /**
   * Triggers a rolling Kubernetes deployment (Phase 6)
   */
  async triggerDeployment(payload) {
    const response = await fetch(`${API_BASE_URL}/api/deployments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Deployment failed (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches live rollout status for a deployment (Phase 6)
   */
  async getDeploymentRolloutStatus(id) {
    const response = await fetch(`${API_BASE_URL}/api/deployments/${id}/status`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch rollout status (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Triggers a unified CI/CD Pipeline execution (Phase 7)
   */
  async triggerPipeline(projectId, payload = {}) {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/pipelines`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Pipeline trigger failed (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches pipeline executions for a specific project (Phase 7)
   */
  async getProjectPipelines(projectId) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/pipelines`, {
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) {
        return [];
      }
      return await response.json();
    } catch (err) {
      console.warn('Could not fetch project pipelines:', err.message);
      return [];
    }
  },

  /**
   * Fetches recent pipeline executions across all projects (Phase 7)
   */
  async getAllPipelines() {
    try {
      const response = await fetch(`${API_BASE_URL}/api/pipelines`, {
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) {
        return [];
      }
      return await response.json();
    } catch (err) {
      console.warn('Could not fetch all pipelines:', err.message);
      return [];
    }
  },

  /**
   * Fetches a specific pipeline execution by ID (Phase 7)
   */
  async getPipeline(id) {
    const response = await fetch(`${API_BASE_URL}/api/pipelines/${id}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch pipeline (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches live execution and stage status of a pipeline (Phase 7)
   */
  async getPipelineStatus(id) {
    const response = await fetch(`${API_BASE_URL}/api/pipelines/${id}/status`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch pipeline status (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Cancels a running pipeline execution (Phase 7)
   */
  async cancelPipeline(id) {
    const response = await fetch(`${API_BASE_URL}/api/pipelines/${id}/cancel`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to cancel pipeline (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /* ==========================================================================
     Version 8: Observability, Monitoring & Operational Visibility API
     ========================================================================== */

  /**
   * Fetches the complete 3-tier monitoring overview (Application, Cloud/K8s, Pipelines, Events)
   */
  async getMonitoringOverview(refresh = false) {
    const query = refresh ? '?refresh=true' : '';
    const response = await fetch(`${API_BASE_URL}/api/monitoring/overview${query}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch monitoring overview (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Tier 1 Application & JVM health metrics
   */
  async getMonitoringApplication() {
    const response = await fetch(`${API_BASE_URL}/api/monitoring/application`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch application health (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Tier 2 Cloud Infrastructure health (Azure, ACR, AKS)
   */
  async getMonitoringInfrastructure() {
    const response = await fetch(`${API_BASE_URL}/api/monitoring/infrastructure`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch infrastructure health (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches Tier 2 live Kubernetes workloads and pod health
   */
  async getMonitoringKubernetes(namespace = '') {
    const query = namespace ? `?namespace=${encodeURIComponent(namespace)}` : '';
    const response = await fetch(`${API_BASE_URL}/api/monitoring/kubernetes${query}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch Kubernetes health (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches deep health inspection of a specific workload / deployment
   */
  async getMonitoringWorkload(deploymentName, namespace = 'default') {
    const response = await fetch(`${API_BASE_URL}/api/monitoring/workloads/${encodeURIComponent(deploymentName)}?namespace=${encodeURIComponent(namespace)}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch workload '${deploymentName}' (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches operational audit events with optional project and limit filters
   */
  async getMonitoringEvents(projectId = null, limit = 50) {
    const params = new URLSearchParams();
    if (projectId) params.append('projectId', projectId);
    if (limit) params.append('limit', limit);
    const query = params.toString() ? `?${params.toString()}` : '';
    const response = await fetch(`${API_BASE_URL}/api/monitoring/events${query}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch monitoring events (HTTP ${response.status})`);
    }
    return await response.json();
  },

  /**
   * Fetches concrete operational metrics (success rates, pod counts, restarts, latency)
   */
  async getMonitoringMetrics() {
    const response = await fetch(`${API_BASE_URL}/api/monitoring/metrics`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      throw new Error(errBody.message || `Failed to fetch monitoring metrics (HTTP ${response.status})`);
    }
    return await response.json();
  }
};


