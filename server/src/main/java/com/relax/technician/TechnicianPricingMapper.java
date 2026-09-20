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
            + "tp.project_id AS projectId, p.name AS projectName, tp.override_price AS overridePrice, "
            + "COALESCE(tp.override_price, p.base_price) AS effectivePrice, "
            + "tp.status "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "WHERE tp.technician_id = #{technicianId}")
    List<TechnicianProjectView> findByTechnician(@Param("technicianId") long technicianId);

    @Select("SELECT tp.id, tp.technician_id AS technicianId, t.service_name AS technicianName, "
            + "tp.project_id AS projectId, p.name AS projectName, tp.override_price AS overridePrice, "
            + "COALESCE(tp.override_price, p.base_price) AS effectivePrice, "
            + "tp.status "
            + "FROM technician_project tp "
            + "JOIN technician t ON t.id = tp.technician_id "
            + "JOIN service_project p ON p.id = tp.project_id "
            + "WHERE tp.technician_id = #{technicianId} AND tp.project_id = #{projectId} AND tp.status = 'ENABLED'")
    Optional<TechnicianProjectView> findActive(@Param("technicianId") long technicianId,
            @Param("projectId") long projectId);

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

    record TechnicianProjectView(long id, long technicianId, String technicianName,
            long projectId, String projectName, BigDecimal overridePrice,
            BigDecimal effectivePrice, String status) {
    }
}
