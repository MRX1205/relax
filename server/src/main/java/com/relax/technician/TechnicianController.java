package com.relax.technician;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;
import com.relax.common.api.BusinessException;

@RestController
@RequestMapping("/api/v1/technician")
public class TechnicianController {

    private final TechnicianService technicianService;
    private final TechnicianAuditMapper auditMapper;

    TechnicianController(TechnicianService technicianService, TechnicianAuditMapper auditMapper) {
        this.technicianService = technicianService;
        this.auditMapper = auditMapper;
    }

    @GetMapping("/application")
    ApiResponse<TechnicianMapper.ApplicationView> application(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(technicianService.latestApplication(currentUser.id()).orElse(null));
    }

    @PostMapping("/application")
    ApiResponse<TechnicianMapper.ApplicationView> submit(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ApplicationRequest request) {
        return ApiResponse.success(technicianService.submitApplication(currentUser.id(),
                new TechnicianService.ApplicationRequest(
                        request.serviceName, request.realName, request.phone, request.intro,
                        request.experienceYears, request.serviceAreaCodes,
                        request.photoFileId, request.certificateFileId)));
    }

    @GetMapping("/profile")
    ApiResponse<TechnicianAuditMapper.TechnicianBrief> profile(@AuthenticationPrincipal CurrentUser currentUser) {
        TechnicianAuditMapper.TechnicianBrief tech = auditMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == currentUser.id())
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师资料不存在"));
        return ApiResponse.success(tech);
    }

    @PutMapping("/online-status")
    ApiResponse<Void> updateOnlineStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody OnlineStatusRequest request) {
        long techId = auditMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == currentUser.id())
                .findFirst()
                .map(TechnicianAuditMapper.TechnicianBrief::id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师资料不存在"));
        auditMapper.updateOnlineStatus(techId, request.onlineStatus());
        return ApiResponse.success(null);
    }

    public record ApplicationRequest(
            @NotBlank @Size(max = 64) String serviceName,
            @NotBlank @Size(max = 40) String realName,
            @NotBlank @Pattern(regexp = "1\\d{10}") String phone,
            @Size(max = 500) String intro,
            @Min(0) int experienceYears,
            @NotEmpty List<String> serviceAreaCodes,
            Long photoFileId,
            Long certificateFileId) {
    }

    public record OnlineStatusRequest(
            @NotBlank @Pattern(regexp = "ONLINE|OFFLINE") String onlineStatus) {
    }
}
