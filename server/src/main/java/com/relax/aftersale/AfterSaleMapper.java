package com.relax.aftersale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AfterSaleMapper {

    @Insert("INSERT INTO after_sale_case (id, case_no, order_id, user_id, type, content) "
            + "VALUES (#{id}, #{caseNo}, #{orderId}, #{userId}, #{type}, #{content})")
    void insertCase(@Param("id") long id, @Param("caseNo") String caseNo, @Param("orderId") long orderId,
            @Param("userId") long userId, @Param("type") String type, @Param("content") String content);

    @Select("SELECT id, case_no AS caseNo, order_id AS orderId, user_id AS userId, type, content, "
            + "status, assignee_id AS assigneeId, created_at AS createdAt "
            + "FROM after_sale_case WHERE case_no = #{caseNo}")
    Optional<CaseView> findByCaseNo(@Param("caseNo") String caseNo);

    @Select("SELECT id, case_no AS caseNo, order_id AS orderId, user_id AS userId, type, content, "
            + "status, assignee_id AS assigneeId, created_at AS createdAt "
            + "FROM after_sale_case WHERE order_id = #{orderId} ORDER BY id DESC")
    List<CaseView> findByOrderId(@Param("orderId") long orderId);

    @Select("SELECT id, case_no AS caseNo, order_id AS orderId, user_id AS userId, type, content, "
            + "status, assignee_id AS assigneeId, created_at AS createdAt "
            + "FROM after_sale_case ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<CaseView> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE after_sale_case SET status = #{status}, assignee_id = #{assigneeId}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE case_no = #{caseNo}")
    int updateStatus(@Param("caseNo") String caseNo, @Param("status") String status,
            @Param("assigneeId") Long assigneeId);

    @Insert("INSERT INTO after_sale_record (id, case_id, operator_id, action, content) "
            + "VALUES (#{id}, #{caseId}, #{operatorId}, #{action}, #{content})")
    void insertRecord(@Param("id") long id, @Param("caseId") long caseId,
            @Param("operatorId") long operatorId, @Param("action") String action,
            @Param("content") String content);

    @Select("SELECT id, case_id AS caseId, operator_id AS operatorId, action, content, created_at AS createdAt "
            + "FROM after_sale_record WHERE case_id = #{caseId} ORDER BY id")
    List<RecordView> findRecords(@Param("caseId") long caseId);

    record CaseView(long id, String caseNo, long orderId, long userId, String type, String content,
            String status, Long assigneeId, LocalDateTime createdAt) {}

    record RecordView(long id, long caseId, long operatorId, String action, String content,
            LocalDateTime createdAt) {}
}
