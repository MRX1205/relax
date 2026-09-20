package com.relax.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper {

    @Insert("INSERT INTO review (id, order_id, user_id, technician_id, score, content) "
            + "VALUES (#{id}, #{orderId}, #{userId}, #{technicianId}, #{score}, #{content})")
    void insert(@Param("id") long id, @Param("orderId") long orderId, @Param("userId") long userId,
            @Param("technicianId") long technicianId, @Param("score") int score, @Param("content") String content);

    @Select("SELECT id, order_id AS orderId, user_id AS userId, technician_id AS technicianId, "
            + "score, content, status, created_at AS createdAt FROM review WHERE order_id = #{orderId}")
    Optional<ReviewView> findByOrderId(@Param("orderId") long orderId);

    @Select("SELECT id, order_id AS orderId, user_id AS userId, technician_id AS technicianId, "
            + "score, content, status, created_at AS createdAt FROM review "
            + "WHERE technician_id = #{technicianId} AND status = 'VISIBLE' "
            + "ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ReviewView> findByTechnician(@Param("technicianId") long technicianId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, order_id AS orderId, user_id AS userId, technician_id AS technicianId, "
            + "score, content, status, created_at AS createdAt FROM review ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ReviewView> findAll(@Param("limit") int limit, @Param("offset") int offset);

    record ReviewView(long id, long orderId, long userId, long technicianId, int score,
            String content, String status, LocalDateTime createdAt) {}
}
