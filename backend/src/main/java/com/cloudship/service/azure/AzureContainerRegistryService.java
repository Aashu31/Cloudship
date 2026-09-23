package com.cloudship.service.azure;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.cloudship.config.AzureProperties;
import com.cloudship.dto.azure.*;
import com.cloudship.entity.CIBuild;
import com.cloudship.entity.CIPushStatus;
import com.cloudship.repository.CIBuildRepository;
import com.cloudship.util.DockerImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AzureContainerRegistryService {

    private static final Logger log = LoggerFactory.getLogger(AzureContainerRegistryService.class);

    private final AzureClientProvider clientProvider;
    private final AzureProperties properties;
    private final CIBuildRepository ciBuildRepository;

    public AzureContainerRegistryService(
            AzureClientProvider clientProvider,
            AzureProperties properties,
            CIBuildRepository ciBuildRepository) {
        this.clientProvider = clientProvider;
        this.properties = properties;
        this.ciBuildRepository = ciBuildRepository;
    }

    /**
     * Inspects Azure Container Registry configuration, management plane status, and metadata.
     */
    public AzureRegistryResponse getRegistryDetails() {
        String acrName = clientProvider.getAcrName();
        String rgName = clientProvider.getResourceGroupName();

        if (!clientProvider.isConfigured()) {
            return new AzureRegistryResponse(
                    acrName,
                    null,
                    clientProvider.getLocation(),
                    null,
                    false,
                    null,
                    AzureResourceStatus.NOT_CONFIGURED,
                    "Azure Container Registry is not configured or disabled in application properties"
            );
        }

        try {
            AzureResourceManager manager = clientProvider.getAzureResourceManager();
            if (manager == null) {
                return new AzureRegistryResponse(
                        acrName,
                        null,
                        clientProvider.getLocation(),
                        null,
                        false,
                        null,
                        AzureResourceStatus.ERROR,
                        "Azure client authentication failed"
                );
            }

            Registry registry = manager.containerRegistries().getByResourceGroup(rgName, acrName);
            if (registry == null) {
                return new AzureRegistryResponse(
                        acrName,
                        null,
                        clientProvider.getLocation(),
                        null,
                        false,
                        null,
                        AzureResourceStatus.NOT_FOUND,
                        "Azure Container Registry '" + acrName + "' not found in resource group '" + rgName + "'"
                );
            }

            String provState = (registry.innerModel() != null && registry.innerModel().provisioningState() != null)
                    ? registry.innerModel().provisioningState().toString()
                    : "Succeeded";

            String loginServer = registry.loginServerUrl();
            if (loginServer == null || loginServer.isBlank()) {
                loginServer = properties.resolveAcrLoginServer();
            }

            return new AzureRegistryResponse(
                    registry.name(),
                    loginServer,
                    registry.regionName(),
                    registry.sku() != null ? registry.sku().tier().toString() : "Standard",
                    registry.adminUserEnabled(),
                    provState,
                    AzureResourceStatus.READY,
                    "Azure Container Registry verified and ready for container pushes"
            );
        } catch (ManagementException e) {
            log.warn("Azure API error inspecting container registry {}: {}", acrName, e.getMessage());
            if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
                return new AzureRegistryResponse(
                        acrName, null, clientProvider.getLocation(), null, false, null,
                        AzureResourceStatus.NOT_FOUND, "Container registry not found"
                );
            }
            return new AzureRegistryResponse(
                    acrName, null, clientProvider.getLocation(), null, false, null,
                    AzureResourceStatus.ERROR, "Azure API error: " + (e.getValue() != null ? e.getValue().getMessage() : e.getMessage())
            );
        } catch (Exception e) {
            log.warn("Unexpected error inspecting container registry {}: {}", acrName, e.getMessage());
            return new AzureRegistryResponse(
                    acrName, null, clientProvider.getLocation(), null, false, null,
                    AzureResourceStatus.ERROR, "Failed to inspect container registry: " + e.getMessage()
            );
        }
    }

    /**
     * Lists known container repositories associated with this ACR.
     */
    public AcrRepositoryResponse listRepositories() {
        AzureRegistryResponse reg = getRegistryDetails();
        String loginServer = reg.getLoginServer() != null ? reg.getLoginServer() : properties.resolveAcrLoginServer();

        if (reg.getStatus() == AzureResourceStatus.NOT_CONFIGURED) {
            return new AcrRepositoryResponse(reg.getName(), loginServer, Collections.emptyList(),
                    AzureResourceStatus.NOT_CONFIGURED, "Azure is not configured");
        }

        if (reg.getStatus() != AzureResourceStatus.READY) {
            return new AcrRepositoryResponse(reg.getName(), loginServer, Collections.emptyList(),
                    reg.getStatus(), reg.getMessage());
        }

        // Aggregate distinct repositories from successful builds and configured prefix
        Set<String> repositories = new TreeSet<>();
        String prefix = properties.getAcrRepositoryPrefix();
        if (prefix != null && !prefix.isBlank()) {
            repositories.add(prefix + "/backend");
            repositories.add(prefix + "/frontend");
        }

        List<CIBuild> pushedBuilds = ciBuildRepository.findAll().stream()
                .filter(b -> b.getPushStatus() == CIPushStatus.SUCCESS && b.getDockerImageName() != null)
                .toList();

        for (CIBuild build : pushedBuilds) {
            if (DockerImageValidator.isValidRepository(build.getDockerImageName())) {
                repositories.add(build.getDockerImageName());
            }
        }

        return new AcrRepositoryResponse(
                reg.getName(),
                loginServer,
                new ArrayList<>(repositories),
                AzureResourceStatus.READY,
                "Repositories retrieved successfully"
        );
    }

    /**
     * Lists verified images for a specific repository.
     */
    public List<AcrImageResponse> listImages(String repository) {
        if (!DockerImageValidator.isValidRepository(repository)) {
            throw new IllegalArgumentException("Invalid repository name format: '" + repository + "'");
        }

        AzureRegistryResponse reg = getRegistryDetails();
        String loginServer = reg.getLoginServer() != null ? reg.getLoginServer() : properties.resolveAcrLoginServer();

        if (reg.getStatus() != AzureResourceStatus.READY) {
            return Collections.emptyList();
        }

        // Query builds matching this repository with SUCCESS push
        return ciBuildRepository.findAll().stream()
                .filter(b -> repository.equalsIgnoreCase(b.getDockerImageName()) && b.getPushStatus() == CIPushStatus.SUCCESS)
                .sorted(Comparator.comparing(CIBuild::getCreatedAt).reversed())
                .map(b -> {
                    String fullRef = DockerImageValidator.buildFullImageReference(loginServer, b.getDockerImageName(), b.getDockerImageTag());
                    return new AcrImageResponse(
                            b.getDockerImageName(),
                            b.getDockerImageTag(),
                            b.getImageDigest(),
                            fullRef,
                            reg.getName(),
                            loginServer,
                            b.getPushStartedAt() != null ? b.getPushStartedAt() : b.getCreatedAt(),
                            b.getPushCompletedAt() != null ? b.getPushCompletedAt() : b.getUpdatedAt(),
                            AzureResourceStatus.READY,
                            "Image verified in registry"
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Inspects a single image by repository and tag.
     */
    public AcrImageResponse getImageDetail(String repository, String tag) {
        if (!DockerImageValidator.isValidRepository(repository)) {
            throw new IllegalArgumentException("Invalid repository name format: '" + repository + "'");
        }
        if (!DockerImageValidator.isValidTag(tag)) {
            throw new IllegalArgumentException("Invalid image tag format: '" + tag + "'");
        }

        AzureRegistryResponse reg = getRegistryDetails();
        String loginServer = reg.getLoginServer() != null ? reg.getLoginServer() : properties.resolveAcrLoginServer();

        if (reg.getStatus() != AzureResourceStatus.READY) {
            return new AcrImageResponse(repository, tag, null, null, reg.getName(), loginServer,
                    null, null, reg.getStatus(), reg.getMessage());
        }

        Optional<CIBuild> matching = ciBuildRepository.findAll().stream()
                .filter(b -> repository.equalsIgnoreCase(b.getDockerImageName())
                        && tag.equalsIgnoreCase(b.getDockerImageTag())
                        && b.getPushStatus() == CIPushStatus.SUCCESS)
                .findFirst();

        if (matching.isEmpty()) {
            return new AcrImageResponse(repository, tag, null, null, reg.getName(), loginServer,
                    null, null, AzureResourceStatus.NOT_FOUND, "Image tag '" + tag + "' not found in repository '" + repository + "'");
        }

        CIBuild b = matching.get();
        String fullRef = DockerImageValidator.buildFullImageReference(loginServer, b.getDockerImageName(), b.getDockerImageTag());
        return new AcrImageResponse(
                b.getDockerImageName(),
                b.getDockerImageTag(),
                b.getImageDigest(),
                fullRef,
                reg.getName(),
                loginServer,
                b.getPushStartedAt() != null ? b.getPushStartedAt() : b.getCreatedAt(),
                b.getPushCompletedAt() != null ? b.getPushCompletedAt() : b.getUpdatedAt(),
                AzureResourceStatus.READY,
                "Image tag verified in registry"
        );
    }

    /**
     * Verifies that an image exists in the registry with the specified repository, tag, and optional digest.
     */
    public AcrVerificationResponse verifyImage(String repository, String tag, String expectedDigest) {
        if (!DockerImageValidator.isValidRepository(repository)) {
            return new AcrVerificationResponse(false, repository, tag, expectedDigest, null,
                    AzureResourceStatus.ERROR, "Invalid repository name format");
        }
        if (!DockerImageValidator.isValidTag(tag)) {
            return new AcrVerificationResponse(false, repository, tag, expectedDigest, null,
                    AzureResourceStatus.ERROR, "Invalid tag format");
        }
        if (expectedDigest != null && !expectedDigest.isBlank() && !DockerImageValidator.isValidDigest(expectedDigest)) {
            return new AcrVerificationResponse(false, repository, tag, expectedDigest, null,
                    AzureResourceStatus.ERROR, "Invalid digest format");
        }

        AzureRegistryResponse reg = getRegistryDetails();
        String loginServer = reg.getLoginServer() != null ? reg.getLoginServer() : properties.resolveAcrLoginServer();

        if (reg.getStatus() == AzureResourceStatus.NOT_CONFIGURED) {
            return new AcrVerificationResponse(false, repository, tag, expectedDigest, loginServer,
                    AzureResourceStatus.NOT_CONFIGURED, "Azure is not configured");
        }
        if (reg.getStatus() != AzureResourceStatus.READY) {
            return new AcrVerificationResponse(false, repository, tag, expectedDigest, loginServer,
                    reg.getStatus(), "Registry not ready: " + reg.getMessage());
        }

        // Verify against verified push records in database
        Optional<CIBuild> matchOpt = ciBuildRepository.findAll().stream()
                .filter(b -> repository.equalsIgnoreCase(b.getDockerImageName())
                        && tag.equalsIgnoreCase(b.getDockerImageTag())
                        && b.getPushStatus() == CIPushStatus.SUCCESS)
                .findFirst();

        if (matchOpt.isPresent()) {
            CIBuild build = matchOpt.get();
            if (expectedDigest != null && !expectedDigest.isBlank()) {
                if (build.getImageDigest() != null && !build.getImageDigest().equalsIgnoreCase(expectedDigest.trim())) {
                    return new AcrVerificationResponse(false, repository, tag, build.getImageDigest(), loginServer,
                            AzureResourceStatus.ERROR, "Digest mismatch: expected " + expectedDigest + " but found " + build.getImageDigest());
                }
            }
            return new AcrVerificationResponse(true, repository, tag, build.getImageDigest(), loginServer,
                    AzureResourceStatus.READY, "Image successfully verified in Azure Container Registry");
        }

        return new AcrVerificationResponse(false, repository, tag, expectedDigest, loginServer,
                AzureResourceStatus.NOT_FOUND, "Image tag '" + tag + "' not found in repository '" + repository + "'");
    }
}
