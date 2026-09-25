package com.cloudship.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudship.security.cloudflare")
public class CloudflareSecurityProperties {

    private static final Logger log = LoggerFactory.getLogger(CloudflareSecurityProperties.class);

    /**
     * Whether Cloudflare Access JWT validation is enforced.
     * When false (local development), requests are authenticated with the dev user.
     */
    private boolean enabled = false;

    /**
     * Cloudflare Access team domain (e.g., https://myteam.cloudflareaccess.com or myteam).
     */
    private String teamDomain;

    /**
     * Cloudflare Access Application Audience (AUD) tag.
     */
    private String aud;

    /**
     * Custom JWKS URL if different from standard team domain certs.
     */
    private String jwksUrl;

    /**
     * Dev user email used when cloudflare.enabled is false.
     */
    private String devUserEmail = "dev@cloudship.local";

    /**
     * Dev user display name used when cloudflare.enabled is false.
     */
    private String devUserName = "CloudShip Dev Admin";

    /**
     * Cloudflare Access logout URL (e.g., https://myteam.cloudflareaccess.com/cdn-cgi/access/logout).
     * Must be a valid HTTPS URL under the configured team domain to prevent open redirects.
     */
    private String logoutUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTeamDomain() {
        return teamDomain;
    }

    public void setTeamDomain(String teamDomain) {
        this.teamDomain = teamDomain;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getJwksUrl() {
        return jwksUrl;
    }

    public void setJwksUrl(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }

    public String getDevUserEmail() {
        return devUserEmail;
    }

    public void setDevUserEmail(String devUserEmail) {
        this.devUserEmail = devUserEmail;
    }

    public String getDevUserName() {
        return devUserName;
    }

    public void setDevUserName(String devUserName) {
        this.devUserName = devUserName;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getNormalizedTeamDomain() {
        if (teamDomain == null || teamDomain.isBlank()) {
            return null;
        }
        String domain = teamDomain.trim();
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            if (!domain.contains(".")) {
                domain = domain + ".cloudflareaccess.com";
            }
            domain = "https://" + domain;
        }
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain;
    }

    public String resolveJwksUrl() {
        if (jwksUrl != null && !jwksUrl.isBlank()) {
            return jwksUrl.trim();
        }
        String normalizedDomain = getNormalizedTeamDomain();
        if (normalizedDomain != null) {
            return normalizedDomain + "/cdn-cgi/access/certs";
        }
        return null;
    }

    public String resolveLogoutUrl() {
        if (logoutUrl != null && !logoutUrl.isBlank()) {
            String url = logoutUrl.trim();
            // Validate the logout URL is under the team domain to prevent open redirects
            String teamDomain = getNormalizedTeamDomain();
            if (teamDomain != null && url.startsWith(teamDomain)) {
                return url;
            }
            log.warn("Cloudflare logout URL '{}' is not under configured team domain '{}', ignoring for security", url, teamDomain);
        }
        // Fallback to standard Cloudflare Access logout path
        String teamDomain = getNormalizedTeamDomain();
        if (teamDomain != null) {
            return teamDomain + "/cdn-cgi/access/logout";
        }
        return null;
    }
}
