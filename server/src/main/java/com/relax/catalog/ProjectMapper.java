package com.relax.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectMapper {

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, p.duration_minutes AS durationMinutes, "
            + "p.base_price AS basePrice, p.description, p.notice, p.cover_file_id AS coverFileId, "
            + "p.status, p.sort, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.created_at AS createdAt "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id ORDER BY p.sort, p.id DESC")
    List<ProjectView> findAll();

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, p.duration_minutes AS durationMinutes, "
            + "p.base_price AS basePrice, p.description, p.notice, p.cover_file_id AS coverFileId, "
            + "p.status, p.sort, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.created_at AS createdAt "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id WHERE p.id = #{id}")
    Optional<ProjectView> findById(@Param("id") long id);

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, p.duration_minutes AS durationMinutes, "
            + "p.base_price AS basePrice, p.description, p.notice, p.cover_file_id AS coverFileId, "
            + "p.status, p.sort, COALESCE(p.creator_type, 'PLATFORM') AS creatorType, COALESCE(p.creator_id, 0) AS creatorId, "
            + "p.created_at AS createdAt "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id "
            + "WHERE p.creator_type = 'PLATFORM' AND p.status = 'ON_SHELF' ORDER BY p.sort, p.id DESC")
    List<ProjectView> findPlatformAvailable();

    @Insert("INSERT INTO service_project (id, category_id, name, duration_minutes, base_price, description, notice, cover_file_id, sort, creator_type, creator_id, status) "
            + "VALUES (#{id}, #{categoryId}, #{name}, #{durationMinutes}, #{basePrice}, #{description}, #{notice}, #{coverFileId}, #{sort}, #{creatorType}, #{creatorId}, #{status})")
    void insertWithCreator(@Param("id") long id, @Param("categoryId") long categoryId, @Param("name") String name,
            @Param("durationMinutes") int durationMinutes, @Param("basePrice") BigDecimal basePrice,
            @Param("description") String description, @Param("notice") String notice,
            @Param("coverFileId") Long coverFileId, @Param("sort") int sort,
            @Param("creatorType") String creatorType, @Param("creatorId") long creatorId,
            @Param("status") String status);

    @Insert("INSERT INTO service_project (id, category_id, name, duration_minutes, base_price, description, notice, cover_file_id, sort, creator_type, creator_id, status) "
            + "VALUES (#{id}, #{categoryId}, #{name}, #{durationMinutes}, #{basePrice}, #{description}, #{notice}, #{coverFileId}, #{sort}, 'PLATFORM', 0, 'DRAFT')")
    void insert(@Param("id") long id, @Param("categoryId") long categoryId, @Param("name") String name,
            @Param("durationMinutes") int durationMinutes, @Param("basePrice") BigDecimal basePrice,
            @Param("description") String description, @Param("notice") String notice,
            @Param("coverFileId") Long coverFileId, @Param("sort") int sort);

    @Update("UPDATE service_project SET category_id = #{categoryId}, name = #{name}, duration_minutes = #{durationMinutes}, "
            + "base_price = #{basePrice}, description = #{description}, notice = #{notice}, cover_file_id = #{coverFileId}, "
            + "sort = #{sort}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(@Param("id") long id, @Param("categoryId") long categoryId, @Param("name") String name,
            @Param("durationMinutes") int durationMinutes, @Param("basePrice") BigDecimal basePrice,
            @Param("description") String description, @Param("notice") String notice,
            @Param("coverFileId") Long coverFileId, @Param("sort") int sort);

    @Update("UPDATE service_project SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") String status);

    @Delete("DELETE FROM service_project WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Select("SELECT tp.project_id AS projectId, t.id AS technicianId, t.service_name AS serviceName, "
            + "COALESCE(t.avatar_url, '') AS avatarUrl, t.online_status AS onlineStatus "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "WHERE tp.status = 'ENABLED' AND t.status = 'ACTIVE' AND t.online_status = 'ONLINE'")
    List<ProjectOnlineTech> findOnlineTechniciansForAllProjects();

    record ProjectOnlineTech(long projectId, long technicianId, String serviceName, String avatarUrl, String onlineStatus) {}

    record ProjectView(long id, long categoryId, String categoryName, String name, int durationMinutes,
            BigDecimal basePrice, String description, String notice, Long coverFileId,
            String status, int sort, String creatorType, Long creatorId, LocalDateTime createdAt) {
    }
}
