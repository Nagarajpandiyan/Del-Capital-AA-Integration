package com.delcapital.aa.service;

import com.delcapital.aa.model.AuditLog;
import com.delcapital.aa.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepo;

    @Async
    public void log(String entityType, UUID entityId, String action,
                    String actor, Map<String, Object> oldState, Map<String, Object> newState) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actor(actor)
                    .oldState(oldState)
                    .newState(newState)
                    .build();
            auditLogRepo.save(entry);
            log.debug("Audit logged: entity={}, id={}, action={}", entityType, entityId, action);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    @Async
    public void logWithMeta(String entityType, UUID entityId, String action,
                            String actor, String ipAddress, Map<String, Object> metadata) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actor(actor)
                    .ipAddress(ipAddress)
                    .metadata(metadata)
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }
}
