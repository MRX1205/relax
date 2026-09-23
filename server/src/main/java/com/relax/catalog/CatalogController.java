package com.relax.catalog;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class CatalogController {

    private final CatalogService catalogService;

    CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // === 分类（公开 + 管理） ===

    @GetMapping("/categories")
    ApiResponse<List<CategoryMapper.CategoryView>> listCategories() {
        return ApiResponse.success(catalogService.listCategories());
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<CategoryMapper.CategoryView> createCategory(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(catalogService.createCategory(
                new CatalogService.CategoryRequest(request.name(), request.sort())));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<CategoryMapper.CategoryView> updateCategory(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(catalogService.updateCategory(id,
                new CatalogService.CategoryRequest(request.name(), request.sort())));
    }

    @PutMapping("/admin/categories/{id}/status")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<CategoryMapper.CategoryView> updateCategoryStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(catalogService.updateCategoryStatus(id, request.status()));
    }

    // === 项目管理（仅管理端） ===

    @GetMapping("/admin/projects")
    @PreAuthorize("hasAuthority('project:read') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<List<ProjectMapper.ProjectView>> listAllProjects() {
        return ApiResponse.success(catalogService.listProjects());
    }

    @PostMapping("/admin/projects")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<ProjectMapper.ProjectView> createProject(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(catalogService.createProject(new CatalogService.ProjectRequest(
                request.categoryId(), request.name(), request.durationMinutes(), request.basePrice(),
                request.description(), request.notice(), request.coverFileId(), request.sort())));
    }

    @PutMapping("/admin/projects/{id}")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<ProjectMapper.ProjectView> updateProject(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(catalogService.updateProject(id, new CatalogService.ProjectRequest(
                request.categoryId(), request.name(), request.durationMinutes(), request.basePrice(),
                request.description(), request.notice(), request.coverFileId(), request.sort())));
    }

    @PutMapping("/admin/projects/{id}/status")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<ProjectMapper.ProjectView> updateProjectStatus(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, @Valid @RequestBody StatusRequest request) {
        return ApiResponse.success(catalogService.updateProjectStatus(id, request.status()));
    }

    @DeleteMapping("/admin/projects/{id}")
    @PreAuthorize("hasAuthority('project:write') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    ApiResponse<Void> deleteProject(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        catalogService.deleteProject(id);
        return ApiResponse.success(null);
    }

    public record CategoryRequest(@NotBlank @Size(max = 64) String name, @Min(0) int sort) {
    }

    public record ProjectRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 100) String name,
            @Min(1) int durationMinutes,
            @NotNull @DecimalMin("0.01") BigDecimal basePrice,
            @Size(max = 1000) String description,
            @Size(max = 500) String notice,
            Long coverFileId,
            @Min(0) int sort) {
    }

    public record StatusRequest(@NotBlank @Pattern(regexp = "ENABLED|DISABLED|DRAFT|ON_SHELF|OFF_SHELF") String status) {
    }
}
