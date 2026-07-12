package com.mtdsubmitter.service;

import com.mtdsubmitter.model.AuditLog;
import com.mtdsubmitter.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Service for recording all user actions in the audit log.
 * Every HMRC submission, data change, and login is logged.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an action to the audit trail.
     */
    public void log(UUID userId, String entityType, UUID entityId,
                    String action, String oldValue, String newValue) {
        String ipAddress = null;
        String userAgent = null;

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception ignored) {
            // Not in a web request context (e.g., scheduled task)
        }

        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent != null && userAgent.length() > 512
                        ? userAgent.substring(0, 512) : userAgent)
                .build();

        auditLogRepository.save(entry);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
