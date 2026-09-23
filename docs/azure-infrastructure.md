# CloudShip — Azure Infrastructure Foundation Architecture & Guide

## 1. Overview & Mission

CloudShip Phase 4 establishes the **Azure Infrastructure Foundation**, integrating Microsoft Azure Resource Management to discover, inspect, and verify the core cloud resources required to support future deployment phases.

### Scope & Strict Boundaries

```
                 CloudShip Platform
                         │
             ┌───────────┴───────────┐
             │ AzureClientProvider   │ (ClientSecretCredential / EnvironmentCredential)
             └───────────┬───────────┘
                         │
     Azure Resource Manager SDK (Java 2.41.0)
                         │
  ┌──────────────────────┼──────────────────────┐
  ▼                      ▼                      ▼
Resource Group    Virtual Network & Subnet    Azure Container Registry (ACR)
(rg-cloudship-dev) (vnet-cloudship / snet-*)   (cloudshipcr.azurecr.io)
[Inspection Only]  [Topology Discovery]       [Read-Only Inspection]
```

> [!IMPORTANT]
> **Infrastructure Boundary Enforcement**:
> - **Phase 4 Scope (Complete)**: Authentication via Azure Identity, client lifecycle management, read-only discovery, topology inspection, resource state verification (Resource Group, VNet, Subnet, ACR), REST endpoints, and frontend drawer integration.
> - **Phase 5 Scope (Complete)**: Automated Docker image pushing to Azure Container Registry (ACR), deterministic image tagging, push telemetry tracking, live digest verification, and ACR Control Center UI. See [docs/azure-acr.md](azure-acr.md).
> - **Strictly Deferred to Phase 6+**:
>   - Azure Kubernetes Service (AKS) provisioning, cluster peering, or pod scheduling.
>   - Continuous Deployment (CD), rollout strategies, ingress routing, or recovery automation.
>   - Azure Monitor / Application Insights telemetry collectors.
>   - Multi-cloud orchestration (AWS, GCP).

---

## 2. Architecture & Components

### Component Structure

- **`AzureProperties`** (`com.cloudship.config.AzureProperties`):
  Type-safe Spring Boot `@ConfigurationProperties(prefix = "cloudship.azure")` mapping all Azure settings.
- **`AzureClientProvider`** (`com.cloudship.service.azure.AzureClientProvider`):
  Interface defining the contract for Azure Resource Manager client provisioning, credential resolution, and configuration inspection.
- **`AzureClientProviderImpl`** (`com.cloudship.service.azure.AzureClientProviderImpl`):
  Production implementation using `AzureProfile`, `ClientSecretCredentialBuilder` (with `EnvironmentCredentialBuilder` fallback), and `AzureResourceManager.authenticate()`. Gracefully degrades to `NOT_CONFIGURED` when credentials or configuration are absent.
- **`AzureInfrastructureService`** (`com.cloudship.service.azure.AzureInfrastructureService`):
  Encapsulates inspection and query logic for Resource Groups, Virtual Networks, Subnets, and Container Registries. Gracefully handles `ManagementException` (e.g., 404 NOT_FOUND vs 403 FORBIDDEN vs network timeout).
- **`AzureInfrastructureController`** (`com.cloudship.controller.AzureInfrastructureController`):
  REST controller mapped to both `/api/infrastructure/azure` and `/api/azure` to serve connection status, resource inspection, aggregated topology, and health probes.

---

## 3. Configuration Reference

### Application Configuration (`backend/src/main/resources/application.yml`)

```yaml
cloudship:
  azure:
    enabled: ${AZURE_ENABLED:false}
    subscription-id: ${AZURE_SUBSCRIPTION_ID:}
    tenant-id: ${AZURE_TENANT_ID:}
    client-id: ${AZURE_CLIENT_ID:}
    client-secret: ${AZURE_CLIENT_SECRET:}
    resource-group: ${AZURE_RESOURCE_GROUP:rg-cloudship-dev}
    location: ${AZURE_LOCATION:eastus}
    vnet-name: ${AZURE_VNET_NAME:vnet-cloudship}
    subnet-name: ${AZURE_SUBNET_NAME:snet-cloudship}
    acr-name: ${AZURE_ACR_NAME:cloudshipcr}
```

### Environment Variables (`.env.example` & `.env`)

