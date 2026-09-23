package com.cloudship.service.azure;

import com.cloudship.entity.Deployment;

public interface KubernetesDeploymentService {

    boolean applyDeployment(Deployment deployment);

    void checkRolloutStatus(Deployment deployment);
}
