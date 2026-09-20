# CloudShip — Multi-Environment Strategy & Configuration Management (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Environment Tiers

CloudShip adheres to the 12-Factor App methodology: strict decoupling of configuration from source code across three isolated operational tiers.

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   DEVELOPMENT   │  ───► │     STAGING     │  ───► │   PRODUCTION    │
│  (Local/Docker) │       │ (Azure VNet/K8s)│       │ (Azure VNet/K8s)│
└─────────────────┘       └─────────────────┘       └─────────────────┘
```

| Tier | Purpose | Target Platform | Persistence | Data Sensitivity |
|---|---|---|---|---|
| **Development (`dev`)** | Local feature coding & unit tests | Local workstation / Docker Compose | Local PostgreSQL container (`postgres:15-alpine`) | Synthetic test data |
| **Staging (`stage`)** | Integration testing & failure drills | Azure VNet / Minikube or Azure VM | Local/Azure PostgreSQL (Burstable B1ms) | Anonymized staging data |
| **Production (`prod`)** | Operational deployment control plane | Dedicated Azure Resource Group / K8s | Azure Database for PostgreSQL (TLS enforced) | Live operational audit logs |

---

## 2. Configuration Separation & Spring Profiles

Configuration is segregated using Spring Boot profiles:
- `application-dev.yml`: Local PostgreSQL, verbose ANSI console logging.
- `application-stage.yml`: Azure ACR references, staging namespaces, structured JSON logging.
- `application-prod.yml`: Azure Key Vault secrets, TLS verification, restricted endpoints.

### 2.1 Database Configuration Matrix

| Setting | Development (`dev`) | Staging (`stage`) | Production (`prod`) |
|---|---|---|---|
| **Host** | `localhost` or `postgres-dev` | `postgres-stage.internal` | `psql-cloudship-prod.postgres.database.azure.com` |
| **Port** | `5432` | `5432` | `5432` |
| **SSL Mode** | `disable` / `prefer` | `require` | `verify-full` |
| **Pool Size (HikariCP)**| `5` | `10` | `20` |
| **DDL Mode** | `validate` (Flyway managed) | `validate` (Flyway managed) | `validate` (Flyway managed) |

### 2.2 Azure Configuration Reference

| Variable Name | Environment | Description | Example / Default |
|---|---|---|---|
| `AZURE_SUBSCRIPTION_ID` | Stage / Prod | Active Azure Student Subscription ID | `00000000-0000-0000-0000-000000000000` |
| `AZURE_RESOURCE_GROUP` | Stage / Prod | Azure Resource Group name | `rg-cloudship-dev` |
| `AZURE_LOCATION` | Stage / Prod | Azure data center region | `eastus` |
| `AZURE_ACR_NAME` | Stage / Prod | Azure Container Registry instance name | `cloudshipcr` |
| `AZURE_ACR_LOGIN_SERVER`| Stage / Prod | ACR fully qualified domain name | `cloudshipcr.azurecr.io` |
| `AZURE_CLIENT_ID` | Stage / Prod | Azure Service Principal client ID | `00000000-0000-0000-0000-000000000000` |
| `AZURE_TENANT_ID` | Stage / Prod | Azure Entra ID Tenant ID | `00000000-0000-0000-0000-000000000000` |
| `AZURE_CLIENT_SECRET` | Stage / Prod | Service Principal secret (NEVER COMMIT) | `dummy_client_secret` |

---

## 3. Secret Management Strategy

1. **Local Development**:
   - Variables loaded from uncommitted `.env` file.
   - `.env.example` provides the authoritative template with dummy placeholders.
2. **Azure Staging & Production**:
   - Secrets are managed via **Azure Key Vault** or injected into Kubernetes via native Kubernetes Secrets.
   - Pods retrieve secrets via Azure Managed Identity (Workload Identity) without static API keys stored on disk.
3. **Strict Policy**:
   - Never commit `.env`, Azure client secrets, service principal passwords, or database credentials to Git.
