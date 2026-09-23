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

    // === 项目 ===

    public List<ProjectMapper.ProjectView> listProjects() {
        return projectMapper.findAll();
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
}
