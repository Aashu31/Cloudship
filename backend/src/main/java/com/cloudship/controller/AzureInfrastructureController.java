package com.cloudship.controller;

import com.cloudship.dto.azure.*;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.AzureInfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/infrastructure/azure", "/api/azure"})
public class AzureInfrastructureController {

    private static final Logger log = LoggerFactory.getLogger(AzureInfrastructureController.class);

    private final AzureInfrastructureService azureService;
    private final AzureContainerRegistryService acrService;

    public AzureInfrastructureController(
            AzureInfrastructureService azureService,
            AzureContainerRegistryService acrService) {
        this.azureService = azureService;
        this.acrService = acrService;
    }

    @GetMapping({"", "/infrastructure"})
    public ResponseEntity<AzureInfrastructureResponse> getInfrastructureOverview() {
        log.info("Fetching Azure infrastructure overview");
        AzureInfrastructureResponse overview = azureService.getInfrastructureOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/status")
    public ResponseEntity<AzureStatusResponse> getAzureStatus() {
        log.info("Fetching Azure connection status");
        AzureStatusResponse status = azureService.getAzureStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping({"/resources", "/resource-group"})
    public ResponseEntity<AzureResourceGroupResponse> getResourceGroup() {
        log.info("Inspecting Azure resource group");
        AzureResourceGroupResponse rg = azureService.getResourceGroup();
        return ResponseEntity.ok(rg);
    }

    @GetMapping("/network")
    public ResponseEntity<AzureNetworkResponse> getNetwork() {
        log.info("Inspecting Azure network (VNet & Subnet)");
        AzureNetworkResponse network = azureService.getNetwork();
        return ResponseEntity.ok(network);
    }

    @GetMapping("/registry")
    public ResponseEntity<AzureRegistryResponse> getContainerRegistry() {
        log.info("Inspecting Azure Container Registry");
        AzureRegistryResponse registry = acrService.getRegistryDetails();
        return ResponseEntity.ok(registry);
    }

    @GetMapping("/registry/status")
    public ResponseEntity<AzureRegistryResponse> getRegistryStatus() {
        log.info("Fetching Azure Container Registry status");
        AzureRegistryResponse registry = acrService.getRegistryDetails();
        return ResponseEntity.ok(registry);
    }

    @GetMapping("/registry/health")
    public ResponseEntity<Map<String, Object>> getRegistryHealth() {
        log.info("Probing Azure Container Registry health");
        AzureRegistryResponse reg = acrService.getRegistryDetails();
        return ResponseEntity.ok(Map.of(
                "service", "azure-container-registry",
                "registryName", reg.getName() != null ? reg.getName() : "",
                "loginServer", reg.getLoginServer() != null ? reg.getLoginServer() : "",
                "status", reg.getStatus().name(),
                "sku", reg.getSku() != null ? reg.getSku() : "",
                "provisioningState", reg.getProvisioningState() != null ? reg.getProvisioningState() : "",
                "message", reg.getMessage() != null ? reg.getMessage() : ""
        ));
    }

    @GetMapping("/registry/repositories")
    public ResponseEntity<AcrRepositoryResponse> getRegistryRepositories() {
        log.info("Listing Azure Container Registry repositories");
        AcrRepositoryResponse response = acrService.listRepositories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registry/images")
    public ResponseEntity<List<AcrImageResponse>> getRegistryImages(
            @RequestParam(name = "repository", required = false) String repository) {
        log.info("Listing Azure Container Registry images (filter: {})", repository);
        if (repository != null && !repository.isBlank()) {
            return ResponseEntity.ok(acrService.listImages(repository.trim()));
        }
        AcrRepositoryResponse repos = acrService.listRepositories();
        if (repos.getRepositories() == null || repos.getRepositories().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        // Return images for the primary repository
        return ResponseEntity.ok(acrService.listImages(repos.getRepositories().get(0)));
    }

    @GetMapping("/registry/images/{repository:.+}")
    public ResponseEntity<List<AcrImageResponse>> getImagesForRepository(
            @PathVariable("repository") String repository) {
        log.info("Listing images for repository '{}'", repository);
        return ResponseEntity.ok(acrService.listImages(repository));
    }

    @GetMapping("/registry/images/{repository:.+}/{tag}")
    public ResponseEntity<AcrImageResponse> getImageDetail(
            @PathVariable("repository") String repository,
            @PathVariable("tag") String tag) {
        log.info("Fetching image details for repository '{}', tag '{}'", repository, tag);
        return ResponseEntity.ok(acrService.getImageDetail(repository, tag));
    }

    @PostMapping("/registry/verify")
    public ResponseEntity<AcrVerificationResponse> verifyRegistryImage(
            @RequestBody Map<String, String> payload) {
        String repository = payload != null ? payload.get("repository") : null;
        String tag = payload != null ? payload.get("tag") : null;
        String digest = payload != null ? payload.get("digest") : null;

        log.info("Verifying image in ACR: repository='{}', tag='{}', digest='{}'", repository, tag, digest);
        AcrVerificationResponse response = acrService.verifyImage(repository, tag, digest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAzureHealth() {
        log.info("Probing Azure health");
        AzureStatusResponse status = azureService.getAzureStatus();
        return ResponseEntity.ok(Map.of(
                "service", "azure-infrastructure",
                "configured", status.isConfigured(),
                "status", status.getConnectionStatus().name(),
                "resourceGroup", status.getResourceGroup() != null ? status.getResourceGroup() : "",
                "message", status.getMessage() != null ? status.getMessage() : ""
        ));
    }
}
