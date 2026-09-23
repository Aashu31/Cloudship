package com.cloudship.service.azure;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.cloudship.config.AzureProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AzureClientProviderImpl implements AzureClientProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureClientProviderImpl.class);

    private final AzureProperties properties;
    private volatile AzureResourceManager azureResourceManager;

    public AzureClientProviderImpl(AzureProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return properties.isEnabled() && properties.hasCredentials();
    }

    @Override
    public synchronized AzureResourceManager getAzureResourceManager() {
        if (!isConfigured()) {
            return null;
        }

        if (this.azureResourceManager != null) {
            return this.azureResourceManager;
        }

        try {
            TokenCredential credential;
            if (properties.getClientSecret() != null && !properties.getClientSecret().isBlank()) {
                credential = new ClientSecretCredentialBuilder()
                        .tenantId(properties.getTenantId())
                        .clientId(properties.getClientId())
                        .clientSecret(properties.getClientSecret())
                        .build();
            } else {
                credential = new DefaultAzureCredentialBuilder().build();
            }

            AzureProfile profile = new AzureProfile(
                    properties.getTenantId(),
                    properties.getSubscriptionId(),
                    AzureEnvironment.AZURE
            );

            this.azureResourceManager = AzureResourceManager
                    .authenticate(credential, profile)
                    .withSubscription(properties.getSubscriptionId());

            log.info("Azure Resource Manager client successfully initialized for subscription {}", properties.getSubscriptionId());
            return this.azureResourceManager;
        } catch (Exception e) {
            log.warn("Failed to initialize Azure Resource Manager client: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getSubscriptionId() {
        return properties.getSubscriptionId();
    }

    @Override
    public String getTenantId() {
        return properties.getTenantId();
    }

    @Override
    public String getResourceGroupName() {
        return properties.getResourceGroup();
    }

    @Override
    public String getLocation() {
        return properties.getLocation();
    }

    @Override
    public String getVnetName() {
        return properties.getVnetName();
    }

    @Override
    public String getSubnetName() {
        return properties.getSubnetName();
    }

    @Override
    public String getAcrName() {
        return properties.getAcrName();
    }

    @Override
    public String getAcrLoginServer() {
        return properties.getAcrLoginServer();
    }

    @Override
    public String getAcrRepositoryPrefix() {
        return properties.getAcrRepositoryPrefix();
    }

    @Override
    public String resolveAcrLoginServer() {
        return properties.resolveAcrLoginServer();
    }
}
