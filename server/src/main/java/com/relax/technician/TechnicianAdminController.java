package com.relax.technician;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/technicians")
@PreAuthorize("hasAuthority('technician:audit')")
public class TechnicianAdminController {

    private final TechnicianAdminService adminService;

    TechnicianAdminController(TechnicianAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    ApiResponse<List<TechnicianAuditMapper.TechnicianBrief>> list(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.listTechnicians(status));
    }

    @GetMapping("/applications/pending")
    ApiResponse<List<TechnicianAuditMapper.TechnicianApplicationView>> pendingApplications() {
        return ApiResponse.success(adminService.pendingApplications());
    }

    @PostMapping("/applications/{id}/approve")
    ApiResponse<Void> approve(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        adminService.approveApplication(id, currentUser.id());
        return ApiResponse.success(null);
    }

    @PostMapping("/applications/{id}/reject")
    ApiResponse<Void> reject(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id,
            @Valid @RequestBody RejectRequest request) {
        adminService.rejectApplication(id, currentUser.id(), request.reason());
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    ApiResponse<Void> updateStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        adminService.updateTechnicianStatus(id, request.status());
        return ApiResponse.success(null);
    }

    // === 项目定价 ===

    @GetMapping("/{technicianId}/pricing")
    ApiResponse<List<TechnicianPricingMapper.TechnicianProjectView>> listPricing(
            @PathVariable long technicianId) {
        return ApiResponse.success(adminService.listPricing(technicianId));
    }

    @PutMapping("/{technicianId}/pricing/{projectId}")
    ApiResponse<TechnicianPricingMapper.TechnicianProjectView> setPricing(
            @PathVariable long technicianId, @PathVariable long projectId,
            @Valid @RequestBody PricingRequest request) {
        return ApiResponse.success(adminService.setPricing(technicianId, projectId, request.overridePrice()));
    }

    @DeleteMapping("/{technicianId}/pricing/{pricingId}")
    ApiResponse<Void> removePricing(@PathVariable long technicianId, @PathVariable long pricingId) {
        adminService.removePricing(pricingId);
        return ApiResponse.success(null);
    }

    public record RejectRequest(@Size(max = 500) String reason) {
    }

    public record StatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED|SUSPENDED") String status) {
    }

    public record PricingRequest(@NotNull @DecimalMin("0.01") BigDecimal overridePrice) {
    }
}
