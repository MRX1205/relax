package com.relax.region;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.audit.AuditService;
import com.relax.common.api.BusinessException;

@Service
public class RegionService {

    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("113.52");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("114.25");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("22.65");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("23.15");

    private final RegionMapper regionMapper;
    private final AuditService auditService;

    RegionService(RegionMapper regionMapper, AuditService auditService) {
        this.regionMapper = regionMapper;
        this.auditService = auditService;
    }

    public List<RegionMapper.ServiceArea> serviceAreas() {
        return regionMapper.findServiceAreas();
    }

    public List<RegionMapper.UserAddress> addresses(long userId) {
        return regionMapper.findAddresses(userId);
    }

    @Transactional
    public RegionMapper.UserAddress createAddress(long userId, AddressRequest request) {
        RegionMapper.ServiceArea area = requireServiceable(request.regionCode(), request.longitude(), request.latitude());
        boolean makeDefault = request.isDefault() || regionMapper.countAddresses(userId) == 0;
        if (makeDefault) {
            regionMapper.clearDefault(userId);
        }
        long id = IdWorker.getId();
        regionMapper.insertAddress(id, userId, request.contactName().strip(), request.contactPhone().strip(),
                area.regionId(), request.detail().strip(), request.longitude(), request.latitude(),
                normalizeLabel(request.label()), makeDefault);
        return regionMapper.findAddress(userId, id).orElseThrow();
    }

    @Transactional
    public RegionMapper.UserAddress updateAddress(long userId, long id, AddressRequest request) {
        if (regionMapper.findAddress(userId, id).isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "地址不存在");
        }
        RegionMapper.ServiceArea area = requireServiceable(request.regionCode(), request.longitude(), request.latitude());
        if (request.isDefault()) {
            regionMapper.clearDefault(userId);
        }
        regionMapper.updateAddress(id, userId, request.contactName().strip(), request.contactPhone().strip(),
                area.regionId(), request.detail().strip(), request.longitude(), request.latitude(),
                normalizeLabel(request.label()), request.isDefault());
        return regionMapper.findAddress(userId, id).orElseThrow();
    }

    @Transactional
    public void deleteAddress(long userId, long id) {
        RegionMapper.UserAddress existing = regionMapper.findAddress(userId, id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "地址不存在"));
        regionMapper.deleteAddress(userId, id);
        if (existing.isDefault()) {
            regionMapper.makeLatestDefault(userId);
        }
    }

    @Transactional
    public RegionMapper.ServiceArea updateServiceArea(long operatorId, long id, String status, String ipAddress) {
        if (!Set.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessException("SERVICE_AREA_STATUS_INVALID", "服务区域状态无效");
        }
        if (regionMapper.updateServiceArea(id, status, operatorId) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "SERVICE_AREA_NOT_FOUND", "服务区域不存在");
        }
        auditService.record(operatorId, "SERVICE_AREA_UPDATED", "SERVICE_AREA", Long.toString(id),
                "status=" + status, ipAddress);
        return regionMapper.findServiceAreas().stream().filter(area -> area.id() == id).findFirst().orElseThrow();
    }

    private RegionMapper.ServiceArea requireServiceable(String regionCode, BigDecimal longitude, BigDecimal latitude) {
        RegionMapper.ServiceArea area = regionMapper.findServiceAreaByRegionCode(regionCode)
                .orElseThrow(() -> new BusinessException("ADDRESS_OUT_OF_SERVICE", "地址不在东莞市服务范围内"));
        if (!"ENABLED".equals(area.status()) || !insideDongguanBounds(longitude, latitude)) {
            throw new BusinessException("ADDRESS_OUT_OF_SERVICE", "该地址暂不在平台服务范围内");
        }
        return area;
    }

    private boolean insideDongguanBounds(BigDecimal longitude, BigDecimal latitude) {
        return longitude.compareTo(MIN_LONGITUDE) >= 0 && longitude.compareTo(MAX_LONGITUDE) <= 0
                && latitude.compareTo(MIN_LATITUDE) >= 0 && latitude.compareTo(MAX_LATITUDE) <= 0;
    }

    private String normalizeLabel(String label) {
        return label == null || label.isBlank() ? null : label.strip();
    }

    public record AddressRequest(
            String contactName,
            String contactPhone,
            String regionCode,
            String detail,
            BigDecimal longitude,
            BigDecimal latitude,
            String label,
            boolean isDefault) {
    }

    public record ServiceAreaStatusRequest(String status) {
    }
}
