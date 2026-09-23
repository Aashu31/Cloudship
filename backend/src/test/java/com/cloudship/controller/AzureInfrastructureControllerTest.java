package com.cloudship.controller;

import com.cloudship.dto.azure.*;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.AzureInfrastructureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AzureInfrastructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AzureInfrastructureService azureInfrastructureService;

    @MockBean
    private AzureContainerRegistryService acrService;

    @Test
    @DisplayName("GET /api/azure/status should return Azure connection status")
    void shouldReturnAzureStatus() throws Exception {
        AzureStatusResponse status = new AzureStatusResponse(
                true,
                AzureConnectionStatus.CONNECTED,
                "00000000-0000-0000-0000-000000000000",
                "11111111-1111-1111-1111-111111111111",
                "rg-cloudship-dev",
                "eastus",
                "Azure connection established",
                OffsetDateTime.now()
        );
        when(azureInfrastructureService.getAzureStatus()).thenReturn(status);

        mockMvc.perform(get("/api/azure/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.resourceGroup").value("rg-cloudship-dev"))
                .andExpect(jsonPath("$.location").value("eastus"));

        // Also test alternate path /api/infrastructure/azure/status
        mockMvc.perform(get("/api/infrastructure/azure/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"));
    }

    @Test
    @DisplayName("GET /api/azure/resource-group should return resource group details")
    void shouldReturnResourceGroup() throws Exception {
        AzureResourceGroupResponse response = new AzureResourceGroupResponse(
                "rg-cloudship-dev",
                "eastus",
                "Succeeded",
                AzureResourceStatus.READY,
                "Resource Group verified",
                Map.of("Project", "CloudShip")
        );
        when(azureInfrastructureService.getResourceGroup()).thenReturn(response);

        mockMvc.perform(get("/api/azure/resource-group").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("rg-cloudship-dev"))
                .andExpect(jsonPath("$.location").value("eastus"))
                .andExpect(jsonPath("$.provisioningState").value("Succeeded"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("GET /api/azure/network should return virtual network and subnet details")
    void shouldReturnNetwork() throws Exception {
        AzureNetworkResponse response = new AzureNetworkResponse(
                "vnet-cloudship",
                "snet-cloudship",
                "eastus",
                "Succeeded",
                List.of("10.0.0.0/16"),
                List.of("10.0.1.0/24"),
                AzureResourceStatus.READY,
                AzureResourceStatus.READY,
                "Network verified"
        );
        when(azureInfrastructureService.getNetwork()).thenReturn(response);

        mockMvc.perform(get("/api/azure/network").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vnetName").value("vnet-cloudship"))
                .andExpect(jsonPath("$.subnetName").value("snet-cloudship"))
                .andExpect(jsonPath("$.addressSpaces[0]").value("10.0.0.0/16"))
                .andExpect(jsonPath("$.subnetAddressPrefixes[0]").value("10.0.1.0/24"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("GET /api/azure/registry should return container registry details")
    void shouldReturnRegistry() throws Exception {
        AzureRegistryResponse response = new AzureRegistryResponse(
                "cloudshipcr",
                "cloudshipcr.azurecr.io",
                "eastus",
                "Basic",
                true,
                "Succeeded",
                AzureResourceStatus.READY,
                "Registry verified"
        );
        when(acrService.getRegistryDetails()).thenReturn(response);

        mockMvc.perform(get("/api/azure/registry").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cloudshipcr"))
                .andExpect(jsonPath("$.loginServer").value("cloudshipcr.azurecr.io"))
                .andExpect(jsonPath("$.sku").value("Basic"))
                .andExpect(jsonPath("$.adminUserEnabled").value(true))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/registry/repositories should list repositories")
    void shouldReturnRepositories() throws Exception {
        AcrRepositoryResponse repos = new AcrRepositoryResponse(
                "cloudshipcr",
                "cloudshipcr.azurecr.io",
                List.of("cloudship/backend", "cloudship/frontend"),
                AzureResourceStatus.READY,
                "Repositories retrieved"
        );
        when(acrService.listRepositories()).thenReturn(repos);

        mockMvc.perform(get("/api/infrastructure/azure/registry/repositories").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registryName").value("cloudshipcr"))
                .andExpect(jsonPath("$.loginServer").value("cloudshipcr.azurecr.io"))
                .andExpect(jsonPath("$.repositoryCount").value(2))
                .andExpect(jsonPath("$.repositories[0]").value("cloudship/backend"));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/registry/images should list images")
    void shouldReturnImages() throws Exception {
        AcrImageResponse img = new AcrImageResponse(
                "cloudship/backend",
                "a3f9c21",
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "cloudshipcr.azurecr.io/cloudship/backend:a3f9c21",
                "cloudshipcr",
                "cloudshipcr.azurecr.io",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                AzureResourceStatus.READY,
                "Verified"
        );
        when(acrService.listImages("cloudship/backend")).thenReturn(List.of(img));

        mockMvc.perform(get("/api/infrastructure/azure/registry/images?repository=cloudship/backend").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository").value("cloudship/backend"))
                .andExpect(jsonPath("$[0].tag").value("a3f9c21"))
                .andExpect(jsonPath("$[0].imageReference").value("cloudshipcr.azurecr.io/cloudship/backend:a3f9c21"));
    }

    @Test
    @DisplayName("POST /api/infrastructure/azure/registry/verify should verify image")
    void shouldVerifyImage() throws Exception {
        AcrVerificationResponse verification = new AcrVerificationResponse(
                true,
                "cloudship/backend",
                "a3f9c21",
                "sha256:abc",
                "cloudshipcr.azurecr.io",
                AzureResourceStatus.READY,
                "Image verified"
        );
        when(acrService.verifyImage("cloudship/backend", "a3f9c21", "sha256:abc")).thenReturn(verification);

        mockMvc.perform(post("/api/infrastructure/azure/registry/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repository\":\"cloudship/backend\",\"tag\":\"a3f9c21\",\"digest\":\"sha256:abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.repository").value("cloudship/backend"))
                .andExpect(jsonPath("$.tag").value("a3f9c21"))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("GET /api/azure/infrastructure should return aggregated infrastructure")
    void shouldReturnAggregatedInfrastructure() throws Exception {
        AzureStatusResponse status = new AzureStatusResponse(
                false,
                AzureConnectionStatus.NOT_CONFIGURED,
                null,
                null,
                "rg-cloudship-dev",
                "eastus",
                "Not configured",
                OffsetDateTime.now()
        );
        AzureResourceGroupResponse rg = new AzureResourceGroupResponse(
                "rg-cloudship-dev", "eastus", "NotConfigured", AzureResourceStatus.NOT_CONFIGURED, "Not configured", Map.of()
        );
        AzureNetworkResponse net = new AzureNetworkResponse(
                "vnet-cloudship", "snet-cloudship", "eastus", "NotConfigured", List.of(), List.of(), AzureResourceStatus.NOT_CONFIGURED, AzureResourceStatus.NOT_CONFIGURED, "Not configured"
        );
        AzureRegistryResponse reg = new AzureRegistryResponse(
                "cloudshipcr", null, "eastus", null, false, "NotConfigured", AzureResourceStatus.NOT_CONFIGURED, "Not configured"
        );

        AzureInfrastructureResponse infra = new AzureInfrastructureResponse(status, rg, net, reg);
        when(azureInfrastructureService.getInfrastructureOverview()).thenReturn(infra);

        mockMvc.perform(get("/api/azure/infrastructure").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.connectionStatus").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.resourceGroup.status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.network.status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.containerRegistry.status").value("NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("GET /api/azure/health should return health probe status")
    void shouldReturnAzureHealth() throws Exception {
        AzureStatusResponse status = new AzureStatusResponse(
                true,
                AzureConnectionStatus.CONNECTED,
                "sub-id",
                "tenant-id",
                "rg-cloudship-dev",
                "eastus",
                "Ready",
                OffsetDateTime.now()
        );
        when(azureInfrastructureService.getAzureStatus()).thenReturn(status);

        mockMvc.perform(get("/api/azure/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("azure-infrastructure"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.status").value("CONNECTED"))
                .andExpect(jsonPath("$.resourceGroup").value("rg-cloudship-dev"))
                .andExpect(jsonPath("$.message").value("Ready"));
    }
}
