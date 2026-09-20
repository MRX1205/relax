package com.relax.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1")
public class CouponController {

    private final CouponService couponService;

    CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // === Public: Available coupons ===
    @GetMapping("/coupons/available")
    ApiResponse<List<CouponMapper.CouponTemplate>> availableCoupons() {
        return ApiResponse.success(couponService.listTemplates().stream()
                .filter(t -> "ACTIVE".equals(t.status()))
                .toList());
    }

    // === User: My coupons ===
    @GetMapping("/coupons/mine")
    ApiResponse<List<CouponMapper.UserCoupon>> myCoupons(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(couponService.getMyCoupons(currentUser.id(), status));
    }

    // === User: Claim coupon ===
    @PostMapping("/coupons/{templateId}/claim")
    ApiResponse<Void> claimCoupon(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long templateId) {
        couponService.claimCoupon(currentUser.id(), templateId);
        return ApiResponse.success(null);
    }

    // === Admin: Manage coupons ===
    @GetMapping("/admin/coupons")
    @PreAuthorize("hasAuthority('content:manage')")
    ApiResponse<List<CouponMapper.CouponTemplate>> listTemplates() {
        return ApiResponse.success(couponService.listTemplates());
    }

    @PostMapping("/admin/coupons")
    @PreAuthorize("hasAuthority('content:manage')")
    ApiResponse<CouponMapper.CouponTemplate> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.success(couponService.createTemplate(
                new CouponService.CreateTemplateRequest(request.name(), request.amount(), request.minSpend(),
                        request.totalCount(), request.startAt(), request.endAt())));
    }

    @PutMapping("/admin/coupons/{id}/status")
    @PreAuthorize("hasAuthority('content:manage')")
    ApiResponse<Void> updateStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        couponService.updateTemplateStatus(id, request.status());
        return ApiResponse.success(null);
    }

    public record CreateTemplateRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull @DecimalMin("0") BigDecimal minSpend,
            @Min(1) int totalCount,
            LocalDateTime startAt,
            LocalDateTime endAt) {}

    public record StatusRequest(@NotBlank String status) {}
}
