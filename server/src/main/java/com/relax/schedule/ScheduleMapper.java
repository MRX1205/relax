package com.relax.schedule;

import java.time.LocalDate;
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
public interface ScheduleMapper {

    @Select("SELECT id, technician_id AS technicianId, schedule_date AS scheduleDate, "
            + "start_time AS startTime, end_time AS endTime, type, status, created_at AS createdAt "
            + "FROM technician_schedule WHERE technician_id = #{technicianId} AND schedule_date >= #{fromDate} "
            + "ORDER BY schedule_date, start_time")
    List<ScheduleView> findByTechnician(@Param("technicianId") long technicianId, @Param("fromDate") LocalDate fromDate);

    @Select("SELECT id, technician_id AS technicianId, schedule_date AS scheduleDate, "
            + "start_time AS startTime, end_time AS endTime, type, status, created_at AS createdAt "
            + "FROM technician_schedule WHERE technician_id = #{technicianId} AND schedule_date = #{date} "
            + "ORDER BY start_time")
    List<ScheduleView> findByTechnicianAndDate(@Param("technicianId") long technicianId, @Param("date") LocalDate date);

    @Select("SELECT id, technician_id AS technicianId, schedule_date AS scheduleDate, "
            + "start_time AS startTime, end_time AS endTime, type, status, created_at AS createdAt "
            + "FROM technician_schedule WHERE id = #{id}")
    Optional<ScheduleView> findById(@Param("id") long id);

    @Insert("INSERT INTO technician_schedule (id, technician_id, schedule_date, start_time, end_time, type) "
            + "VALUES (#{id}, #{technicianId}, #{scheduleDate}, #{startTime}, #{endTime}, #{type})")
    void insert(@Param("id") long id, @Param("technicianId") long technicianId,
            @Param("scheduleDate") LocalDate scheduleDate, @Param("startTime") String startTime,
            @Param("endTime") String endTime, @Param("type") String type);

    @Update("UPDATE technician_schedule SET start_time = #{startTime}, end_time = #{endTime}, type = #{type}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(@Param("id") long id, @Param("startTime") String startTime,
            @Param("endTime") String endTime, @Param("type") String type);

    @Delete("DELETE FROM technician_schedule WHERE id = #{id}")
    int delete(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM technician_schedule WHERE technician_id = #{technicianId} "
            + "AND schedule_date = #{date} AND status = 'ENABLED' "
            + "AND NOT (end_time <= #{startTime} OR start_time >= #{endTime})")
    int countConflict(@Param("technicianId") long technicianId, @Param("date") LocalDate date,
            @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("SELECT COUNT(*) FROM technician_schedule WHERE technician_id = #{technicianId} "
            + "AND schedule_date = #{date} AND status = 'ENABLED' AND id != #{excludeId} "
            + "AND NOT (end_time <= #{startTime} OR start_time >= #{endTime})")
    int countConflictExcluding(@Param("technicianId") long technicianId, @Param("date") LocalDate date,
            @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("excludeId") long excludeId);

    record ScheduleView(long id, long technicianId, LocalDate scheduleDate, String startTime,
            String endTime, String type, String status, LocalDateTime createdAt) {
    }
}
