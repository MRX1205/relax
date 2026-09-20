package com.relax.technician;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class TechnicianAdminService {

    private final TechnicianAuditMapper auditMapper;
    private final TechnicianPricingMapper pricingMapper;

    TechnicianAdminService(TechnicianAuditMapper auditMapper, TechnicianPricingMapper pricingMapper) {
        this.auditMapper = auditMapper;
        this.pricingMapper = pricingMapper;
    }

    // === 技师列表 ===

    public List<TechnicianAuditMapper.TechnicianBrief> listTechnicians(String status) {
        if (status != null && !status.isBlank()) {
            return auditMapper.findTechniciansByStatus(status);
        }
        return auditMapper.findAllTechnicians();
    }

    // === 入驻审核 ===

    public List<TechnicianAuditMapper.TechnicianApplicationView> pendingApplications() {
        return auditMapper.findPendingApplications();
    }

    @Transactional
    public void approveApplication(long applicationId, long reviewerId) {
        TechnicianAuditMapper.TechnicianApplicationView app = auditMapper.findApplicationById(applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "申请不存在"));
        if (!"PENDING".equals(app.status())) {
            throw new BusinessException("APPLICATION_NOT_PENDING", "该申请不在待审核状态");
        }
        auditMapper.reviewApplication(applicationId, "APPROVED", null, reviewerId);
        long techId = IdWorker.getId();
        auditMapper.insertTechnician(techId, app.userId(), app.serviceName(), app.realName(),
                app.phone(), app.intro(), app.experienceYears());
    }

    @Transactional
    public void rejectApplication(long applicationId, long reviewerId, String reason) {
        TechnicianAuditMapper.TechnicianApplicationView app = auditMapper.findApplicationById(applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "申请不存在"));
        if (!"PENDING".equals(app.status())) {
            throw new BusinessException("APPLICATION_NOT_PENDING", "该申请不在待审核状态");
        }
        auditMapper.reviewApplication(applicationId, "REJECTED", reason, reviewerId);
    }

    @Transactional
    public void updateTechnicianStatus(long technicianId, String status) {
        if (auditMapper.updateTechnicianStatus(technicianId, status) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师不存在");
        }
    }

    // === 项目定价 ===

    public List<TechnicianPricingMapper.TechnicianProjectView> listPricing(long technicianId) {
        return pricingMapper.findByTechnician(technicianId);
    }

    @Transactional
    public TechnicianPricingMapper.TechnicianProjectView setPricing(long technicianId, long projectId,
            BigDecimal overridePrice) {
        Optional<TechnicianPricingMapper.TechnicianProjectView> existing =
                pricingMapper.findActive(technicianId, projectId);
        if (existing.isPresent()) {
            pricingMapper.updatePrice(existing.get().id(), overridePrice);
            return pricingMapper.findByTechnician(technicianId).stream()
                    .filter(p -> p.projectId() == projectId).findFirst().orElseThrow();
        }
        long id = IdWorker.getId();
        pricingMapper.insert(id, technicianId, projectId, overridePrice);
        return pricingMapper.findByTechnician(technicianId).stream()
                .filter(p -> p.projectId() == projectId).findFirst().orElseThrow();
    }

    @Transactional
    public void removePricing(long pricingId) {
        pricingMapper.delete(pricingId);
    }
}
