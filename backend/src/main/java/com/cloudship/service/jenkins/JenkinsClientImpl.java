package com.cloudship.service.jenkins;

import com.cloudship.entity.CIBuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
public class JenkinsClientImpl implements JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClientImpl.class);

    private final String baseUrl;
    private final String username;
    private final String apiToken;
    private final String defaultJobName;
    private final RestClient restClient;

    public JenkinsClientImpl(
            @Value("${cloudship.jenkins.base-url:http://localhost:8080}") String baseUrl,
            @Value("${cloudship.jenkins.username:}") String username,
            @Value("${cloudship.jenkins.api-token:}") String apiToken,
            @Value("${cloudship.jenkins.default-job-name:cloudship-ci}") String defaultJobName) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
        this.username = username != null ? username.trim() : "";
        this.apiToken = apiToken != null ? apiToken.trim() : "";
        this.defaultJobName = defaultJobName != null && !defaultJobName.isBlank() ? defaultJobName.trim() : "cloudship-ci";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "CloudShip-CI-Client");

        if (!this.username.isEmpty() && !this.apiToken.isEmpty()) {
            String credentials = this.username + ":" + this.apiToken;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }

        this.restClient = builder.build();
    }

    @Override
    public boolean isAvailable() {
        try {
            var response = restClient.get()
                    .uri("/api/json")
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Jenkins health check failed at {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public JenkinsTriggerResult triggerJob(String jobName, Map<String, String> parameters) {
        String targetJob = (jobName != null && !jobName.isBlank()) ? jobName.trim() : this.defaultJobName;
        try {
            boolean hasParams = parameters != null && !parameters.isEmpty();
            String path = hasParams ? "/job/{jobName}/buildWithParameters" : "/job/{jobName}/build";

            var requestSpec = restClient.post().uri(path, targetJob);

            Map<String, String> crumb = fetchCrumb();
            if (crumb != null && crumb.containsKey("crumbRequestField") && crumb.containsKey("crumb")) {
                requestSpec.header(crumb.get("crumbRequestField"), crumb.get("crumb"));
            }

            if (hasParams) {
                var formData = new LinkedMultiValueMap<String, String>();
                parameters.forEach(formData::add);
                requestSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData);
            }

            var response = requestSpec.retrieve().toBodilessEntity();
            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            log.info("Triggered Jenkins job {} (status: {}, location: {})", targetJob, response.getStatusCode(), location);
            return new JenkinsTriggerResult(true, null, location, "Job triggered successfully");
        } catch (Exception e) {
            log.warn("Jenkins trigger failed for job {}: {}", targetJob, e.getMessage());
            return new JenkinsTriggerResult(false, null, null, "Jenkins unavailable or error: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public JenkinsBuildDetails getBuildDetails(String jobName, int buildNumber) {
        String targetJob = (jobName != null && !jobName.isBlank()) ? jobName.trim() : this.defaultJobName;
        try {
            Map<String, Object> resp = restClient.get()
                    .uri("/job/{jobName}/{buildNumber}/api/json", targetJob, buildNumber)
                    .retrieve()
                    .body(Map.class);

            if (resp != null) {
                String result = (String) resp.get("result");
                Number duration = (Number) resp.get("duration");
                Boolean building = (Boolean) resp.get("building");

                CIBuildStatus status = CIBuildStatus.RUNNING;
                if (Boolean.TRUE.equals(building)) {
                    status = CIBuildStatus.RUNNING;
                } else if ("SUCCESS".equalsIgnoreCase(result)) {
                    status = CIBuildStatus.SUCCESS;
                } else if ("FAILURE".equalsIgnoreCase(result)) {
                    status = CIBuildStatus.FAILED;
                } else if ("ABORTED".equalsIgnoreCase(result)) {
                    status = CIBuildStatus.ABORTED;
                }

                return new JenkinsBuildDetails(buildNumber, status, duration != null ? duration.longValue() : 0L, result);
            }
        } catch (Exception e) {
            log.debug("Could not retrieve build details for job {} #{}: {}", targetJob, buildNumber, e.getMessage());
        }
        return null;
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchCrumb() {
        try {
            return restClient.get()
                    .uri("/crumbIssuer/api/json")
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.debug("No CSRF crumb issuer required or available: {}", e.getMessage());
            return null;
        }
    }
}
