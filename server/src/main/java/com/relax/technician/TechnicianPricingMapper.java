package com.relax.technician;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TechnicianPricingMapper {

    @Select("SELECT tp.id, tp.technician_id AS technicianId, t.service_name AS technicianName, "
            + "tp.project_id AS projectId, p.name AS projectName, p.duration_minutes AS durationMinutes, "
            + "c.name AS categoryName, p.base_price AS basePrice, tp.override_price AS overridePrice, "
            + "COALESCE(tp.override_price, p.base_price) AS effectivePrice, "
            + "tp.status, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "JOIN service_category c ON c.id = p.category_id "
            + "WHERE tp.technician_id = #{technicianId} "
            + "ORDER BY tp.id DESC")
    List<TechnicianProjectView> findByTechnician(@Param("technicianId") long technicianId);

    @Select("SELECT tp.id, tp.technician_id AS technicianId, t.service_name AS technicianName, "
            + "tp.project_id AS projectId, p.name AS projectName, p.duration_minutes AS durationMinutes, "
            + "c.name AS categoryName, p.base_price AS basePrice, tp.override_price AS overridePrice, "
            + "COALESCE(tp.override_price, p.base_price) AS effectivePrice, "
            + "tp.status, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "JOIN service_category c ON c.id = p.category_id "
            + "WHERE tp.technician_id = #{technicianId} AND tp.project_id = #{projectId} AND tp.status = 'ENABLED'")
    Optional<TechnicianProjectView> findActive(@Param("technicianId") long technicianId,
            @Param("projectId") long projectId);

    @Select("SELECT tp.id, tp.technician_id AS technicianId, t.service_name AS technicianName, "
            + "tp.project_id AS projectId, p.name AS projectName, p.duration_minutes AS durationMinutes, "
            + "c.name AS categoryName, p.base_price AS basePrice, tp.override_price AS overridePrice, "
            + "COALESCE(tp.override_price, p.base_price) AS effectivePrice, "
            + "tp.status, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "JOIN service_category c ON c.id = p.category_id "
            + "WHERE tp.id = #{id}")
    Optional<TechnicianProjectView> findById(@Param("id") long id);

    @Select("SELECT id, technician_id AS technicianId, project_id AS projectId, status "
            + "FROM technician_project WHERE technician_id = #{technicianId} AND project_id = #{projectId}")
    Optional<TechnicianProjectRecord> findByTechAndProject(@Param("technicianId") long technicianId, @Param("projectId") long projectId);

    @Insert("INSERT INTO technician_project (id, technician_id, project_id, override_price, status) "
            + "VALUES (#{id}, #{technicianId}, #{projectId}, #{overridePrice}, #{status})")
    void insertWithStatus(@Param("id") long id, @Param("technicianId") long technicianId,
            @Param("projectId") long projectId, @Param("overridePrice") BigDecimal overridePrice,
            @Param("status") String status);

    @Insert("INSERT INTO technician_project (id, technician_id, project_id, override_price) "
            + "VALUES (#{id}, #{technicianId}, #{projectId}, #{overridePrice})")
    void insert(@Param("id") long id, @Param("technicianId") long technicianId,
            @Param("projectId") long projectId, @Param("overridePrice") BigDecimal overridePrice);

    @Update("UPDATE technician_project SET override_price = #{overridePrice}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id}")
    int updatePrice(@Param("id") long id, @Param("overridePrice") BigDecimal overridePrice);

    @Update("UPDATE technician_project SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") String status);

    @Delete("DELETE FROM technician_project WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Delete("DELETE FROM technician_project WHERE id = #{id} AND technician_id = #{technicianId}")
    int deleteByTechAndId(@Param("id") long id, @Param("technicianId") long technicianId);

    record TechnicianProjectView(long id, long technicianId, String technicianName,
            long projectId, String projectName, Integer durationMinutes, String categoryName,
            BigDecimal basePrice, BigDecimal overridePrice, BigDecimal effectivePrice,
            String status, String creatorType, Long creatorId, String description, String notice, Long coverFileId) {
    }

    record TechnicianProjectRecord(long id, long technicianId, long projectId, String status) {
    }
}
