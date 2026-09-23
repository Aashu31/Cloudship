package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerregistry.models.Registries;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.azure.resourcemanager.containerregistry.fluent.models.RegistryInner;
import com.azure.resourcemanager.containerregistry.models.Sku;
import com.azure.resourcemanager.containerregistry.models.SkuTier;
import com.cloudship.config.AzureProperties;
import com.cloudship.dto.azure.*;
import com.cloudship.entity.CIBuild;
import com.cloudship.entity.CIPushStatus;
import com.cloudship.entity.Project;
import com.cloudship.repository.CIBuildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureContainerRegistryServiceTest {

    @Mock
    private AzureClientProvider clientProvider;

    @Mock
    private CIBuildRepository ciBuildRepository;

    @Mock
    private AzureResourceManager azureManager;

    @Mock
    private Registries registries;

    @Mock
    private Registry registry;

    private AzureProperties properties;
    private AzureContainerRegistryService service;

    @BeforeEach
    void setUp() {
        properties = new AzureProperties();
        properties.setEnabled(true);
        properties.setAcrName("cloudshipcr");
        properties.setResourceGroup("rg-cloudship-dev");
        properties.setLocation("eastus");

        service = new AzureContainerRegistryService(clientProvider, properties, ciBuildRepository);
    }

    @Test
    @DisplayName("Returns NOT_CONFIGURED when client provider is not configured")
    void getRegistryDetailsNotConfigured() {
        when(clientProvider.isConfigured()).thenReturn(false);
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getLocation()).thenReturn("eastus");

        AzureRegistryResponse response = service.getRegistryDetails();

        assertThat(response.getStatus()).isEqualTo(AzureResourceStatus.NOT_CONFIGURED);
        assertThat(response.getName()).isEqualTo("cloudshipcr");
        assertThat(response.getLoginServer()).isNull();
    }

    @Test
    @DisplayName("Returns READY when registry exists in resource group")
    void getRegistryDetailsReady() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "cloudshipcr")).thenReturn(registry);

        when(registry.name()).thenReturn("cloudshipcr");
        when(registry.loginServerUrl()).thenReturn("cloudshipcr.azurecr.io");
        when(registry.regionName()).thenReturn("eastus");
        Sku sku = mock(Sku.class);
        when(sku.tier()).thenReturn(SkuTier.STANDARD);
        when(registry.sku()).thenReturn(sku);
        when(registry.adminUserEnabled()).thenReturn(true);
        when(registry.innerModel()).thenReturn(new RegistryInner());

        AzureRegistryResponse response = service.getRegistryDetails();

        assertThat(response.getStatus()).isEqualTo(AzureResourceStatus.READY);
        assertThat(response.getName()).isEqualTo("cloudshipcr");
        assertThat(response.getLoginServer()).isEqualTo("cloudshipcr.azurecr.io");
        assertThat(response.getSku()).isEqualTo("Standard");
        assertThat(response.isAdminUserEnabled()).isTrue();
    }

    @Test
    @DisplayName("Returns NOT_FOUND when registry does not exist in resource group")
    void getRegistryDetailsNotFound() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAcrName()).thenReturn("missingcr");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "missingcr")).thenReturn(null);

        AzureRegistryResponse response = service.getRegistryDetails();

        assertThat(response.getStatus()).isEqualTo(AzureResourceStatus.NOT_FOUND);
        assertThat(response.getMessage()).contains("missingcr");
    }

    @Test
    @DisplayName("Lists repositories from successful builds and configured prefix")
    void listRepositories() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "cloudshipcr")).thenReturn(registry);
        when(registry.name()).thenReturn("cloudshipcr");
        when(registry.loginServerUrl()).thenReturn("cloudshipcr.azurecr.io");

        CIBuild build = new CIBuild();
        build.setDockerImageName("cloudship/backend");
        build.setDockerImageTag("a3f9c21");
        build.setPushStatus(CIPushStatus.SUCCESS);

        when(ciBuildRepository.findAll()).thenReturn(List.of(build));

        AcrRepositoryResponse repos = service.listRepositories();

        assertThat(repos.getStatus()).isEqualTo(AzureResourceStatus.READY);
        assertThat(repos.getRepositories()).contains("cloudship/backend");
    }

    @Test
    @DisplayName("Rejects invalid repository name with IllegalArgumentException")
    void listImagesInvalidRepository() {
        assertThatThrownBy(() -> service.listImages("INVALID_UPPERCASE;rm -rf"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Verifies pushed image in registry successfully")
    void verifyImageSuccess() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "cloudshipcr")).thenReturn(registry);
        when(registry.name()).thenReturn("cloudshipcr");
        when(registry.loginServerUrl()).thenReturn("cloudshipcr.azurecr.io");

        CIBuild build = new CIBuild();
        build.setDockerImageName("cloudship/backend");
        build.setDockerImageTag("a3f9c21");
        build.setImageDigest("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        build.setPushStatus(CIPushStatus.SUCCESS);

        when(ciBuildRepository.findAll()).thenReturn(List.of(build));

        AcrVerificationResponse result = service.verifyImage("cloudship/backend", "a3f9c21", "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        assertThat(result.isVerified()).isTrue();
        assertThat(result.getStatus()).isEqualTo(AzureResourceStatus.READY);
    }

    @Test
    @DisplayName("Reports digest mismatch during image verification")
    void verifyImageDigestMismatch() {
        when(clientProvider.isConfigured()).thenReturn(true);
        when(clientProvider.getAcrName()).thenReturn("cloudshipcr");
        when(clientProvider.getResourceGroupName()).thenReturn("rg-cloudship-dev");
        when(clientProvider.getAzureResourceManager()).thenReturn(azureManager);
        when(azureManager.containerRegistries()).thenReturn(registries);
        when(registries.getByResourceGroup("rg-cloudship-dev", "cloudshipcr")).thenReturn(registry);
        when(registry.name()).thenReturn("cloudshipcr");
        when(registry.loginServerUrl()).thenReturn("cloudshipcr.azurecr.io");

        CIBuild build = new CIBuild();
        build.setDockerImageName("cloudship/backend");
        build.setDockerImageTag("a3f9c21");
        build.setImageDigest("sha256:1111111111111111111111111111111111111111111111111111111111111111");
        build.setPushStatus(CIPushStatus.SUCCESS);

        when(ciBuildRepository.findAll()).thenReturn(List.of(build));

        AcrVerificationResponse result = service.verifyImage("cloudship/backend", "a3f9c21", "sha256:2222222222222222222222222222222222222222222222222222222222222222");

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AzureResourceStatus.ERROR);
        assertThat(result.getMessage()).contains("Digest mismatch");
    }
}
