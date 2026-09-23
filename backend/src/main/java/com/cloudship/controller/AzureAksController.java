package com.cloudship.controller;

import com.cloudship.dto.azure.AksClusterResponse;
import com.cloudship.dto.azure.KubernetesPodResponse;
import com.cloudship.dto.azure.KubernetesServiceResponse;
import com.cloudship.dto.azure.KubernetesWorkloadResponse;
import com.cloudship.service.azure.AzureAksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/infrastructure/azure/aks", "/api/azure/aks"})
public class AzureAksController {

    private static final Logger log = LoggerFactory.getLogger(AzureAksController.class);

    private final AzureAksService aksService;

    public AzureAksController(AzureAksService aksService) {
        this.aksService = aksService;
    }

    @GetMapping({"", "/cluster"})
    public ResponseEntity<AksClusterResponse> getClusterDetails() {
        log.info("Fetching AKS cluster details");
        AksClusterResponse details = aksService.getClusterDetails();
        return ResponseEntity.ok(details);
    }

    @GetMapping("/status")
    public ResponseEntity<AksClusterResponse> getClusterStatus() {
        log.info("Fetching AKS cluster status");
        AksClusterResponse status = aksService.getClusterDetails();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getClusterHealth() {
        log.info("Probing AKS cluster health");
        AksClusterResponse cluster = aksService.getClusterDetails();
        return ResponseEntity.ok(Map.of(
                "service", "azure-kubernetes-service",
                "clusterName", cluster.getName() != null ? cluster.getName() : "",
                "status", cluster.getStatus().name(),
                "configured", cluster.isConfigured(),
                "kubernetesVersion", cluster.getKubernetesVersion() != null ? cluster.getKubernetesVersion() : "",
                "totalNodes", cluster.getTotalNodes() != null ? cluster.getTotalNodes() : 0,
                "powerState", cluster.getPowerState() != null ? cluster.getPowerState() : "",
                "message", cluster.getMessage() != null ? cluster.getMessage() : ""
        ));
    }

    @GetMapping("/workloads")
    public ResponseEntity<List<KubernetesWorkloadResponse>> getWorkloads(
            @RequestParam(name = "namespace", required = false) String namespace) {
        log.info("Listing Kubernetes workloads (namespace: {})", namespace);
        List<KubernetesWorkloadResponse> workloads = aksService.listWorkloads(namespace);
        return ResponseEntity.ok(workloads);
    }

    @GetMapping("/pods")
    public ResponseEntity<List<KubernetesPodResponse>> getPods(
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "deployment", required = false) String deployment) {
        log.info("Listing Kubernetes pods (namespace: {}, deployment: {})", namespace, deployment);
        List<KubernetesPodResponse> pods = aksService.listPods(namespace, deployment);
        return ResponseEntity.ok(pods);
    }

    @GetMapping("/services")
    public ResponseEntity<List<KubernetesServiceResponse>> getServices(
            @RequestParam(name = "namespace", required = false) String namespace) {
        log.info("Listing Kubernetes services (namespace: {})", namespace);
        List<KubernetesServiceResponse> services = aksService.listServices(namespace);
        return ResponseEntity.ok(services);
    }
}
