package com.cloudship.service.azure;

import com.cloudship.entity.Deployment;
import com.cloudship.entity.DeploymentStatus;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class KubernetesDeploymentServiceImpl implements KubernetesDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesDeploymentServiceImpl.class);

    private final KubernetesClientProvider clientProvider;

    public KubernetesDeploymentServiceImpl(KubernetesClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Override
    public boolean applyDeployment(Deployment deployment) {
        if (deployment == null) {
            return false;
        }

        String namespace = deployment.getNamespace() != null && !deployment.getNamespace().isBlank()
                ? deployment.getNamespace().trim()
                : clientProvider.getNamespace();
        if (namespace == null || namespace.isBlank()) {
            namespace = "default";
        }
        deployment.setNamespace(namespace);

        String depName = deployment.getDeploymentName();
        if (depName == null || depName.isBlank()) {
            depName = deployment.getProject() != null ? deployment.getProject().getName().toLowerCase().replaceAll("[^a-z0-9-]", "-") : "cloudship-service";
            deployment.setDeploymentName(depName);
        }

        String svcName = deployment.getServiceName();
        if (svcName == null || svcName.isBlank()) {
            svcName = depName + "-service";
            deployment.setServiceName(svcName);
        }

        AppsV1Api appsApi = clientProvider.getAppsV1Api();
        CoreV1Api coreApi = clientProvider.getCoreV1Api();

        if (appsApi == null || coreApi == null) {
            String msg = "Kubernetes client not available: " + clientProvider.getStatusMessage();
            log.warn(msg);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setRolloutStatus("FAILED");
            deployment.setErrorMessage(msg);
            deployment.setCompletedAt(OffsetDateTime.now());
            return false;
        }

        try {
            // 1. Ensure Namespace exists
            ensureNamespaceExists(coreApi, namespace);

            // 2. Build and apply native Kubernetes Deployment resource
            V1Deployment k8sDep = buildV1Deployment(deployment, namespace, depName);
            applyV1Deployment(appsApi, namespace, depName, k8sDep);

            // 3. Build and apply native Kubernetes Service resource
            V1Service k8sSvc = buildV1Service(deployment, namespace, depName, svcName);
            applyV1Service(coreApi, namespace, svcName, k8sSvc);

            // 4. Inspect initial rollout status
            checkRolloutStatus(deployment);

            log.info("Successfully applied Kubernetes resources for deployment '{}' in namespace '{}'", depName, namespace);
            return true;
        } catch (ApiException e) {
            String errMsg = String.format("Kubernetes API error (HTTP %d): %s - %s", e.getCode(), e.getMessage(), e.getResponseBody());
            log.error(errMsg, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setRolloutStatus("FAILED");
            deployment.setErrorMessage(errMsg);
            deployment.setCompletedAt(OffsetDateTime.now());
            return false;
        } catch (Exception e) {
            String errMsg = "Unexpected error applying Kubernetes deployment: " + e.getMessage();
            log.error(errMsg, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deployment.setRolloutStatus("FAILED");
            deployment.setErrorMessage(errMsg);
            deployment.setCompletedAt(OffsetDateTime.now());
            return false;
        }
    }

    @Override
    public void checkRolloutStatus(Deployment deployment) {
        if (deployment == null) {
            return;
        }

        AppsV1Api appsApi = clientProvider.getAppsV1Api();
        if (appsApi == null) {
            return;
        }

        String namespace = deployment.getNamespace();
        String depName = deployment.getDeploymentName();

        try {
            V1Deployment k8sDep = appsApi.readNamespacedDeployment(depName, namespace).execute();
            if (k8sDep == null || k8sDep.getStatus() == null) {
                return;
            }

            V1DeploymentStatus status = k8sDep.getStatus();
            int desired = deployment.getReplicas() != null ? deployment.getReplicas() : 1;
            int ready = status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
            int updated = status.getUpdatedReplicas() != null ? status.getUpdatedReplicas() : 0;
            int available = status.getAvailableReplicas() != null ? status.getAvailableReplicas() : 0;

            deployment.setReadyReplicas(ready);
            deployment.setUpdatedReplicas(updated);
            deployment.setAvailableReplicas(available);

            if (ready >= desired && updated >= desired) {
                deployment.setRolloutStatus("SUCCESS");
                deployment.setStatus(DeploymentStatus.SUCCESS);
                if (deployment.getCompletedAt() == null) {
                    deployment.setCompletedAt(OffsetDateTime.now());
                }
            } else {
                deployment.setRolloutStatus("RUNNING");
                deployment.setStatus(DeploymentStatus.RUNNING);
            }
        } catch (Exception e) {
            log.debug("Could not inspect rollout status for '{}/{}': {}", namespace, depName, e.getMessage());
        }
    }

    private void ensureNamespaceExists(CoreV1Api coreApi, String namespace) throws ApiException {
        try {
            coreApi.readNamespace(namespace).execute();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Namespace '{}' does not exist; creating it now", namespace);
                V1Namespace ns = new V1Namespace()
                        .metadata(new V1ObjectMeta().name(namespace));
                coreApi.createNamespace(ns).execute();
            } else {
                throw e;
            }
        }
    }

    private V1Deployment buildV1Deployment(Deployment deployment, String namespace, String depName) {
        Map<String, String> labels = Map.of(
                "app", depName,
                "app.kubernetes.io/name", depName,
                "app.kubernetes.io/managed-by", "cloudship"
        );

        String fullImage = resolveContainerImage(deployment);
        int replicas = deployment.getReplicas() != null ? deployment.getReplicas() : 1;

        V1Container container = new V1Container()
                .name(depName)
                .image(fullImage)
                .imagePullPolicy("IfNotPresent")
                .ports(List.of(new V1ContainerPort().containerPort(8088).name("http")))
                .resources(new V1ResourceRequirements()
                        .requests(Map.of("cpu", new Quantity("100m"), "memory", new Quantity("256Mi")))
                        .limits(Map.of("cpu", new Quantity("500m"), "memory", new Quantity("512Mi"))))
                .livenessProbe(new V1Probe()
                        .httpGet(new V1HTTPGetAction().path("/actuator/health/liveness").port(new IntOrString(8088)))
                        .initialDelaySeconds(30)
                        .periodSeconds(10))
                .readinessProbe(new V1Probe()
                        .httpGet(new V1HTTPGetAction().path("/actuator/health/readiness").port(new IntOrString(8088)))
                        .initialDelaySeconds(15)
                        .periodSeconds(5));

        V1PodTemplateSpec template = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(new V1PodSpec().containers(List.of(container)));

        V1RollingUpdateDeployment rollingUpdate = new V1RollingUpdateDeployment()
                .maxSurge(new IntOrString("25%"))
                .maxUnavailable(new IntOrString("25%"));

        V1DeploymentStrategy strategy = new V1DeploymentStrategy()
                .type("RollingUpdate")
                .rollingUpdate(rollingUpdate);

        return new V1Deployment()
                .metadata(new V1ObjectMeta().name(depName).namespace(namespace).labels(labels))
                .spec(new V1DeploymentSpec()
                        .replicas(replicas)
                        .selector(new V1LabelSelector().matchLabels(Map.of("app", depName)))
                        .strategy(strategy)
                        .template(template));
    }

    private void applyV1Deployment(AppsV1Api appsApi, String namespace, String depName, V1Deployment k8sDep) throws ApiException {
        try {
            appsApi.readNamespacedDeployment(depName, namespace).execute();
            log.info("Updating existing Kubernetes Deployment '{}/{}'", namespace, depName);
            appsApi.replaceNamespacedDeployment(depName, namespace, k8sDep).execute();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Creating new Kubernetes Deployment '{}/{}'", namespace, depName);
                appsApi.createNamespacedDeployment(namespace, k8sDep).execute();
            } else {
                throw e;
            }
        }
    }

    private V1Service buildV1Service(Deployment deployment, String namespace, String depName, String svcName) {
        Map<String, String> labels = Map.of(
                "app", depName,
                "app.kubernetes.io/name", depName,
                "app.kubernetes.io/managed-by", "cloudship"
        );

        return new V1Service()
                .metadata(new V1ObjectMeta().name(svcName).namespace(namespace).labels(labels))
                .spec(new V1ServiceSpec()
                        .type("ClusterIP")
                        .selector(Map.of("app", depName))
                        .ports(List.of(new V1ServicePort().name("http").port(8088).targetPort(new IntOrString(8088)))));
    }

    private void applyV1Service(CoreV1Api coreApi, String namespace, String svcName, V1Service k8sSvc) throws ApiException {
        try {
            V1Service existing = coreApi.readNamespacedService(svcName, namespace).execute();
            if (existing != null && existing.getSpec() != null && existing.getSpec().getClusterIP() != null) {
                k8sSvc.getSpec().clusterIP(existing.getSpec().getClusterIP());
            }
            log.info("Updating existing Kubernetes Service '{}/{}'", namespace, svcName);
            coreApi.replaceNamespacedService(svcName, namespace, k8sSvc).execute();
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Creating new Kubernetes Service '{}/{}'", namespace, svcName);
                coreApi.createNamespacedService(namespace, k8sSvc).execute();
            } else {
                throw e;
            }
        }
    }

    private String resolveContainerImage(Deployment deployment) {
        String imageName = deployment.getImageName();
        String tag = deployment.getImageTag();
        String digest = deployment.getImageDigest();

        if (imageName == null || imageName.isBlank()) {
            imageName = "cloudship/backend";
        }
        if (digest != null && !digest.isBlank()) {
            return imageName + "@" + digest;
        }
        if (tag != null && !tag.isBlank()) {
            return imageName + ":" + tag;
        }
        return imageName + ":latest";
    }
}
