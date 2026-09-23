package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
public class KubernetesClientProviderImpl implements KubernetesClientProvider {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientProviderImpl.class);

    private final AzureClientProvider azureClientProvider;

    private volatile ApiClient apiClient;
    private volatile AppsV1Api appsV1Api;
    private volatile CoreV1Api coreV1Api;
    private volatile boolean connected = false;
    private volatile String statusMessage = "Not initialized";

    public KubernetesClientProviderImpl(AzureClientProvider azureClientProvider) {
        this.azureClientProvider = azureClientProvider;
    }

    @Override
    public boolean isConfigured() {
        if (azureClientProvider == null) {
            return false;
        }
        String kubeconfigPath = azureClientProvider.getK8sKubeconfigPath();
        if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
            return true;
        }
        return azureClientProvider.isConfigured()
                && azureClientProvider.getAksClusterName() != null
                && !azureClientProvider.getAksClusterName().trim().isEmpty();
    }

    @Override
    public boolean isConnected() {
        if (!isConfigured()) {
            return false;
        }
        if (this.apiClient != null && this.connected) {
            return true;
        }
        ensureClientInitialized();
        return this.connected;
    }

    @Override
    public synchronized ApiClient getApiClient() {
        ensureClientInitialized();
        return this.apiClient;
    }

    @Override
    public synchronized AppsV1Api getAppsV1Api() {
        if (getApiClient() == null) {
            return null;
        }
        if (this.appsV1Api == null) {
            this.appsV1Api = new AppsV1Api(this.apiClient);
        }
        return this.appsV1Api;
    }

    @Override
    public synchronized CoreV1Api getCoreV1Api() {
        if (getApiClient() == null) {
            return null;
        }
        if (this.coreV1Api == null) {
            this.coreV1Api = new CoreV1Api(this.apiClient);
        }
        return this.coreV1Api;
    }

    @Override
    public String getClusterName() {
        return azureClientProvider != null ? azureClientProvider.getAksClusterName() : "aks-cloudship-dev";
    }

    @Override
    public String getNamespace() {
        return azureClientProvider != null && azureClientProvider.getK8sNamespace() != null
                ? azureClientProvider.getK8sNamespace()
                : "default";
    }

    @Override
    public String getStatusMessage() {
        return this.statusMessage;
    }

    private synchronized void ensureClientInitialized() {
        if (this.apiClient != null) {
            return;
        }

        if (!isConfigured()) {
            this.connected = false;
            this.statusMessage = "Kubernetes / AKS integration is not configured or disabled.";
            return;
        }

        // Option 1: Direct kubeconfig file path
        String kubeconfigPath = azureClientProvider.getK8sKubeconfigPath();
        if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
            try {
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeconfigPath.trim(), StandardCharsets.UTF_8));
                this.apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
                this.connected = true;
                this.statusMessage = "Connected via local kubeconfig: " + kubeconfigPath;
                log.info("Initialized Kubernetes client from file: {}", kubeconfigPath);
                return;
            } catch (Exception e) {
                log.warn("Failed to initialize Kubernetes client from file {}: {}", kubeconfigPath, e.getMessage());
                this.connected = false;
                this.statusMessage = "Failed to load kubeconfig from file: " + e.getMessage();
            }
        }

        // Option 2: Azure Resource Manager AKS credentials
        try {
            AzureResourceManager arm = azureClientProvider.getAzureResourceManager();
            if (arm == null) {
                this.connected = false;
                this.statusMessage = "Azure Resource Manager client is not available.";
                return;
            }

            String rg = azureClientProvider.getResourceGroupName();
            String clusterName = azureClientProvider.getAksClusterName();
            log.info("Retrieving AKS cluster admin credentials from Azure: RG='{}', Cluster='{}'", rg, clusterName);

            KubernetesCluster cluster = arm.kubernetesClusters().getByResourceGroup(rg, clusterName);
            if (cluster == null) {
                this.connected = false;
                this.statusMessage = String.format("AKS cluster '%s' not found in resource group '%s'", clusterName, rg);
                log.warn("AKS cluster '{}' not found in resource group '{}'", clusterName, rg);
                return;
            }

            byte[] adminKubeconfig = cluster.adminKubeconfigContent();
            if (adminKubeconfig == null || adminKubeconfig.length == 0) {
                adminKubeconfig = cluster.userKubeconfigContent();
            }

            if (adminKubeconfig != null && adminKubeconfig.length > 0) {
                String configContent = new String(adminKubeconfig, StandardCharsets.UTF_8);
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new StringReader(configContent));
                this.apiClient = ClientBuilder.kubeconfig(kubeConfig).build();
                this.connected = true;
                this.statusMessage = String.format("Connected to AKS cluster '%s' in RG '%s'", clusterName, rg);
                log.info("Successfully initialized Kubernetes ApiClient for AKS cluster '{}'", clusterName);
            } else {
                this.connected = false;
                this.statusMessage = "Cluster credentials content was empty.";
                log.warn("No kubeconfig content returned for AKS cluster '{}'", clusterName);
            }
        } catch (Exception e) {
            this.connected = false;
            this.statusMessage = "Failed to retrieve AKS cluster credentials: " + e.getMessage();
            log.warn("Failed to connect to AKS cluster: {}", e.getMessage());
        }
    }
}
