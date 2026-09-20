package com.relax.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectPublicMapper {

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, "
            + "p.duration_minutes AS durationMinutes, p.base_price AS basePrice, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId, p.sort "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id "
            + "WHERE p.status = 'ON_SHELF' "
            + "AND (#{categoryId} = 0 OR p.category_id = #{categoryId}) "
            + "ORDER BY p.sort, p.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ProjectBrief> findPublished(@Param("categoryId") long categoryId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, "
            + "p.duration_minutes AS durationMinutes, p.base_price AS basePrice, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId, p.sort "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id "
            + "WHERE p.id = #{id} AND p.status = 'ON_SHELF'")
    Optional<ProjectBrief> findPublishedById(@Param("id") long id);

    @Select("SELECT t.id, t.service_name AS serviceName, u.avatar_url AS avatarUrl, "
            + "t.experience_years AS experienceYears, "
            + "COALESCE(tp.override_price, p.base_price) AS price "
            + "FROM technician t "
            + "JOIN platform_user u ON u.id = t.user_id "
            + "JOIN technician_project tp ON tp.technician_id = t.id AND tp.project_id = #{projectId} AND tp.status = 'ENABLED' "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "WHERE t.status = 'ACTIVE' AND t.online_status = 'ONLINE' "
            + "ORDER BY price LIMIT #{limit}")
    List<ProjectTechnician> findTechniciansForProject(@Param("projectId") long projectId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM technician t "
            + "JOIN technician_project tp ON tp.technician_id = t.id AND tp.project_id = #{projectId} AND tp.status = 'ENABLED' "
            + "WHERE t.status = 'ACTIVE' AND t.online_status = 'ONLINE'")
    int countTechniciansForProject(@Param("projectId") long projectId);

    record ProjectBrief(long id, long categoryId, String categoryName, String name,
            int durationMinutes, BigDecimal basePrice, String description, String notice,
            Long coverFileId, int sort) {}

    record ProjectTechnician(long id, String serviceName, String avatarUrl,
            int experienceYears, BigDecimal price) {}
}
