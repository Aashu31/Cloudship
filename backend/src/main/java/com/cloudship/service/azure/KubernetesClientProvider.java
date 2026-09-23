package com.cloudship.service.azure;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface KubernetesClientProvider {

    boolean isConfigured();

    boolean isConnected();

    ApiClient getApiClient();

    AppsV1Api getAppsV1Api();

    CoreV1Api getCoreV1Api();

    String getClusterName();

    String getNamespace();

    String getStatusMessage();
}
