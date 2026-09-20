package com.relax.technician;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class TechnicianService {

    private final TechnicianMapper technicianMapper;

    TechnicianService(TechnicianMapper technicianMapper) {
        this.technicianMapper = technicianMapper;
    }

    public Optional<TechnicianMapper.ApplicationView> latestApplication(long userId) {
        return technicianMapper.findLatestApplication(userId);
    }

    @Transactional
    public TechnicianMapper.ApplicationView submitApplication(long userId, ApplicationRequest request) {
        Optional<TechnicianMapper.ApplicationView> existing = technicianMapper.findLatestApplication(userId);
        if (existing.isPresent() && "PENDING".equals(existing.get().status())) {
            throw new BusinessException("APPLICATION_PENDING", "已有一份待审核申请，请等待审核结果");
        }
        long id = IdWorker.getId();
        String areaCodes = String.join(",", request.serviceAreaCodes());
        technicianMapper.insertApplication(id, userId, request.serviceName().strip(),
                request.realName().strip(), request.phone().strip(),
                request.intro() == null ? "" : request.intro().strip(),
                request.experienceYears(), areaCodes,
                request.photoFileId(), request.certificateFileId());
        return technicianMapper.findLatestApplication(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "APPLICATION_CREATE_FAILED", "申请创建失败"));
    }

    public record ApplicationRequest(
            String serviceName,
            String realName,
            String phone,
            String intro,
            int experienceYears,
            List<String> serviceAreaCodes,
            Long photoFileId,
            Long certificateFileId) {
    }
}
