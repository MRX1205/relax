package com.relax.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SettlementMapper {

    // === 收入 ===
    @Insert("INSERT INTO technician_income (id, order_id, technician_id, gross_amount, platform_fee, payable_amount) "
            + "VALUES (#{id}, #{orderId}, #{technicianId}, #{grossAmount}, #{platformFee}, #{payableAmount})")
    void insertIncome(@Param("id") long id, @Param("orderId") long orderId,
            @Param("technicianId") long technicianId, @Param("grossAmount") BigDecimal grossAmount,
            @Param("platformFee") BigDecimal platformFee, @Param("payableAmount") BigDecimal payableAmount);

    @Select("SELECT id, order_id AS orderId, technician_id AS technicianId, gross_amount AS grossAmount, "
            + "platform_fee AS platformFee, payable_amount AS payableAmount, status, created_at AS createdAt "
            + "FROM technician_income WHERE id = #{id}")
    IncomeView findIncomeById(@Param("id") long id);

    @Select("SELECT id, order_id AS orderId, technician_id AS technicianId, gross_amount AS grossAmount, "
            + "platform_fee AS platformFee, payable_amount AS payableAmount, status, created_at AS createdAt "
            + "FROM technician_income WHERE technician_id = #{techId} AND status = 'PENDING' ORDER BY id")
    List<IncomeView> findPendingIncome(@Param("techId") long techId);

    @Select("SELECT id, order_id AS orderId, technician_id AS technicianId, gross_amount AS grossAmount, "
            + "platform_fee AS platformFee, payable_amount AS payableAmount, status, created_at AS createdAt "
            + "FROM technician_income WHERE technician_id = #{techId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<IncomeView> findIncomeByTech(@Param("techId") long techId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE technician_income SET status = #{status} WHERE id = #{id}")
    int updateIncomeStatus(@Param("id") long id, @Param("status") String status);

    // === 结算单 ===
    @Insert("INSERT INTO settlement_batch (id, settlement_no, technician_id, total_amount, status, operator_id) "
            + "VALUES (#{id}, #{settlementNo}, #{technicianId}, #{totalAmount}, 'PENDING', #{operatorId})")
    void insertSettlement(@Param("id") long id, @Param("settlementNo") String settlementNo,
            @Param("technicianId") long technicianId, @Param("totalAmount") BigDecimal totalAmount,
            @Param("operatorId") long operatorId);

    @Insert("INSERT INTO settlement_item (id, settlement_id, income_id, order_id, amount) "
            + "VALUES (#{id}, #{settlementId}, #{incomeId}, #{orderId}, #{amount})")
    void insertItem(@Param("id") long id, @Param("settlementId") long settlementId,
            @Param("incomeId") long incomeId, @Param("orderId") long orderId,
            @Param("amount") BigDecimal amount);

    @Select("SELECT id, settlement_no AS settlementNo, technician_id AS technicianId, total_amount AS totalAmount, "
            + "status, operator_id AS operatorId, paid_at AS paidAt, proof_file_id AS proofFileId, "
            + "reference_no AS referenceNo, created_at AS createdAt "
            + "FROM settlement_batch WHERE technician_id = #{techId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<SettlementView> findSettlementsByTech(@Param("techId") long techId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, settlement_no AS settlementNo, technician_id AS technicianId, total_amount AS totalAmount, "
            + "status, operator_id AS operatorId, paid_at AS paidAt, reference_no AS referenceNo, created_at AS createdAt "
            + "FROM settlement_batch ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<SettlementView> findAllSettlements(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT id, settlement_no AS settlementNo, technician_id AS technicianId, total_amount AS totalAmount, "
            + "status, created_at AS createdAt FROM settlement_batch WHERE id = #{id}")
    Optional<SettlementView> findSettlementById(@Param("id") long id);

    @Update("UPDATE settlement_batch SET status = 'PAID', paid_at = CURRENT_TIMESTAMP, "
            + "reference_no = #{referenceNo}, proof_file_id = #{proofFileId}, "
            + "operator_id = #{operatorId}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markPaid(@Param("id") long id, @Param("referenceNo") String referenceNo,
            @Param("proofFileId") Long proofFileId, @Param("operatorId") long operatorId);

    @Update("UPDATE settlement_batch SET status = 'VOID', updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markVoid(@Param("id") long id);

    record IncomeView(long id, long orderId, long technicianId, BigDecimal grossAmount,
            BigDecimal platformFee, BigDecimal payableAmount, String status, LocalDateTime createdAt) {}

    record SettlementView(long id, String settlementNo, long technicianId, BigDecimal totalAmount,
            String status, Long operatorId, LocalDateTime paidAt, Long proofFileId,
            String referenceNo, LocalDateTime createdAt) {}
}
