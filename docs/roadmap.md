# CloudShip — Phased Engineering Roadmap (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## Roadmap Overview

CloudShip is engineered through a disciplined 16-phase progression (Phase 0 through Phase 15). Each phase is strictly bounded, incrementally verifiable, and cost-controlled for an Azure Student subscription environment.

```text
[Phase 0: Foundation] ────► [Phase 1: App Core] ────► [Phase 2: Docker] ────► [Phase 3: Jenkins CI]
                                                                                      │
[Phase 7: Full CI/CD] ◄─── [Phase 6: K8s] ◄─── [Phase 5: ACR] ◄─── [Phase 4: Azure Infra]
       │
       ▼
[Phase 8: Dashboard] ───► [Phase 9: Monitoring] ───► [Phase 10: Rollback] ──► [Phase 11: Failure Sim]
                                                                                       │
[Phase 15: Release] ◄─── [Phase 14: Hardening] ◄─── [Phase 13: AWS] ◄────── [Phase 12: Auto Recovery]
```

---

## Phase Breakdown & Definition of Done (DoD)

### Phase 0: Foundation & Architecture
- **Objective**: Establish specifications, Azure-first architecture, cloud strategy, security baseline, git conventions, Jira backlog, and initial repository scaffolding.
- **Deliverables**: Comprehensive `docs/` suite, `.env.example`, `.gitignore`, verified GitHub link to `Aashu31/Cloudship`.
- **Dependencies**: None.
- **Cost Considerations**: Zero cloud spend ($0.00).
- **Definition of Done**: All Phase 0 documentation created and validated; repository clean and synchronized on branch `main`.

### Phase 1: Application Foundation
- **Objective**: Build the core Java Spring Boot service, PostgreSQL persistence layer, and health probe endpoints.
- **Deliverables**: Maven/Gradle project in `backend/`, Spring Boot Actuator `/actuator/health/*`, Flyway migrations in `database/`, unit test suite.
- **Dependencies**: Phase 0.
- **Cost Considerations**: Local execution only ($0.00).
- **Definition of Done**: `mvn clean test` succeeds with 100% passing tests; local PostgreSQL connected and schema migrated.

### Phase 2: GitHub + Docker
- **Objective**: Containerize the application using multi-stage builds and configure local multi-service composition.
- **Deliverables**: `docker/Dockerfile`, `docker/docker-compose.yml`, non-root user configuration, `.dockerignore`.
- **Dependencies**: Phase 1.
- **Cost Considerations**: Local Docker engine ($0.00).
- **Definition of Done**: Multi-stage Docker image builds with zero warnings; `docker compose up` runs app and database successfully; image size < 250MB.

### Phase 3: Jenkins CI
- **Objective**: Implement continuous integration automation using declarative Jenkins pipelines.
- **Deliverables**: `jenkins/Jenkinsfile` defining checkout, compilation, unit testing, and Docker packaging stages.
- **Dependencies**: Phase 2.
- **Cost Considerations**: Local Jenkins container ($0.00) or burstable Azure VM stopped when idle.
- **Definition of Done**: Jenkins pipeline executes clean build and test run; generates verifiable test reports; fails fast on deliberate test defect.

### Phase 4: Azure Infrastructure
- **Objective**: Design and provision core Azure Resource Group, Virtual Network (VNet), subnets, and Network Security Groups (NSGs).
- **Deliverables**: Bicep / Azure CLI provisioning scripts in `azure/`, resource group `rg-cloudship-dev`, VNet `vnet-cloudship`, NSG rules.
- **Dependencies**: Phase 0, Phase 1.
- **Cost Considerations**: VNets are free; VMs deallocated when not in use to preserve student credits.
- **Definition of Done**: Resource group and VNet created; NSG default-deny rules verified; zero ongoing charges when VMs deallocated.

### Phase 5: Azure Container Registry (ACR)
- **Objective**: Establish private container registry on Azure Container Registry and integrate Jenkins publishing pipeline.
- **Deliverables**: Azure Container Registry `cloudshipcr` (Basic SKU), service principal with `AcrPush` role, Jenkins pipeline stage.
- **Dependencies**: Phase 3, Phase 4.
- **Cost Considerations**: Basic SKU costs ~$0.16/day; keep image retention tight (last 10 tags).
- **Definition of Done**: Jenkins pushes Git-SHA-tagged container image to ACR; image pullable with `AcrPull` credentials.

### Phase 6: Kubernetes
- **Objective**: Deploy application workloads to Kubernetes with declarative manifests and health probing.
- **Deliverables**: Kubernetes manifests (`Deployment`, `Service`, `ConfigMap`, `Secret`, `Ingress`), RollingUpdate strategy, resource limits.
- **Dependencies**: Phase 5.
- **Cost Considerations**: Use local Minikube/kind to save credits; deploy to Azure AKS only when final demonstration requires it.
- **Definition of Done**: Application pods running 2/2 replicas; kubelet Liveness/Readiness probes actively polling Actuator endpoints without errors.

