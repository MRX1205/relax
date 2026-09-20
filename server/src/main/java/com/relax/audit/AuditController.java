package com.relax.audit;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAuthority('audit:read')")
public class AuditController {

    private final AuditService auditService;

    AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    ApiResponse<List<AuditMapper.AuditEntry>> recent() {
        return ApiResponse.success(auditService.recent());
    }
}
