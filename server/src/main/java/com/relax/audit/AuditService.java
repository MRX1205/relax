package com.relax.audit;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditMapper auditMapper;

    AuditService(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public void record(Long operatorId, String action, String resourceType, String resourceId,
            String detail, String ipAddress) {
        auditMapper.insert(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), operatorId, action,
                resourceType, resourceId, detail, ipAddress);
    }

    public List<AuditMapper.AuditEntry> recent() {
        return auditMapper.findRecent();
    }
}
