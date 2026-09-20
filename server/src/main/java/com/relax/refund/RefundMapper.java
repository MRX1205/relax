package com.relax.refund;

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
public interface RefundMapper {

    @Insert("INSERT INTO refund_order (id, refund_no, order_id, payment_id, amount, reason, status) "
            + "VALUES (#{id}, #{refundNo}, #{orderId}, #{paymentId}, #{amount}, #{reason}, 'PENDING')")
    void insert(@Param("id") long id, @Param("refundNo") String refundNo, @Param("orderId") long orderId,
            @Param("paymentId") long paymentId, @Param("amount") BigDecimal amount, @Param("reason") String reason);

    @Select("SELECT id, refund_no AS refundNo, order_id AS orderId, payment_id AS paymentId, "
            + "amount, reason, status, operator_id AS operatorId, wechat_refund_id AS wechatRefundId, "
            + "created_at AS createdAt FROM refund_order WHERE order_id = #{orderId} ORDER BY id DESC")
    List<RefundView> findByOrderId(@Param("orderId") long orderId);

    @Select("SELECT id, refund_no AS refundNo, order_id AS orderId, payment_id AS paymentId, "
            + "amount, reason, status, operator_id AS operatorId, wechat_refund_id AS wechatRefundId, "
            + "created_at AS createdAt FROM refund_order WHERE refund_no = #{refundNo}")
    Optional<RefundView> findByRefundNo(@Param("refundNo") String refundNo);

    @Select("SELECT id, refund_no AS refundNo, order_id AS orderId, payment_id AS paymentId, "
            + "amount, reason, status, operator_id AS operatorId, created_at AS createdAt "
            + "FROM refund_order ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<RefundView> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE refund_order SET status = #{status}, operator_id = #{operatorId}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE refund_no = #{refundNo} AND status = 'PENDING'")
    int approve(@Param("refundNo") String refundNo, @Param("status") String status,
            @Param("operatorId") long operatorId);

    @Update("UPDATE refund_order SET status = 'SUCCESS', wechat_refund_id = #{wechatRefundId}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE refund_no = #{refundNo} AND status = 'PROCESSING'")
    int markSuccess(@Param("refundNo") String refundNo, @Param("wechatRefundId") String wechatRefundId);

    @Update("UPDATE service_order SET refunded_amount = refunded_amount + #{amount}, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = #{orderId}")
    int addRefundedAmount(@Param("orderId") long orderId, @Param("amount") BigDecimal amount);

    record RefundView(long id, String refundNo, long orderId, long paymentId, BigDecimal amount,
            String reason, String status, Long operatorId, String wechatRefundId, LocalDateTime createdAt) {}
}
