package com.relax.technician;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TechnicianServiceAreaMapper {

    @Select("SELECT service_area_id FROM technician_service_area WHERE technician_id = #{technicianId}")
    List<Long> findAreaIdsByTechnician(@Param("technicianId") long technicianId);

    @Delete("DELETE FROM technician_service_area WHERE technician_id = #{technicianId}")
    int deleteByTechnician(@Param("technicianId") long technicianId);

    @Insert("INSERT INTO technician_service_area (technician_id, service_area_id) VALUES (#{technicianId}, #{serviceAreaId})")
    int insert(@Param("technicianId") long technicianId, @Param("serviceAreaId") long serviceAreaId);
}
