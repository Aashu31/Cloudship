package com.cloudship.controller;

import com.cloudship.dto.azure.*;
import com.cloudship.service.azure.AzureAksService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AzureAksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AzureAksService aksService;

    @Test
    @DisplayName("GET /api/infrastructure/azure/aks should return AKS cluster metadata")
    void testGetClusterDetails() throws Exception {
        AksClusterResponse response = AksClusterResponse.ready(
                "aks-cloudship-dev",
                "rg-cloudship-dev",
                "MC_rg-cloudship-dev_aks-cloudship-dev_eastus",
                "eastus",
                "1.28.5",
                "Succeeded",
                "Running",
                1,
                3,
                "aks-dns.hcp.eastus.azmk8s.io",
                "aks-dns"
        );
        when(aksService.getClusterDetails()).thenReturn(response);

        mockMvc.perform(get("/api/infrastructure/azure/aks").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("aks-cloudship-dev"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.kubernetesVersion").value("1.28.5"))
                .andExpect(jsonPath("$.totalNodes").value(3))
                .andExpect(jsonPath("$.fqdn").value("aks-dns.hcp.eastus.azmk8s.io"));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/aks/health should return health probe map")
    void testGetClusterHealth() throws Exception {
        AksClusterResponse response = AksClusterResponse.ready(
                "aks-cloudship-dev", "rg-cloudship-dev", "MC_rg", "eastus",
                "1.28.5", "Succeeded", "Running", 1, 3, "fqdn", "dns"
        );
        when(aksService.getClusterDetails()).thenReturn(response);

        mockMvc.perform(get("/api/infrastructure/azure/aks/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("azure-kubernetes-service"))
                .andExpect(jsonPath("$.clusterName").value("aks-cloudship-dev"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.totalNodes").value(3));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/aks/workloads should return active deployments")
    void testGetWorkloads() throws Exception {
        KubernetesWorkloadResponse workload = new KubernetesWorkloadResponse(
                "cloudship-backend", "default", 2, 2, 2, 2,
                "cloudshipcr.azurecr.io/cloudship/backend:v1", "SUCCESS", OffsetDateTime.now()
        );
        when(aksService.listWorkloads("default")).thenReturn(List.of(workload));

        mockMvc.perform(get("/api/infrastructure/azure/aks/workloads?namespace=default").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("cloudship-backend"))
                .andExpect(jsonPath("$[0].desiredReplicas").value(2))
                .andExpect(jsonPath("$[0].readyReplicas").value(2))
                .andExpect(jsonPath("$[0].rolloutStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/aks/pods should return active pods list")
    void testGetPods() throws Exception {
        KubernetesPodResponse pod = new KubernetesPodResponse(
                "cloudship-backend-789-xyz", "default", "aks-nodepool1",
                "Running", "1/1", 1, 1, 0, OffsetDateTime.now(), "5m", ""
        );
        when(aksService.listPods("default", "cloudship-backend")).thenReturn(List.of(pod));

        mockMvc.perform(get("/api/infrastructure/azure/aks/pods?namespace=default&deployment=cloudship-backend").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("cloudship-backend-789-xyz"))
                .andExpect(jsonPath("$[0].phase").value("Running"))
                .andExpect(jsonPath("$[0].ready").value("1/1"));
    }

    @Test
    @DisplayName("GET /api/infrastructure/azure/aks/services should return Kubernetes services")
    void testGetServices() throws Exception {
        KubernetesServiceResponse svc = new KubernetesServiceResponse(
                "cloudship-backend-service", "default", "ClusterIP", "10.0.12.34",
                List.of("8088:8088"), Map.of("app", "cloudship-backend")
        );
        when(aksService.listServices("default")).thenReturn(List.of(svc));

        mockMvc.perform(get("/api/infrastructure/azure/aks/services?namespace=default").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("cloudship-backend-service"))
                .andExpect(jsonPath("$[0].type").value("ClusterIP"))
                .andExpect(jsonPath("$[0].clusterIp").value("10.0.12.34"));
    }
}
