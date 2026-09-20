package com.relax.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper {

    @Insert("INSERT INTO payment_order (id, payment_no, order_id, channel, amount, expire_at) "
            + "VALUES (#{id}, #{paymentNo}, #{orderId}, #{channel}, #{amount}, #{expireAt})")
    void insert(@Param("id") long id, @Param("paymentNo") String paymentNo,
            @Param("orderId") long orderId, @Param("channel") String channel,
            @Param("amount") BigDecimal amount, @Param("expireAt") LocalDateTime expireAt);

    @Select("SELECT id, payment_no AS paymentNo, order_id AS orderId, channel, amount, "
            + "status, transaction_id AS transactionId, expire_at AS expireAt, "
            + "paid_at AS paidAt, created_at AS createdAt "
            + "FROM payment_order WHERE payment_no = #{paymentNo}")
    Optional<PaymentView> findByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT id, payment_no AS paymentNo, order_id AS orderId, channel, amount, "
            + "status, transaction_id AS transactionId, expire_at AS expireAt, "
            + "paid_at AS paidAt, created_at AS createdAt "
            + "FROM payment_order WHERE order_id = #{orderId} AND status = 'PENDING'")
    Optional<PaymentView> findPendingByOrderId(@Param("orderId") long orderId);

    @Update("UPDATE payment_order SET status = 'SUCCESS', transaction_id = #{transactionId}, "
            + "paid_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
            + "WHERE payment_no = #{paymentNo} AND status = 'PENDING'")
    int markSuccess(@Param("paymentNo") String paymentNo, @Param("transactionId") String transactionId);

    @Update("UPDATE payment_order SET status = 'FAILED', updated_at = CURRENT_TIMESTAMP "
            + "WHERE payment_no = #{paymentNo} AND status = 'PENDING'")
    int markFailed(@Param("paymentNo") String paymentNo);

    @Select("SELECT COUNT(*) FROM payment_notify_log WHERE payment_no = #{paymentNo} AND transaction_id = #{transactionId}")
    int countDuplicateNotify(@Param("paymentNo") String paymentNo, @Param("transactionId") String transactionId);

    @Insert("INSERT INTO payment_notify_log (id, payment_no, transaction_id, result) "
            + "VALUES (#{id}, #{paymentNo}, #{transactionId}, #{result})")
    void insertNotify(@Param("id") long id, @Param("paymentNo") String paymentNo,
            @Param("transactionId") String transactionId, @Param("result") String result);

    record PaymentView(long id, String paymentNo, long orderId, String channel, BigDecimal amount,
            String status, String transactionId, LocalDateTime expireAt, LocalDateTime paidAt, LocalDateTime createdAt) {}
}
