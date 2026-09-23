package com.cloudship.dto.azure;

import java.time.OffsetDateTime;

public class AcrVerificationResponse {

    private boolean verified;
    private String repository;
    private String tag;
    private String digest;
    private String loginServer;
    private AzureResourceStatus status;
    private String message;
    private OffsetDateTime verifiedAt;

    public AcrVerificationResponse() {
    }

    public AcrVerificationResponse(boolean verified, String repository, String tag, String digest,
                                   String loginServer, AzureResourceStatus status, String message) {
        this.verified = verified;
        this.repository = repository;
        this.tag = tag;
        this.digest = digest;
        this.loginServer = loginServer;
        this.status = status;
        this.message = message;
        this.verifiedAt = OffsetDateTime.now();
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getLoginServer() {
        return loginServer;
    }

    public void setLoginServer(String loginServer) {
        this.loginServer = loginServer;
    }

    public AzureResourceStatus getStatus() {
        return status;
    }

    public void setStatus(AzureResourceStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
