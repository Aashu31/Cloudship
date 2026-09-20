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
  }
};

