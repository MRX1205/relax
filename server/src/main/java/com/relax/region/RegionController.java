package com.relax.region;

import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class RegionController {

    private final RegionService regionService;

    RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping("/regions/dongguan/service-areas")
    ApiResponse<List<RegionMapper.ServiceArea>> serviceAreas() {
        return ApiResponse.success(regionService.serviceAreas());
    }

    @GetMapping("/addresses")
    ApiResponse<List<RegionMapper.UserAddress>> addresses(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(regionService.addresses(currentUser.id()));
    }

    @PostMapping("/addresses")
    ApiResponse<RegionMapper.UserAddress> createAddress(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(regionService.createAddress(currentUser.id(), request.toServiceRequest()));
    }

    @PutMapping("/addresses/{id}")
    ApiResponse<RegionMapper.UserAddress> updateAddress(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(regionService.updateAddress(currentUser.id(), id, request.toServiceRequest()));
    }

    @DeleteMapping("/addresses/{id}")
    ApiResponse<Void> deleteAddress(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        regionService.deleteAddress(currentUser.id(), id);
        return ApiResponse.success(null);
    }

    @PutMapping("/admin/service-areas/{id}")
    @PreAuthorize("hasAuthority('region:manage')")
    ApiResponse<RegionMapper.ServiceArea> updateServiceArea(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody RegionService.ServiceAreaStatusRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(regionService.updateServiceArea(currentUser.id(), id, request.status(),
                servletRequest.getRemoteAddr()));
    }

    public record AddressRequest(
            @NotBlank @Size(max = 40) String contactName,
            @NotBlank @Pattern(regexp = "1\\d{10}") String contactPhone,
            @NotBlank @Pattern(regexp = "\\d{6,12}") String regionCode,
            @NotBlank @Size(max = 255) String detail,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @Size(max = 20) String label,
            boolean isDefault) {

        RegionService.AddressRequest toServiceRequest() {
            return new RegionService.AddressRequest(contactName, contactPhone, regionCode, detail,
                    longitude, latitude, label, isDefault);
        }
    }
}
