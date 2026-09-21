package com.relax.technician;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.auth.CurrentUser;
import com.relax.catalog.ProjectMapper;
import com.relax.common.api.ApiResponse;
import com.relax.common.api.BusinessException;

@RestController
@RequestMapping("/api/v1/technician")
public class TechnicianController {

    private final TechnicianService technicianService;
    private final TechnicianAuditMapper auditMapper;
    private final TechnicianPricingMapper pricingMapper;
    private final TechnicianPhotoMapper photoMapper;
    private final ProjectMapper projectMapper;

    TechnicianController(TechnicianService technicianService, TechnicianAuditMapper auditMapper,
            TechnicianPricingMapper pricingMapper, TechnicianPhotoMapper photoMapper,
            ProjectMapper projectMapper) {
        this.technicianService = technicianService;
        this.auditMapper = auditMapper;
        this.pricingMapper = pricingMapper;
        this.photoMapper = photoMapper;
        this.projectMapper = projectMapper;
    }

    private long getTechnicianId(long userId) {
        return auditMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == userId)
                .findFirst()
                .map(TechnicianAuditMapper.TechnicianBrief::id)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "TECHNICIAN_NOT_FOUND", "当前账号不是已认证技师"));
    }

    // === 入驻申请 ===

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

    // === 技师资料管理 ===

    @GetMapping("/profile")
    ApiResponse<TechnicianAuditMapper.TechnicianBrief> profile(@AuthenticationPrincipal CurrentUser currentUser) {
        TechnicianAuditMapper.TechnicianBrief tech = auditMapper.findAllTechnicians().stream()
                .filter(t -> t.userId() == currentUser.id())
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师资料不存在"));
        return ApiResponse.success(tech);
    }

    @GetMapping("/profile-full")
    ApiResponse<TechnicianFullProfileView> getFullProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        long techId = getTechnicianId(currentUser.id());
        TechnicianAuditMapper.TechnicianFullProfile profile = auditMapper.findFullProfileById(techId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师资料不存在"));
        List<TechnicianPhotoMapper.PhotoView> photos = photoMapper.findByTechnician(techId);
        return ApiResponse.success(new TechnicianFullProfileView(profile, photos));
    }

    @PutMapping("/profile-full")
    @Transactional
    ApiResponse<TechnicianFullProfileView> updateFullProfile(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody UpdateFullProfileRequest request) {
        long techId = getTechnicianId(currentUser.id());
        auditMapper.updateFullProfile(techId, request.serviceName(), request.phone(), request.intro(),
                request.experienceYears() == null ? 0 : request.experienceYears(),
                request.avatarUrl(), request.age(), request.ageTag(), request.height(), request.weight(),
                request.latitude(), request.longitude(), request.baseAddress(), request.certificationsJson());
        return getFullProfile(currentUser);
    }

    @PutMapping("/online-status")
    ApiResponse<Void> updateOnlineStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody OnlineStatusRequest request) {
        long techId = getTechnicianId(currentUser.id());
        auditMapper.updateOnlineStatus(techId, request.onlineStatus());
        return ApiResponse.success(null);
    }

    // === 技师生活相册管理 ===

    @GetMapping("/photos")
    ApiResponse<List<TechnicianPhotoMapper.PhotoView>> listPhotos(@AuthenticationPrincipal CurrentUser currentUser) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(photoMapper.findByTechnician(techId));
    }

    @PostMapping("/photos")
    ApiResponse<List<TechnicianPhotoMapper.PhotoView>> addPhoto(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AddPhotoRequest request) {
        long techId = getTechnicianId(currentUser.id());
        long photoId = IdWorker.getId();
        photoMapper.insert(photoId, techId, request.fileId() == null ? 0L : request.fileId(),
                request.fileUrl(), request.photoType() == null ? "WORK" : request.photoType(),
                request.sort() == null ? 0 : request.sort());
        return ApiResponse.success(photoMapper.findByTechnician(techId));
    }

    @DeleteMapping("/photos/{id}")
    ApiResponse<Void> deletePhoto(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        long techId = getTechnicianId(currentUser.id());
        photoMapper.delete(id, techId);
        return ApiResponse.success(null);
    }

    // === 技师服务项目管理（增删改查、上下架、加入平台项目库） ===

    @GetMapping("/projects")
    ApiResponse<List<TechnicianPricingMapper.TechnicianProjectView>> listMyProjects(
            @AuthenticationPrincipal CurrentUser currentUser) {
        long techId = getTechnicianId(currentUser.id());
        return ApiResponse.success(pricingMapper.findByTechnician(techId));
    }

    @GetMapping("/available-platform-projects")
    ApiResponse<List<ProjectMapper.ProjectView>> listAvailablePlatformProjects(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(projectMapper.findPlatformAvailable());
    }

    @PostMapping("/projects/join")
    @Transactional
    ApiResponse<TechnicianPricingMapper.TechnicianProjectView> joinPlatformProject(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody JoinProjectRequest request) {
        long techId = getTechnicianId(currentUser.id());
        ProjectMapper.ProjectView project = projectMapper.findById(request.projectId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "平台项目不存在"));

        BigDecimal price = request.overridePrice() != null ? request.overridePrice() : project.basePrice();

        var existing = pricingMapper.findByTechAndProject(techId, request.projectId());
        if (existing.isPresent()) {
            pricingMapper.updatePrice(existing.get().id(), price);
            pricingMapper.updateStatus(existing.get().id(), "ENABLED");
            return ApiResponse.success(pricingMapper.findById(existing.get().id()).orElseThrow());
        } else {
            long id = IdWorker.getId();
            pricingMapper.insertWithStatus(id, techId, request.projectId(), price, "ENABLED");
            return ApiResponse.success(pricingMapper.findById(id).orElseThrow());
        }
    }

    @PostMapping("/projects")
    @Transactional
    ApiResponse<TechnicianPricingMapper.TechnicianProjectView> createCustomProject(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateCustomProjectRequest request) {
        long techId = getTechnicianId(currentUser.id());
        long projectId = IdWorker.getId();

        String status = Boolean.TRUE.equals(request.onShelf()) ? "ON_SHELF" : "OFF_SHELF";
        projectMapper.insertWithCreator(projectId, request.categoryId(), request.name(),
                request.durationMinutes(), request.basePrice(),
                request.description() == null ? "" : request.description(),
                request.notice() == null ? "" : request.notice(),
                request.coverFileId(), 0, "TECHNICIAN", techId, status);

        long pricingId = IdWorker.getId();
        String techStatus = Boolean.TRUE.equals(request.onShelf()) ? "ENABLED" : "DISABLED";
        pricingMapper.insertWithStatus(pricingId, techId, projectId, request.basePrice(), techStatus);

        return ApiResponse.success(pricingMapper.findById(pricingId).orElseThrow());
    }

    @PutMapping("/projects/{id}")
    @Transactional
    ApiResponse<TechnicianPricingMapper.TechnicianProjectView> updateProject(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        long techId = getTechnicianId(currentUser.id());
        TechnicianPricingMapper.TechnicianProjectView existing = pricingMapper.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "服务项目不存在"));
        if (existing.technicianId() != techId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权操作该项目");
        }

        if (request.overridePrice() != null) {
            pricingMapper.updatePrice(id, request.overridePrice());
        }

        // 如果是技师自定义项目，支持同步更新项目详情
        if ("TECHNICIAN".equals(existing.creatorType()) && existing.creatorId() == techId) {
            projectMapper.update(existing.projectId(),
                    request.categoryId() != null ? request.categoryId() : 101L,
                    request.name() != null ? request.name() : existing.projectName(),
                    request.durationMinutes() != null ? request.durationMinutes() : existing.durationMinutes(),
                    request.overridePrice() != null ? request.overridePrice() : existing.basePrice(),
                    request.description() != null ? request.description() : existing.description(),
                    request.notice() != null ? request.notice() : existing.notice(),
                    request.coverFileId() != null ? request.coverFileId() : existing.coverFileId(),
                    0);
        }

        if (request.status() != null) {
            pricingMapper.updateStatus(id, request.status());
            if ("TECHNICIAN".equals(existing.creatorType()) && existing.creatorId() == techId) {
                projectMapper.updateStatus(existing.projectId(), "ENABLED".equals(request.status()) ? "ON_SHELF" : "OFF_SHELF");
            }
        }

        return ApiResponse.success(pricingMapper.findById(id).orElseThrow());
    }

    @PutMapping("/projects/{id}/toggle-status")
    @Transactional
    ApiResponse<TechnicianPricingMapper.TechnicianProjectView> toggleProjectStatus(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id) {
        long techId = getTechnicianId(currentUser.id());
        TechnicianPricingMapper.TechnicianProjectView existing = pricingMapper.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "服务项目不存在"));
        if (existing.technicianId() != techId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权操作该项目");
        }

        String newStatus = "ENABLED".equals(existing.status()) ? "DISABLED" : "ENABLED";
        pricingMapper.updateStatus(id, newStatus);

        // 自定义项目联动 service_project
        if ("TECHNICIAN".equals(existing.creatorType()) && existing.creatorId() == techId) {
            projectMapper.updateStatus(existing.projectId(), "ENABLED".equals(newStatus) ? "ON_SHELF" : "OFF_SHELF");
        }

        return ApiResponse.success(pricingMapper.findById(id).orElseThrow());
    }

    @DeleteMapping("/projects/{id}")
    @Transactional
    ApiResponse<Void> removeProject(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id) {
        long techId = getTechnicianId(currentUser.id());
        TechnicianPricingMapper.TechnicianProjectView existing = pricingMapper.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "服务项目不存在"));
        if (existing.technicianId() != techId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权操作该项目");
        }

        pricingMapper.deleteByTechAndId(id, techId);

        // 如果是该技师自建的项目，删除 technician_project 关系的同时也清理自建的项目
        if ("TECHNICIAN".equals(existing.creatorType()) && existing.creatorId() == techId) {
            projectMapper.delete(existing.projectId());
        }

        return ApiResponse.success(null);
    }

    // === 内部 Records ===

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

    public record TechnicianFullProfileView(
            TechnicianAuditMapper.TechnicianFullProfile profile,
            List<TechnicianPhotoMapper.PhotoView> photos) {
    }

    public record UpdateFullProfileRequest(
            @NotBlank @Size(max = 64) String serviceName,
            @NotBlank @Pattern(regexp = "1\\d{10}") String phone,
            @Size(max = 1000) String intro,
            @Min(0) Integer experienceYears,
            String avatarUrl,
            Integer age,
            String ageTag,
            Integer height,
            Integer weight,
            BigDecimal latitude,
            BigDecimal longitude,
            String baseAddress,
            String certificationsJson) {
    }

    public record AddPhotoRequest(
            Long fileId,
            String fileUrl,
            String photoType,
            Integer sort) {
    }

    public record JoinProjectRequest(
            @NotNull Long projectId,
            @DecimalMin("0.01") BigDecimal overridePrice) {
    }

    public record CreateCustomProjectRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Long categoryId,
            @NotNull @Min(15) Integer durationMinutes,
            @NotNull @DecimalMin("0.01") BigDecimal basePrice,
            @Size(max = 1000) String description,
            @Size(max = 500) String notice,
            Long coverFileId,
            Boolean onShelf) {
    }

    public record UpdateProjectRequest(
            BigDecimal overridePrice,
            String name,
            Long categoryId,
            Integer durationMinutes,
            String description,
            String notice,
            Long coverFileId,
            String status) {
    }
}
