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
  }
};
