package com.relax.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class CatalogService {

    private final CategoryMapper categoryMapper;
    private final ProjectMapper projectMapper;

    CatalogService(CategoryMapper categoryMapper, ProjectMapper projectMapper) {
        this.categoryMapper = categoryMapper;
        this.projectMapper = projectMapper;
    }

    // === 分类 ===

    public List<CategoryMapper.CategoryView> listCategories() {
        return categoryMapper.findAll();
    }

    @Transactional
    public CategoryMapper.CategoryView createCategory(CategoryRequest request) {
        long id = IdWorker.getId();
        categoryMapper.insert(id, request.name().strip(), request.sort());
        return categoryMapper.findById(id).orElseThrow();
    }

    @Transactional
    public CategoryMapper.CategoryView updateCategory(long id, CategoryRequest request) {
        requireCategory(id);
        categoryMapper.update(id, request.name().strip(), request.sort());
        return categoryMapper.findById(id).orElseThrow();
    }

    @Transactional
    public CategoryMapper.CategoryView updateCategoryStatus(long id, String status) {
        requireCategory(id);
        categoryMapper.updateStatus(id, status);
        return categoryMapper.findById(id).orElseThrow();
    }

    private void requireCategory(long id) {
        if (categoryMapper.findById(id).isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "分类不存在");
        }
    }

    public List<ProjectMapper.ProjectView> listProjects() {
        return projectMapper.findAll();
    }

    public List<AdminProjectItemView> listAdminProjects() {
        List<ProjectMapper.ProjectView> projects = projectMapper.findAll();
        List<ProjectMapper.ProjectOnlineTech> allOnline = projectMapper.findOnlineTechniciansForAllProjects();
        java.util.Map<Long, List<ProjectMapper.ProjectOnlineTech>> onlineMap = allOnline.stream()
                .collect(java.util.stream.Collectors.groupingBy(ProjectMapper.ProjectOnlineTech::projectId));

        return projects.stream().map(p -> new AdminProjectItemView(
                p.id(), p.categoryId(), p.categoryName(), p.name(), p.durationMinutes(),
                p.basePrice(), p.description(), p.notice(), p.coverFileId(),
                p.status(), p.sort(), p.creatorType(), p.creatorId(), p.createdAt(),
                onlineMap.getOrDefault(p.id(), List.of())
        )).toList();
    }

    public ProjectMapper.ProjectView getProject(long id) {
        return projectMapper.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "项目不存在"));
    }

    @Transactional
    public ProjectMapper.ProjectView createProject(ProjectRequest request) {
        if (categoryMapper.findById(request.categoryId()).isEmpty()) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在");
        }
        long id = IdWorker.getId();
        projectMapper.insert(id, request.categoryId(), request.name().strip(), request.durationMinutes(),
                request.basePrice(), request.description() == null ? "" : request.description().strip(),
                request.notice() == null ? "" : request.notice().strip(), request.coverFileId(), request.sort());
        return projectMapper.findById(id).orElseThrow();
    }

    @Transactional
    public ProjectMapper.ProjectView updateProject(long id, ProjectRequest request) {
        getProject(id);
        if (categoryMapper.findById(request.categoryId()).isEmpty()) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在");
        }
        projectMapper.update(id, request.categoryId(), request.name().strip(), request.durationMinutes(),
                request.basePrice(), request.description() == null ? "" : request.description().strip(),
                request.notice() == null ? "" : request.notice().strip(), request.coverFileId(), request.sort());
        return projectMapper.findById(id).orElseThrow();
    }

    @Transactional
    public ProjectMapper.ProjectView updateProjectStatus(long id, String status) {
        getProject(id);
        projectMapper.updateStatus(id, status);
        return projectMapper.findById(id).orElseThrow();
    }

    @Transactional
    public void deleteProject(long id) {
        getProject(id);
        projectMapper.delete(id);
    }

    public record CategoryRequest(String name, int sort) {
    }

    public record ProjectRequest(long categoryId, String name, int durationMinutes, BigDecimal basePrice,
            String description, String notice, Long coverFileId, int sort) {
    }

    public record AdminProjectItemView(
            long id, long categoryId, String categoryName, String name, int durationMinutes,
            BigDecimal basePrice, String description, String notice, Long coverFileId,
            String status, int sort, String creatorType, Long creatorId, java.time.LocalDateTime createdAt,
            List<ProjectMapper.ProjectOnlineTech> onlineTechnicians) {
    }
}
