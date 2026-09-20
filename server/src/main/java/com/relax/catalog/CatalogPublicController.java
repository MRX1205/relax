package com.relax.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class CatalogPublicController {

    private final CatalogPublicService catalogService;

    CatalogPublicController(CatalogPublicService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/projects")
    ApiResponse<List<ProjectPublicMapper.ProjectBrief>> listProjects(
            @RequestParam(required = false, defaultValue = "0") long categoryId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(catalogService.listProjects(categoryId, page, size));
    }

    @GetMapping("/projects/{id}")
    ApiResponse<CatalogPublicService.ProjectDetail> getProject(@PathVariable long id) {
        return ApiResponse.success(catalogService.getProject(id));
    }

    @GetMapping("/technicians")
    ApiResponse<List<TechnicianPublicMapper.TechnicianItem>> listTechnicians(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ApiResponse.success(catalogService.listTechnicians(page, size));
    }

    @GetMapping("/technicians/{id}")
    ApiResponse<CatalogPublicService.TechnicianDetail> getTechnician(@PathVariable long id) {
        return ApiResponse.success(catalogService.getTechnician(id));
    }
}
