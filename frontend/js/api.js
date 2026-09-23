/**
 * CloudShip API Client
 * Encapsulates all HTTP communications with the Spring Boot backend REST API.
 */
const API_BASE_URL = window.location.origin.includes(':8088')
  ? ''
  : (window.CLOUDSHIP_API_URL || 'http://localhost:8088');

const api = {
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
  }
};

