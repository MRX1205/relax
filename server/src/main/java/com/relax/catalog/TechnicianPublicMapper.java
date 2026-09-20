package com.relax.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TechnicianPublicMapper {

    @Select("SELECT t.id, t.service_name AS serviceName, u.avatar_url AS avatarUrl, "
            + "t.intro, t.experience_years AS experienceYears, t.online_status AS onlineStatus, "
            + "(SELECT MIN(COALESCE(tp.override_price, p.base_price)) FROM technician_project tp "
            + " JOIN service_project p ON p.id = tp.project_id AND p.status = 'ON_SHELF' "
            + " WHERE tp.technician_id = t.id AND tp.status = 'ENABLED') AS startPrice "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id "
            + "WHERE t.status = 'ACTIVE' "
            + "AND (#{onlineOnly} = FALSE OR t.online_status = 'ONLINE') "
            + "ORDER BY t.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<TechnicianItem> findPublished(@Param("onlineOnly") boolean onlineOnly,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT t.id, t.service_name AS serviceName, u.avatar_url AS avatarUrl, "
            + "t.intro, t.experience_years AS experienceYears, t.online_status AS onlineStatus "
            + "FROM technician t JOIN platform_user u ON u.id = t.user_id "
            + "WHERE t.id = #{id} AND t.status = 'ACTIVE'")
    Optional<TechnicianDetail> findPublishedById(@Param("id") long id);

    @Select("SELECT tp.id, tp.project_id AS projectId, p.name AS projectName, "
            + "p.duration_minutes AS durationMinutes, "
            + "COALESCE(tp.override_price, p.base_price) AS price "
            + "FROM technician_project tp "
            + "JOIN service_project p ON p.id = tp.project_id AND p.status = 'ON_SHELF' "
            + "WHERE tp.technician_id = #{technicianId} AND tp.status = 'ENABLED' "
            + "ORDER BY p.sort")
    List<TechnicianProjectItem> findProjectsForTechnician(@Param("technicianId") long technicianId);

    @Select("SELECT schedule_date AS scheduleDate, start_time AS startTime, end_time AS endTime "
            + "FROM technician_schedule "
            + "WHERE technician_id = #{technicianId} AND schedule_date >= #{fromDate} "
            + "AND status = 'ENABLED' "
            + "ORDER BY schedule_date, start_time LIMIT 30")
    List<AvailabilitySlot> findAvailability(@Param("technicianId") long technicianId,
            @Param("fromDate") LocalDate fromDate);

    record TechnicianItem(long id, String serviceName, String avatarUrl,
            String intro, int experienceYears, String onlineStatus, BigDecimal startPrice) {}

    record TechnicianDetail(long id, String serviceName, String avatarUrl,
            String intro, int experienceYears, String onlineStatus) {}

    record TechnicianProjectItem(long id, long projectId, String projectName,
            int durationMinutes, BigDecimal price) {}

    record AvailabilitySlot(LocalDate scheduleDate, String startTime, String endTime) {}
}
