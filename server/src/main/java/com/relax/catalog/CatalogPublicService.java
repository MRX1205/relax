package com.relax.catalog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.relax.common.api.BusinessException;
import com.relax.technician.TechnicianPhotoMapper;

@Service
public class CatalogPublicService {

    private final ProjectPublicMapper projectMapper;
    private final TechnicianPublicMapper techMapper;
    private final TechnicianPhotoMapper photoMapper;

    CatalogPublicService(ProjectPublicMapper projectMapper, TechnicianPublicMapper techMapper,
            TechnicianPhotoMapper photoMapper) {
        this.projectMapper = projectMapper;
        this.techMapper = techMapper;
        this.photoMapper = photoMapper;
    }

    // === 项目浏览 ===

    public List<ProjectPublicMapper.ProjectBrief> listProjects(long categoryId, int page, int size) {
        return projectMapper.findPublished(categoryId, size, page * size);
    }

    public ProjectDetail getProject(long id) {
        ProjectPublicMapper.ProjectBrief project = projectMapper.findPublishedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "项目不存在"));
        int techCount = projectMapper.countTechniciansForProject(id);
        List<ProjectPublicMapper.ProjectTechnician> technicians = projectMapper.findTechniciansForProject(id, 20);
        return new ProjectDetail(project, techCount, technicians);
    }

    // === 技师浏览 ===

    public List<TechnicianPublicMapper.TechnicianItem> listTechnicians(int page, int size) {
        return techMapper.findPublished(false, size, page * size);
    }

    public TechnicianDetail getTechnician(long id) {
        TechnicianPublicMapper.TechnicianDetail tech = techMapper.findPublishedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TECHNICIAN_NOT_FOUND", "技师不存在"));
        List<TechnicianPublicMapper.TechnicianProjectItem> projects = techMapper.findProjectsForTechnician(id);
        List<TechnicianPublicMapper.AvailabilitySlot> availability = techMapper.findAvailability(id, LocalDate.now());
        List<String> photos = photoMapper.findByTechnician(id).stream()
                .map(p -> p.fileUrl() != null && !p.fileUrl().isBlank() ? p.fileUrl() : "/api/v1/files/public/" + p.fileId())
                .filter(url -> !url.endsWith("/0"))
                .toList();
        return new TechnicianDetail(tech, projects, availability, photos);
    }

    public record ProjectDetail(
            ProjectPublicMapper.ProjectBrief project,
            int technicianCount,
            List<ProjectPublicMapper.ProjectTechnician> technicians) {}

    public record TechnicianDetail(
            TechnicianPublicMapper.TechnicianDetail technician,
            List<TechnicianPublicMapper.TechnicianProjectItem> projects,
            List<TechnicianPublicMapper.AvailabilitySlot> availability,
            List<String> photos) {}
}
