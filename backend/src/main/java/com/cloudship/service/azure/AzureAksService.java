package com.cloudship.service.azure;

import com.cloudship.dto.azure.AksClusterResponse;
import com.cloudship.dto.azure.KubernetesPodResponse;
import com.cloudship.dto.azure.KubernetesServiceResponse;
import com.cloudship.dto.azure.KubernetesWorkloadResponse;

import java.util.List;

public interface AzureAksService {

    AksClusterResponse getClusterDetails();

    List<KubernetesWorkloadResponse> listWorkloads(String namespace);

    List<KubernetesPodResponse> listPods(String namespace, String deploymentName);

    List<KubernetesServiceResponse> listServices(String namespace);
}
