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
    private final com.relax.auth.AuthMapper authMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    TechnicianAdminService(TechnicianAuditMapper auditMapper, TechnicianPricingMapper pricingMapper,
            com.relax.auth.AuthMapper authMapper,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.auditMapper = auditMapper;
        this.pricingMapper = pricingMapper;
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
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
        if (authMapper.countRole(app.userId(), "TECHNICIAN") == 0) {
            authMapper.insertRole(app.userId(), "TECHNICIAN", reviewerId);
        }
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

    @Transactional
    public TechnicianAuditMapper.TechnicianBrief createTechnician(CreateTechnicianCommand cmd, long operatorId) {
        String phone = cmd.phone().strip();
        if (!phone.matches("1\\d{10}")) {
            throw new BusinessException("PHONE_INVALID", "手机号格式不正确，请输入11位手机号");
        }
        if (cmd.password() == null || cmd.password().strip().length() < 6) {
            throw new BusinessException("PASSWORD_INVALID", "登录密码不能少于6位");
        }
        String passwordHash = passwordEncoder.encode(cmd.password().strip());

        Optional<com.relax.auth.UserAccount> userOpt = authMapper.findUserByPhone(phone);
        long userId;
        if (userOpt.isPresent()) {
            userId = userOpt.get().id();
            authMapper.updatePassword(userId, passwordHash);
            if (authMapper.countRole(userId, "TECHNICIAN") == 0) {
                authMapper.insertRole(userId, "TECHNICIAN", operatorId);
            }
        } else {
            userId = IdWorker.getId();
            authMapper.insertUser(userId, "tech:phone:" + phone, null);
            authMapper.updatePhone(userId, phone);
            authMapper.updateProfile(userId, cmd.serviceName().strip(), null);
            authMapper.updatePassword(userId, passwordHash);
            authMapper.insertRole(userId, "USER", operatorId);
            authMapper.insertRole(userId, "TECHNICIAN", operatorId);
        }

        int exp = cmd.experienceYears() != null ? cmd.experienceYears() : 1;
        String realName = (cmd.realName() != null && !cmd.realName().isBlank()) ? cmd.realName().strip() : cmd.serviceName().strip();
        String intro = (cmd.intro() != null && !cmd.intro().isBlank()) ? cmd.intro().strip() : "平台认证专业技师";

        Optional<Long> existingTechId = auditMapper.findTechnicianIdByUserId(userId);
        long techId;
        if (existingTechId.isPresent()) {
            techId = existingTechId.get();
            auditMapper.updateTechnicianStatus(techId, "ACTIVE");
            auditMapper.updateBasicInfo(techId, cmd.serviceName().strip(), realName, phone, intro, exp);
        } else {
            techId = IdWorker.getId();
            auditMapper.insertTechnician(techId, userId, cmd.serviceName().strip(), realName, phone, intro, exp);
        }

        return auditMapper.findAllTechnicians().stream()
                .filter(t -> t.id() == techId)
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteTechnician(long technicianId) {
        TechnicianAuditMapper.TechnicianBrief tech = auditMapper.findAllTechnicians().stream()
                .filter(t -> t.id() == technicianId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师不存在"));
        auditMapper.updateTechnicianStatus(technicianId, "DISABLED");
        auditMapper.updateOnlineStatus(technicianId, "OFFLINE");
    }

    @Transactional
    public void resetTechnicianPassword(long technicianId, String newPassword) {
        if (newPassword == null || newPassword.strip().length() < 6) {
            throw new BusinessException("PASSWORD_INVALID", "登录密码不能少于6位");
        }
        TechnicianAuditMapper.TechnicianBrief tech = auditMapper.findAllTechnicians().stream()
                .filter(t -> t.id() == technicianId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师不存在"));
        authMapper.updatePassword(tech.userId(), passwordEncoder.encode(newPassword.strip()));
    }

    public record CreateTechnicianCommand(String phone, String password, String serviceName,
            String realName, String intro, Integer experienceYears) {
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
