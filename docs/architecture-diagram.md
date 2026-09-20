# CloudShip — Architecture Diagrams (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. End-to-End System Architecture (Primary: Azure)

This diagram illustrates the primary operational workflow from code commit through CI/CD, Azure Container Registry, Azure cloud orchestration, observability, and automated recovery loops, alongside the future multi-cloud boundary.

```mermaid
flowchart TD
    subgraph DeveloperWorkstation ["Developer Workstation"]
        Dev["Developer"]
        GitCLI["Git CLI"]
        Dev -->|"git push origin main"| GitCLI
    end

    subgraph VersionControl ["Source Control (GitHub)"]
        GitHub["GitHub Repository\n(Aashu31/Cloudship)"]
        Webhook["GitHub Webhook /\nPolling Trigger"]
        GitCLI -->|"Push"| GitHub
        GitHub -->|"Notify on Push"| Webhook
    end

    subgraph CIAutomation ["Continuous Integration (Jenkins)"]
        JenkinsMaster["Jenkins Controller\n(Local / Azure VM)"]
        JenkinsAgent["Jenkins Build Agent\n(Disposable Container)"]
        BuildStep["Maven Compilation\n& Unit Tests"]
        DockerBuild["Docker Multi-Stage Build\n(Alpine JRE)"]
        
        Webhook -->|"Trigger Job"| JenkinsMaster
        JenkinsMaster -->|"Spawn Worker"| JenkinsAgent
        JenkinsAgent -->|"1. Compile & Test"| BuildStep
        BuildStep -->|"2. Package JAR"| DockerBuild
    end

    subgraph RegistryAzure ["Azure Container Registry (ACR)"]
        ACR["Azure Container Registry\n(cloudshipcr.azurecr.io)"]
        DockerBuild -->|"docker push (Azure Service Principal)"| ACR
    end

    subgraph CloudAzure ["Microsoft Azure Infrastructure (Primary Cloud)"]
        subgraph VNet ["Azure Virtual Network (VNet)"]
            subgraph SubnetCompute ["Compute Subnet (NSG Protected)"]
                K8sCluster["Kubernetes Cluster\n(Minikube / kind / Azure AKS)"]
                subgraph K8sPods ["CloudShip Application Pods"]
                    Pod1["Pod Replica 1\n(Spring Boot App)"]
                    Pod2["Pod Replica 2\n(Spring Boot App)"]
                    K8sService["K8s ClusterIP Service"]
                    K8sService --> Pod1
                    K8sService --> Pod2
                end
            end

            subgraph SubnetDB ["Persistence Subnet (Private)"]
                Postgres["PostgreSQL 15+\n(Dev Container / Azure PG Flexible)"]
                Pod1 -->|"TCP 5432 (TLS)"| Postgres
                Pod2 -->|"TCP 5432 (TLS)"| Postgres
            end
        end
    end

    JenkinsAgent -->|"kubectl apply / rollout"| K8sCluster
    ACR -->|"Pull Image"| K8sCluster

    subgraph ObservabilityLayer ["Monitoring & Telemetry"]
        Actuator["Spring Boot Actuator\n(/actuator/health/*)"]
        K8sProbes["Kubelet Probes\n(Liveness & Readiness)"]
        AzureMonitor["Azure Monitor &\nApplication Insights"]
        
        Pod1 -.->|"Exposes Telemetry"| Actuator
        Pod2 -.->|"Exposes Telemetry"| Actuator
        K8sProbes -->|"HTTP GET poll"| Actuator
        Pod1 -.->|"JSON Logs / Traces"| AzureMonitor
        Pod2 -.->|"JSON Logs / Traces"| AzureMonitor
    end

    subgraph SelfHealing ["Automated Recovery & SRE Loop"]
        HealthMonitor["Health Evaluation Engine\n(CloudShip SRE Supervisor)"]
        DecisionEngine{"Deployment\nHealthy?"}
        RollbackExec["Automated Rollback Engine\n(kubectl rollout undo)"]
        IncidentLogger["Incident Logger\n(Persist to PostgreSQL)"]

        K8sProbes -->|"Failure Signal"| HealthMonitor
        HealthMonitor --> DecisionEngine
        DecisionEngine -->|"YES: Stable"| Success["Mark Deployment Succeeded"]
        DecisionEngine -->|"NO: 3 Consecutive Fails"| RollbackExec
        RollbackExec -->|"Trigger Rollout Undo"| K8sCluster
        RollbackExec -->|"Audit Event"| IncidentLogger
        IncidentLogger -->|"Save Incident Record"| Postgres
    end

    subgraph ClientLayer ["CloudShip User Interface"]
        UI["CloudShip Web Dashboard\n(HTML5 / CSS / Vanilla JS)"]
        OptionalVercel["(Optional: Vercel Edge Hosting\nfor Static UI Preview)"]
        UI -.-> OptionalVercel
        UI -->|"Query REST API"| K8sService
    end

    %% Future Multi-Cloud AWS Boundary
    subgraph FutureAWS ["AWS Multi-Cloud Expansion [FUTURE - Phase 13]"]
        AWS_ECR["AWS ECR\n(Future Registry)"]
        AWS_EKS["AWS EKS\n(Future Cluster)"]
        AWS_VPC["AWS VPC\n(Future Network)"]
        AWS_ECR -.-> AWS_EKS
    end

    DockerBuild -.->|"Future Push (Phase 13)"| AWS_ECR

    classDef future stroke-dasharray: 5 5,stroke:#f66,fill:#fff5f5;
    class FutureAWS,AWS_ECR,AWS_EKS,AWS_VPC future;
```

---

## 2. Component Interaction & Sequence Flow

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant GH as GitHub (Aashu31/Cloudship)
    participant JK as Jenkins CI
    participant ACR as Azure Container Registry
    participant K8S as Kubernetes
    participant APP as CloudShip App Pods
    participant DB as PostgreSQL
    participant REC as Recovery Supervisor

    Dev->>GH: git push origin main
    GH->>JK: Webhook notification
    JK->>JK: Checkout, Maven build & run unit tests
    JK->>JK: Build multi-stage Docker image
    JK->>ACR: Push image: cloudshipcr.azurecr.io/app:v1.0.0-sha91102b
    JK->>K8S: Update Deployment manifest with new image tag
    K8S->>ACR: Authenticate & pull image
    K8S->>APP: Start Pods (Rolling update)
    APP->>DB: Verify DB connection & execute Flyway migrations
    
    loop Health Probing (0-60s)
        K8S->>APP: GET /actuator/health/readiness
        APP-->>K8S: 200 OK (Healthy)
    end

    alt Deployment Succeeds
        K8S-->>JK: Rollout complete: 2/2 pods ready
        JK->>DB: Log Deployment Record (Status: SUCCESS)
    else Health Check Fails (Simulated Fault or Runtime Error)
        APP-->>K8S: 503 SERVICE UNAVAILABLE (Readiness Down)
        K8S->>REC: Unhealthy threshold exceeded (timeout)
        REC->>K8S: Execute `kubectl rollout undo deployment/cloudship-app`
        K8S->>APP: Revert to previous stable replica set
        REC->>DB: Log Incident (Status: ROLLED_BACK, MTTR: 18s)
    end
```
