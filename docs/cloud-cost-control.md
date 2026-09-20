# CloudShip — Cloud Cost Control & Student Credit FinOps (Azure-First)

**Document Version:** 1.1.0  
**Phase:** Phase 0 (Foundation & Architecture)  
**Status:** Approved  

---

## 1. Context: Azure for Students Credit Governance

CloudShip's primary cloud infrastructure runs under an **Azure for Students** subscription, which provides a bounded **$100 credit pool**. Because credit exhaustion stops all running services immediately, **strict financial governance (FinOps) is mandatory from Day 1**.

```text
       ┌─────────────────────────────────────────────────────────┐
       │              Student Credit FinOps Guardrails           │
       ├─────────────────────────────────────────────────────────┤
       │ 1. Local-First Engineering (Docker & Minikube/kind)     │
       │ 2. Always Deallocate VMs When Inactive                  │
       │ 3. Defer Managed AKS Until Final Release Phase          │
       │ 4. Burstable B-Series VMs Only (Standard_B1s / B2s)     │
       │ 5. Clean Resource Group Tagging & Automated Teardowns   │
       │ 6. AWS Zero Spend (Reserved for Phase 13)               │
       └─────────────────────────────────────────────────────────┘
```

---

## 2. Resource Sizing & Cost Comparison Matrix

| Resource | Sizing / SKU | Estimated Cost | FinOps Rule & Justification |
|---|---|---|---|
| **Local Dev Cluster** | Minikube / kind | **$0.00** | Primary testing target for Phase 0–5; uses local machine resources. |
| **Azure VM (CI/CD)** | `Standard_B2s` (2 vCPU, 4GB RAM) | ~$0.0416/hr (~$0.50/day if active 12 hrs) | Must be stopped (`deallocated`) when not running pipelines. |
| **Azure Container Registry** | `Basic` SKU | ~$0.167/day (~$5.00/month) | Lowest cost private registry; keep image count < 10 tags. |
| **Azure Virtual Network** | Standard VNet | **$0.00** | Free of charge; fundamental for secure private communication. |
| **Azure Public IP** | Standard Static IP | ~$0.005/hr (~$3.60/month) | Only 1 public IP permitted; use port forwarding / SSH tunnels. |
| **Managed AKS Cluster** | Standard AKS (Free Tier control plane) | VM worker node costs | **DO NOT PROVISION IN EARLY PHASES**. Use local Minikube/kind to conserve credits. |
| **Azure Database for PostgreSQL** | Flexible Server (`B1ms`) | ~$0.017/hr (~$12/month) | Stop server when inactive, or run PostgreSQL in Docker container on VM. |
| **Azure Key Vault** | Standard Tier | ~$0.03 per 10k ops | Nominal cost; batch operations to minimize API calls. |

---

## 3. Mandatory VM Deallocation Protocol

In Microsoft Azure, simply shutting down an OS from inside the guest OS keeps the VM in the `Stopped` state, which **continues incurring full compute charges**. 

To halt compute billing, VMs must always be placed into the `Deallocated` state via the Azure CLI:
```bash
# Deallocate VM to STOP billing:
az vm deallocate --resource-group rg-cloudship-dev --name vm-cloudship-ci-dev

# Verify VM is 'VM deallocated':
az vm get-instance-view --resource-group rg-cloudship-dev --name vm-cloudship-ci-dev \
  --query "instanceView.statuses[?starts_with(code, 'PowerState/')].displayStatus" -o tsv
```

---

## 4. Azure Budget & Credit Consumption Alerts

1. **Azure Cost Management Budget**:
   - Total Monthly Allocation: **$15.00 USD** (preserving credits across 6+ months).
2. **Notification Thresholds**:
   - **50% ($7.50)**: Informational email warning.
   - **75% ($11.25)**: Review running VMs and purge unneeded ACR container tags.
   - **90% ($13.50)**: Automated script deallocates all non-essential Azure compute.

---

## 5. Artifact Retention & Storage Hygiene

1. **ACR Lifecycle Rules**:
   - Automatically purge untagged image manifests older than 7 days.
   - Maintain a maximum of the 10 most recent tagged releases (`v*`).
2. **Log Analytics Retention**:
   - Set Azure Log Analytics workspace data retention to the free minimum of **30 days**.

---

## 6. Phase 0 Rule: Zero Premature Cloud Provisioning

> [!IMPORTANT]
> **Zero Spend Policy in Phase 0**:
> Phase 0 deliverables consist entirely of specifications, diagrams, security policies, and repository templates. No Azure VMs, ACR registries, or cloud services are provisioned during Phase 0.
