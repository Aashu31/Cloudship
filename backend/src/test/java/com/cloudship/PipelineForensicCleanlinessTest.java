package com.cloudship;

import com.cloudship.config.AzureProperties;
import com.cloudship.dto.azure.AzureResourceStatus;
import com.cloudship.service.azure.AzureClientProviderImpl;
import com.cloudship.service.azure.AzureContainerRegistryService;
import com.cloudship.service.azure.AzureInfrastructureService;
import com.cloudship.service.azure.KubernetesClientProviderImpl;
import com.cloudship.service.jenkins.JenkinsClientImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineForensicCleanlinessTest {

    @Test
    @DisplayName("23. DeepSeek references are completely absent from codebase and configuration")
    void testDeepSeekReferencesAbsent() throws IOException {
        Path repoRoot = Path.of("..").toRealPath();
        List<String> deepSeekKeywords = List.of(
                "deepseek",
                "DEEPSEEK",
                "deepseek-api",
                "DEEPSEEK_API_KEY",
                "deepseek-chat",
                "deepseek-coder"
        );

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            List<Path> offendingFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(".git") && !p.toString().contains("target") && !p.getFileName().toString().equals("PipelineForensicCleanlinessTest.java"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            return deepSeekKeywords.stream().anyMatch(content::contains);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList();

            assertThat(offendingFiles)
                    .withFailMessage("Found unexpected DeepSeek references in files: %s", offendingFiles)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("24. NVIDIA references are completely absent from codebase and configuration")
    void testNvidiaReferencesAbsent() throws IOException {
        Path repoRoot = Path.of("..").toRealPath();
        List<String> nvidiaKeywords = List.of(
                "integrate.api.nvidia.com",
                "NVIDIA_API_KEY",
                "nemotron",
                "Nemotron",
                "NVIDIA_NIM",
                "nvidia/nemotron"
        );

        try (Stream<Path> paths = Files.walk(repoRoot)) {
            List<Path> offendingFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().contains(".git") && !p.toString().contains("target") && !p.getFileName().toString().equals("PipelineForensicCleanlinessTest.java"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            return nvidiaKeywords.stream().anyMatch(content::contains);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList();

            assertThat(offendingFiles)
                    .withFailMessage("Found unexpected NVIDIA references in files: %s", offendingFiles)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("19. Missing Azure credentials reports NOT_CONFIGURED / honest unavailable state")
    void testMissingAzureCredentialsHandledHonest() {
        AzureProperties props = new AzureProperties();
        props.setEnabled(false);
        AzureClientProviderImpl provider = new AzureClientProviderImpl(props);

        assertThat(provider.isConfigured()).isFalse();
        assertThat(provider.getAzureResourceManager()).isNull();

        AzureInfrastructureService infraService = new AzureInfrastructureService(provider);
        var res = infraService.getAzureStatus();
        assertThat(res.getConnectionStatus()).isEqualTo(com.cloudship.dto.azure.AzureConnectionStatus.NOT_CONFIGURED);
        assertThat(res.getMessage()).contains("Azure integration is not configured");
    }

    @Test
    @DisplayName("20. Missing Jenkins credentials handled safely without exceptions")
    void testMissingJenkinsCredentialsHandledSafely() {
        JenkinsClientImpl jenkinsClient = new JenkinsClientImpl("http://localhost:8080", "", "", "cloudship-ci");
        assertThat(jenkinsClient.isAvailable()).isFalse();
        var triggerResult = jenkinsClient.triggerJob("cloudship-ci", null);
        assertThat(triggerResult.isSuccess()).isFalse();
        assertThat(triggerResult.getMessage()).contains("Jenkins unavailable");
    }

    @Test
    @DisplayName("21. Missing ACR registry reports honest NOT_CONFIGURED state")
    void testMissingAcrHandledSafely() {
        AzureProperties props = new AzureProperties();
        props.setEnabled(false);
        AzureClientProviderImpl provider = new AzureClientProviderImpl(props);

        AzureContainerRegistryService acrService = new AzureContainerRegistryService(provider, props, null);
        var details = acrService.getRegistryDetails();
        assertThat(details.getStatus()).isEqualTo(AzureResourceStatus.NOT_CONFIGURED);
    }

    @Test
    @DisplayName("22. Missing AKS cluster reports NOT_CONFIGURED state")
    void testMissingAksHandledSafely() {
        AzureProperties props = new AzureProperties();
        props.setEnabled(false);
        AzureClientProviderImpl provider = new AzureClientProviderImpl(props);

        KubernetesClientProviderImpl k8sProvider = new KubernetesClientProviderImpl(provider);
        assertThat(k8sProvider.isConnected()).isFalse();
        assertThat(k8sProvider.getAppsV1Api()).isNull();
        assertThat(k8sProvider.getCoreV1Api()).isNull();
    }
}