### Phase 7: Complete CI/CD
- **Objective**: Wire end-to-end automated deployment triggered by Git push through ACR to Kubernetes rollout completion.
- **Deliverables**: GitHub Webhook integration, automated deployment stage in Jenkins, rollout status watcher (`kubectl rollout status`).
- **Dependencies**: Phase 3, Phase 6.
- **Cost Considerations**: Standard build agent execution.
- **Definition of Done**: A commit pushed to GitHub automatically triggers Jenkins, builds image, pushes to ACR, and updates K8s deployment with zero downtime.

### Phase 8: CloudShip Dashboard
- **Objective**: Construct user-facing operational dashboard interface for real-time visibility (with optional Vercel static preview).
- **Deliverables**: HTML5/CSS3/Vanilla JS interface in `frontend/`, live deployment list, health status badge, manual deploy trigger button.
- **Dependencies**: Phase 1, Phase 7.
- **Cost Considerations**: Free hosting on Vercel or served directly by Spring Boot.
- **Definition of Done**: Dashboard renders in browser, displays active deployment version, updates dynamically via REST API.

### Phase 9: Monitoring
- **Objective**: Implement comprehensive application and infrastructure telemetry using Azure Monitor.
- **Deliverables**: Azure Monitor Log Analytics workspace, Application Insights telemetry agent, Actuator metric hooks.
- **Dependencies**: Phase 6.
- **Cost Considerations**: Keep under 5GB/month free data ingestion limit.
- **Definition of Done**: Application Insights dashboard displays live request latencies; container logs ingested in Log Analytics.

### Phase 10: Rollback
- **Objective**: Implement manual and programmatic one-click deployment rollback.
- **Deliverables**: Backend rollback endpoint (`/api/deployments/{id}/rollback`), UI rollback trigger, `kubectl rollout undo` integration.
- **Dependencies**: Phase 7, Phase 8.
- **Cost Considerations**: $0.00.
- **Definition of Done**: Executing rollback command immediately reverts running pods to the preceding stable revision; zero dropped requests.

### Phase 11: Failure Simulation
- **Objective**: Build safe, controllable failure injection mechanisms to simulate real-world outages.
- **Deliverables**: Diagnostic simulator endpoints (`/api/simulation/*`), CPU burn injector, probe failure toggler, auto-recovery TTL guard.
- **Dependencies**: Phase 1, Phase 8.
- **Cost Considerations**: $0.00.
- **Definition of Done**: Triggering simulation causes readiness probe to fail predictably; auto-resets after configured timeout.

### Phase 12: Automated Recovery
- **Objective**: Implement autonomous self-healing SRE loop that detects degradation and executes automated rollback.
- **Deliverables**: Deployment supervisor loop, consecutive failure detector, automated rollback trigger, incident audit logging.
- **Dependencies**: Phase 10, Phase 11.
- **Cost Considerations**: $0.00.
- **Definition of Done**: Failure simulation during deployment triggers automated rollback within 30 seconds without manual intervention; incident logged to database.

### Phase 13: AWS Multi-Cloud Expansion [FUTURE]
- **Objective**: Extend deployment capability to Amazon Web Services as a secondary cloud target.
- **Deliverables**: `AwsDeploymentAdapter`, AWS ECR repository, AWS EKS deployment manifests, multi-cloud selector in dashboard.
- **Dependencies**: Phase 7, Phase 12.
- **Cost Considerations**: Provision only when AWS access becomes available.
- **Definition of Done**: CloudShip deploys the identical container image to AWS EKS; verified via health probes.

### Phase 14: Security Hardening
- **Objective**: Comprehensive security review, secret scanning, dependency vulnerability remediation, and Azure Key Vault integration.
- **Deliverables**: OWASP dependency check report, Trivy image scan report, Azure Key Vault secret injection, Pod Security Standard Restricted.
- **Dependencies**: All preceding phases.
- **Cost Considerations**: Azure Key Vault Standard ($0.03 per 10k transactions).
- **Definition of Done**: Zero critical/high CVEs; least-privilege Azure RBAC roles verified; NetworkPolicies prevent lateral traffic.

### Phase 15: Testing + Documentation + Release
- **Objective**: Finalize end-to-end integration test suite, operational runbooks, presentation package, and release tagging.
- **Deliverables**: Automated demonstration script (`scripts/demo-e2e.sh`), operational runbook (`docs/runbook.md`), release tag `v1.0.0`.
- **Dependencies**: Phases 0 through 14.
- **Cost Considerations**: Clean up all cloud resources upon completion.
- **Definition of Done**: Complete demonstration passes cleanly from a fresh clone; all documentation verified.
