package com.cloudship.service.azure;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool;
import com.cloudship.dto.azure.*;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AzureAksServiceImpl implements AzureAksService {

    private static final Logger log = LoggerFactory.getLogger(AzureAksServiceImpl.class);

    private final AzureClientProvider azureClientProvider;
    private final KubernetesClientProvider kubernetesClientProvider;

    public AzureAksServiceImpl(
            AzureClientProvider azureClientProvider,
            KubernetesClientProvider kubernetesClientProvider) {
        this.azureClientProvider = azureClientProvider;
        this.kubernetesClientProvider = kubernetesClientProvider;
    }

    @Override
    public AksClusterResponse getClusterDetails() {
        if (!azureClientProvider.isConfigured()) {
            return AksClusterResponse.notConfigured("Azure credentials or subscription not configured.");
        }

        String clusterName = azureClientProvider.getAksClusterName();
        String rg = azureClientProvider.getResourceGroupName();

        try {
            AzureResourceManager arm = azureClientProvider.getAzureResourceManager();
            if (arm == null) {
                return AksClusterResponse.notConnected(clusterName, "Azure Resource Manager client is unavailable.");
            }

            KubernetesCluster cluster = arm.kubernetesClusters().getByResourceGroup(rg, clusterName);
            if (cluster == null) {
                return AksClusterResponse.notFound(clusterName, rg);
            }

            int totalNodes = 0;
            int poolCount = 0;
            if (cluster.agentPools() != null) {
                poolCount = cluster.agentPools().size();
                for (KubernetesClusterAgentPool pool : cluster.agentPools().values()) {
                    totalNodes += pool.count();
                }
            }

            String powerStateStr = cluster.powerState() != null ? cluster.powerState().toString() : "Running";

            return AksClusterResponse.ready(
                    cluster.name(),
                    cluster.resourceGroupName(),
                    cluster.nodeResourceGroup(),
                    cluster.regionName(),
                    cluster.version(),
                    cluster.provisioningState(),
                    powerStateStr,
                    poolCount,
                    totalNodes,
                    cluster.fqdn(),
                    cluster.dnsPrefix()
            );
        } catch (Exception e) {
            log.warn("Failed to retrieve AKS cluster '{}' details: {}", clusterName, e.getMessage());
            return AksClusterResponse.error(clusterName, e.getMessage());
        }
    }

    @Override
    public List<KubernetesWorkloadResponse> listWorkloads(String namespace) {
        String targetNs = (namespace != null && !namespace.isBlank()) ? namespace.trim() : azureClientProvider.getK8sNamespace();
        if (targetNs == null || targetNs.isBlank()) {
            targetNs = "default";
        }

        AppsV1Api appsApi = kubernetesClientProvider.getAppsV1Api();
        if (appsApi == null) {
            log.debug("Kubernetes AppsV1Api not available; returning empty workloads list.");
            return Collections.emptyList();
        }

        try {
            V1DeploymentList deploymentList = appsApi.listNamespacedDeployment(targetNs).execute();
            if (deploymentList == null || deploymentList.getItems() == null) {
                return Collections.emptyList();
            }

            List<KubernetesWorkloadResponse> result = new ArrayList<>();
            for (V1Deployment dep : deploymentList.getItems()) {
                String name = dep.getMetadata() != null ? dep.getMetadata().getName() : "unknown";
                String ns = dep.getMetadata() != null ? dep.getMetadata().getNamespace() : targetNs;
                OffsetDateTime creationTime = dep.getMetadata() != null ? dep.getMetadata().getCreationTimestamp() : null;

                Integer desired = dep.getSpec() != null ? dep.getSpec().getReplicas() : 1;
                Integer ready = dep.getStatus() != null && dep.getStatus().getReadyReplicas() != null ? dep.getStatus().getReadyReplicas() : 0;
                Integer updated = dep.getStatus() != null && dep.getStatus().getUpdatedReplicas() != null ? dep.getStatus().getUpdatedReplicas() : 0;
                Integer available = dep.getStatus() != null && dep.getStatus().getAvailableReplicas() != null ? dep.getStatus().getAvailableReplicas() : 0;

                String image = "--";
                if (dep.getSpec() != null && dep.getSpec().getTemplate() != null
                        && dep.getSpec().getTemplate().getSpec() != null
                        && dep.getSpec().getTemplate().getSpec().getContainers() != null
                        && !dep.getSpec().getTemplate().getSpec().getContainers().isEmpty()) {
                    image = dep.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
                }

                String rolloutStatus = (desired != null && ready >= desired) ? "SUCCESS" : "RUNNING";

                result.add(new KubernetesWorkloadResponse(
                        name, ns, desired, ready, updated, available, image, rolloutStatus, creationTime
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to query Kubernetes workloads in namespace '{}': {}", targetNs, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<KubernetesPodResponse> listPods(String namespace, String deploymentName) {
        String targetNs = (namespace != null && !namespace.isBlank()) ? namespace.trim() : azureClientProvider.getK8sNamespace();
        if (targetNs == null || targetNs.isBlank()) {
            targetNs = "default";
        }

        CoreV1Api coreApi = kubernetesClientProvider.getCoreV1Api();
        if (coreApi == null) {
            log.debug("Kubernetes CoreV1Api not available; returning empty pods list.");
            return Collections.emptyList();
        }

        try {
            var callBuilder = coreApi.listNamespacedPod(targetNs);
            if (deploymentName != null && !deploymentName.isBlank()) {
                callBuilder.labelSelector("app=" + deploymentName.trim());
            }

            V1PodList podList = callBuilder.execute();
            if (podList == null || podList.getItems() == null) {
                return Collections.emptyList();
            }

            List<KubernetesPodResponse> result = new ArrayList<>();
            for (V1Pod pod : podList.getItems()) {
                String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown";
                String ns = pod.getMetadata() != null ? pod.getMetadata().getNamespace() : targetNs;
                String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : "--";
                String phase = pod.getStatus() != null && pod.getStatus().getPhase() != null ? pod.getStatus().getPhase() : "Unknown";

                int readyContainers = 0;
                int totalContainers = 0;
                int restartCount = 0;
                String statusMessage = "";

                if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                    totalContainers = pod.getStatus().getContainerStatuses().size();
                    for (V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        if (Boolean.TRUE.equals(cs.getReady())) {
                            readyContainers++;
                        }
                        if (cs.getRestartCount() != null) {
                            restartCount += cs.getRestartCount();
                        }
                        if (cs.getState() != null && cs.getState().getWaiting() != null) {
                            statusMessage = cs.getState().getWaiting().getReason();
                        }
                    }
                }

                String readyStr = readyContainers + "/" + totalContainers;
                OffsetDateTime startTime = pod.getStatus() != null ? pod.getStatus().getStartTime() : null;
                String age = formatAge(startTime);

                result.add(new KubernetesPodResponse(
                        podName, ns, nodeName, phase, readyStr, readyContainers, totalContainers, restartCount, startTime, age, statusMessage
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to query Kubernetes pods in namespace '{}': {}", targetNs, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<KubernetesServiceResponse> listServices(String namespace) {
        String targetNs = (namespace != null && !namespace.isBlank()) ? namespace.trim() : azureClientProvider.getK8sNamespace();
        if (targetNs == null || targetNs.isBlank()) {
            targetNs = "default";
        }

        CoreV1Api coreApi = kubernetesClientProvider.getCoreV1Api();
        if (coreApi == null) {
            return Collections.emptyList();
        }

        try {
            V1ServiceList svcList = coreApi.listNamespacedService(targetNs).execute();
            if (svcList == null || svcList.getItems() == null) {
                return Collections.emptyList();
            }

            List<KubernetesServiceResponse> result = new ArrayList<>();
            for (V1Service svc : svcList.getItems()) {
                String name = svc.getMetadata() != null ? svc.getMetadata().getName() : "unknown";
                String ns = svc.getMetadata() != null ? svc.getMetadata().getNamespace() : targetNs;
                String type = svc.getSpec() != null ? svc.getSpec().getType() : "ClusterIP";
                String clusterIp = svc.getSpec() != null ? svc.getSpec().getClusterIP() : "--";

                List<String> ports = new ArrayList<>();
                if (svc.getSpec() != null && svc.getSpec().getPorts() != null) {
                    for (V1ServicePort sp : svc.getSpec().getPorts()) {
                        ports.add(sp.getPort() + (sp.getTargetPort() != null ? ":" + sp.getTargetPort() : ""));
                    }
                }

                Map<String, String> selector = svc.getSpec() != null ? svc.getSpec().getSelector() : Collections.emptyMap();

                result.add(new KubernetesServiceResponse(name, ns, type, clusterIp, ports, selector));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to query Kubernetes services in namespace '{}': {}", targetNs, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String formatAge(OffsetDateTime startTime) {
        if (startTime == null) {
            return "--";
        }
        Duration duration = Duration.between(startTime, OffsetDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h";
        } else {
            return (seconds / 86400) + "d";
        }
    }
}
