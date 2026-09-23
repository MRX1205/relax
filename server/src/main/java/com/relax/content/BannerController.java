package com.relax.content;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class BannerController {

    private final BannerService bannerService;

    BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/banners")
    ApiResponse<List<BannerMapper.BannerView>> listActive() {
        return ApiResponse.success(bannerService.listActive());
    }

    @GetMapping("/admin/banners")
    @PreAuthorize("hasAuthority('content:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<List<BannerMapper.BannerView>> listAll() {
        return ApiResponse.success(bannerService.listAll());
    }

    @PostMapping("/admin/banners")
    @PreAuthorize("hasAuthority('content:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<BannerMapper.BannerView> create(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.success(bannerService.create(new BannerService.BannerRequest(
                request.title(), request.imageFileId(), request.linkType(), request.linkValue(), request.sort())));
    }

    @PutMapping("/admin/banners/{id}")
    @PreAuthorize("hasAuthority('content:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<BannerMapper.BannerView> update(@PathVariable long id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.success(bannerService.update(id, new BannerService.BannerRequest(
                request.title(), request.imageFileId(), request.linkType(), request.linkValue(), request.sort())));
    }

    @PutMapping("/admin/banners/{id}/status")
    @PreAuthorize("hasAuthority('content:manage') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<Void> updateStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        bannerService.updateStatus(id, request.status());
        return ApiResponse.success(null);
    }

    public record BannerRequest(@NotBlank @Size(max = 100) String title, Long imageFileId,
            String linkType, @Size(max = 200) String linkValue, @Min(0) int sort) {}

    public record StatusRequest(@NotBlank String status) {}
}
