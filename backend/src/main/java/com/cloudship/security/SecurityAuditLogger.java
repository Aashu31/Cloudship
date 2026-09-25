package com.cloudship.security;

import com.cloudship.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class SecurityAuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("com.cloudship.security.audit");

    public enum AuditEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        ACCESS_DENIED,
        AUTHENTICATION_FAILURE,
        INVALID_JWT,
        INVALID_AUDIENCE,
        INVALID_ISSUER,
        WEBHOOK_REJECTED,
        UNAUTHORIZED_RESOURCE_ACCESS,
        ADMIN_ACTION,
        SECURITY_CONFIGURATION_ERROR,
        LOGOUT
    }

    public void logEvent(AuditEventType eventType, String userEmail, String resourceId, String requestPath, String result, String details) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        auditLog.info("SECURITY_AUDIT event={} user={} resource={} path={} result={} details={} correlationId={} timestamp={}",
                eventType.name(),
                sanitizeForLog(userEmail),
                sanitizeForLog(resourceId),
                sanitizeForLog(requestPath),
                result,
                sanitizeForLog(details),
                correlationId,
                OffsetDateTime.now().toString()
        );
    }

    public void logLoginSuccess(User user, String requestPath) {
        logEvent(AuditEventType.LOGIN_SUCCESS, user.getEmail(), String.valueOf(user.getId()), requestPath, "SUCCESS", "Cloudflare Access JWT validated");
    }

    public void logLoginFailure(String requestPath, String reason) {
        logEvent(AuditEventType.LOGIN_FAILURE, "unknown", "none", requestPath, "FAILURE", reason);
    }

    public void logAccessDenied(User user, String resourceId, String requestPath) {
        logEvent(AuditEventType.ACCESS_DENIED, user.getEmail(), resourceId, requestPath, "DENIED", "Insufficient privileges for resource");
    }

    public void logInvalidJwt(String requestPath, String reason) {
        logEvent(AuditEventType.INVALID_JWT, "unknown", "none", requestPath, "REJECTED", reason);
    }

    public void logUnauthorizedResourceAccess(User user, String resourceId, String requestPath) {
        logEvent(AuditEventType.UNAUTHORIZED_RESOURCE_ACCESS, user.getEmail(), resourceId, requestPath, "DENIED", "Attempted access to unauthorized resource");
    }

    public void logAdminAction(User user, String action, String resourceId, String requestPath) {
        logEvent(AuditEventType.ADMIN_ACTION, user.getEmail(), resourceId, requestPath, "SUCCESS", "Admin action: " + action);
    }

    public void logWebhookRejected(String requestPath, String reason) {
        logEvent(AuditEventType.WEBHOOK_REJECTED, "webhook", "none", requestPath, "REJECTED", reason);
    }

    public void logLogout(User user, String requestPath) {
        logEvent(AuditEventType.LOGOUT, user.getEmail(), String.valueOf(user.getId()), requestPath, "SUCCESS", "User logged out");
    }

    public void logSecurityConfigError(String requestPath, String error) {
        logEvent(AuditEventType.SECURITY_CONFIGURATION_ERROR, "system", "none", requestPath, "ERROR", error);
    }

    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}