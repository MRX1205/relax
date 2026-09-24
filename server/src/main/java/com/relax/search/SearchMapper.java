package com.relax.search;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchMapper {

    @Select("SELECT p.id, p.category_id AS categoryId, c.name AS categoryName, p.name, "
            + "p.duration_minutes AS durationMinutes, p.base_price AS basePrice, "
            + "p.description, p.notice, p.cover_file_id AS coverFileId, p.sort "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id "
            + "WHERE p.status = 'ON_SHELF' "
            + "AND (p.name LIKE CONCAT('%', #{q}, '%') OR p.description LIKE CONCAT('%', #{q}, '%') OR c.name LIKE CONCAT('%', #{q}, '%')) "
            + "ORDER BY p.sort, p.id DESC LIMIT 20")
    List<ProjectResult> searchProjects(@Param("q") String q);

    @Select("SELECT t.id, t.service_name AS serviceName, COALESCE(t.avatar_url, u.avatar_url) AS avatarUrl, "
            + "t.intro, t.experience_years AS experienceYears, t.online_status AS onlineStatus, "
            + "t.age, t.age_tag AS ageTag, "
            + "(SELECT MIN(COALESCE(tp.override_price, p.base_price)) FROM technician_project tp "
            + " JOIN service_project p ON p.id = tp.project_id AND p.status = 'ON_SHELF' "
            + " WHERE tp.technician_id = t.id AND tp.status = 'ENABLED') AS startPrice "
            + "FROM technician t LEFT JOIN platform_user u ON u.id = t.user_id "
            + "WHERE t.status = 'ACTIVE' "
            + "AND ("
            + "   t.service_name LIKE CONCAT('%', #{q}, '%') "
            + "   OR t.real_name LIKE CONCAT('%', #{q}, '%') "
            + "   OR t.intro LIKE CONCAT('%', #{q}, '%') "
            + "   OR EXISTS ("
            + "       SELECT 1 FROM technician_project tp "
            + "       JOIN service_project sp ON sp.id = tp.project_id "
            + "       WHERE tp.technician_id = t.id AND tp.status = 'ENABLED' "
            + "       AND (sp.name LIKE CONCAT('%', #{q}, '%') OR sp.description LIKE CONCAT('%', #{q}, '%'))"
            + "   )"
            + ") "
            + "ORDER BY (t.online_status = 'ONLINE') DESC, t.id DESC LIMIT 20")
    List<TechnicianResult> searchTechnicians(@Param("q") String q);

    record ProjectResult(long id, long categoryId, String categoryName, String name,
            int durationMinutes, BigDecimal basePrice, String description, String notice,
            Long coverFileId, int sort) {}

    record TechnicianResult(long id, String serviceName, String avatarUrl,
            String intro, int experienceYears, String onlineStatus,
            Integer age, String ageTag, BigDecimal startPrice) {}
}
