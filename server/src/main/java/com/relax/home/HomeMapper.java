package com.relax.home;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HomeMapper {

    @Select("SELECT id, name, sort FROM service_category WHERE status = 'ENABLED' ORDER BY sort, id LIMIT #{limit}")
    List<CategoryBrief> findTopCategories(@Param("limit") int limit);

    @Select("SELECT p.id, p.name, p.duration_minutes AS durationMinutes, p.base_price AS basePrice, "
            + "c.name AS categoryName "
            + "FROM service_project p JOIN service_category c ON c.id = p.category_id "
            + "WHERE p.status = 'ON_SHELF' ORDER BY p.sort, p.id DESC LIMIT #{limit}")
    List<ProjectBrief> findFeaturedProjects(@Param("limit") int limit);

    @Select("SELECT t.id, t.service_name AS serviceName, u.avatar_url AS avatarUrl, "
            + "t.experience_years AS experienceYears, t.online_status AS onlineStatus, "
            + "(SELECT MIN(COALESCE(tp.override_price, p.base_price)) FROM technician_project tp "
            + " JOIN service_project p ON p.id = tp.project_id AND p.status = 'ON_SHELF' "
            + " WHERE tp.technician_id = t.id AND tp.status = 'ENABLED') AS startPrice "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id "
            + "WHERE t.status = 'ACTIVE' AND t.online_status = 'ONLINE' "
            + "ORDER BY t.id DESC LIMIT #{limit}")
    List<TechnicianBrief> findFeaturedTechnicians(@Param("limit") int limit);

    record CategoryBrief(long id, String name, int sort) {}

    record ProjectBrief(long id, String name, int durationMinutes, BigDecimal basePrice, String categoryName) {}

    record TechnicianBrief(long id, String serviceName, String avatarUrl,
            int experienceYears, String onlineStatus, BigDecimal startPrice) {}
}