| Variable | Required | Default | Description |
|---|---|---|---|
| `AZURE_ENABLED` | No | `false` | Master toggle for Azure SDK initialization |
| `AZURE_SUBSCRIPTION_ID` | When enabled | `""` | Azure Subscription UUID |
| `AZURE_TENANT_ID` | When enabled | `""` | Azure Entra ID / Active Directory Tenant UUID |
| `AZURE_CLIENT_ID` | When enabled | `""` | Service Principal Application (Client) ID |
| `AZURE_CLIENT_SECRET` | When enabled | `""` | Service Principal Client Secret Credential |
| `AZURE_RESOURCE_GROUP` | No | `rg-cloudship-dev` | Target Azure Resource Group name |
| `AZURE_LOCATION` | No | `eastus` | Primary Azure region |
| `AZURE_VNET_NAME` | No | `vnet-cloudship` | Virtual Network name |
| `AZURE_SUBNET_NAME` | No | `snet-cloudship` | Default Subnet name |
| `AZURE_ACR_NAME` | No | `cloudshipcr` | Azure Container Registry instance name |

---

## 4. REST API Specification

All endpoints support both `/api/infrastructure/azure` and `/api/azure` prefixes.

### 4.1 Connection Status
- **Method & Path**: `GET /api/azure/status`
- **Description**: Probes Azure configuration and authentication connectivity.
- **Response `200 OK`**:
```json
{
  "configured": true,
  "connectionStatus": "CONNECTED",
  "subscriptionId": "00000000-0000-0000-0000-000000000000",
  "tenantId": "11111111-1111-1111-1111-111111111111",
  "resourceGroup": "rg-cloudship-dev",
  "location": "eastus",
  "message": "Azure connection established and verified successfully",
  "lastCheckedAt": "2026-09-22T22:00:00Z"
}
```

### 4.2 Resource Group Inspection
- **Method & Path**: `GET /api/azure/resource-group` (alias: `GET /api/azure/resources`)
- **Description**: Inspects target Azure Resource Group existence, provisioning state, location, and tags.
- **Response `200 OK`**:
```json
{
  "name": "rg-cloudship-dev",
  "location": "eastus",
  "provisioningState": "Succeeded",
  "status": "READY",
  "message": "Resource Group verified successfully",
  "tags": {
    "Environment": "dev",
    "ManagedBy": "CloudShip"
  }
}
```

### 4.3 Virtual Network & Subnet Inspection
- **Method & Path**: `GET /api/azure/network`
- **Description**: Inspects target Virtual Network and Subnet topology, address spaces, and CIDR blocks.
- **Response `200 OK`**:
```json
{
  "vnetName": "vnet-cloudship",
  "subnetName": "snet-cloudship",
  "location": "eastus",
  "provisioningState": "Succeeded",
  "addressSpaces": ["10.0.0.0/16"],
  "subnetAddressPrefixes": ["10.0.1.0/24"],
  "status": "READY",
  "subnetStatus": "READY",
  "message": "Virtual network and subnet verified successfully"
}
```

### 4.4 Azure Container Registry (ACR) Inspection
- **Method & Path**: `GET /api/azure/registry`
- **Description**: Inspects target ACR existence, login server URL, SKU tier, and admin user status in discovery mode. Image pushing is withheld per Phase 4 boundary.
- **Response `200 OK`**:
```json
{
  "name": "cloudshipcr",
  "loginServer": "cloudshipcr.azurecr.io",
  "location": "eastus",
  "sku": "Basic",
  "adminUserEnabled": true,
  "provisioningState": "Succeeded",
  "status": "READY",
  "message": "Azure Container Registry verified successfully (discovery mode; push withheld per Phase 4 boundary)"
}
```

### 4.5 Aggregated Infrastructure Overview
- **Method & Path**: `GET /api/azure/infrastructure` (alias: `GET /api/azure`)
- **Description**: Aggregates connection status, resource group, virtual network, and container registry inspection in a single payload.
- **Response `200 OK`**:
```json
{
  "status": { "connectionStatus": "CONNECTED", "configured": true },
  "resourceGroup": { "name": "rg-cloudship-dev", "status": "READY" },
  "network": { "vnetName": "vnet-cloudship", "status": "READY" },
  "containerRegistry": { "name": "cloudshipcr", "status": "READY" }
}
```

### 4.6 Health Probe
- **Method & Path**: `GET /api/azure/health`
- **Description**: High-level health probe for container and monitoring orchestrators.
- **Response `200 OK`**:
```json
{
  "service": "azure-infrastructure",
  "configured": true,
  "status": "CONNECTED",
  "resourceGroup": "rg-cloudship-dev",
  "message": "Azure connection established and verified successfully"
}
```

---

## 5. UI Integration

- **Command Center Pill (`#infra-azure-pill`)**:
  - Displays live status: `Azure Connected` (green), `Azure Disconnected` (red), or `Azure Not Configured` (muted amber).
- **Infrastructure Foundation Drawer Card**:
  - Displays Resource Group, VNet, Subnet, and ACR summary.
  - Includes `Inspect Infrastructure` action button to fetch and present detailed metadata.
