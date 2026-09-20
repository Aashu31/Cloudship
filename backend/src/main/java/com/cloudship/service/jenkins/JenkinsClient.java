package com.cloudship.service.jenkins;

import java.util.Map;

public interface JenkinsClient {

    boolean isAvailable();

    JenkinsTriggerResult triggerJob(String jobName, Map<String, String> parameters);

    JenkinsBuildDetails getBuildDetails(String jobName, int buildNumber);

    String getBaseUrl();
}
