package com.relax.coupon;

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
public interface CouponMapper {

    // Coupon templates
    @Select("SELECT id, name, amount, min_spend AS minSpend, total_count AS totalCount, "
            + "issued_count AS issuedCount, start_at AS startAt, end_at AS endAt, status "
            + "FROM coupon_template WHERE status = 'ACTIVE' ORDER BY id DESC")
    List<CouponTemplate> findActiveTemplates();

    @Select("SELECT id, name, amount, min_spend AS minSpend, total_count AS totalCount, "
            + "issued_count AS issuedCount, start_at AS startAt, end_at AS endAt, status "
            + "FROM coupon_template ORDER BY id DESC")
    List<CouponTemplate> findAllTemplates();

    @Select("SELECT id, name, amount, min_spend AS minSpend, total_count AS totalCount, "
            + "issued_count AS issuedCount, start_at AS startAt, end_at AS endAt, status "
            + "FROM coupon_template WHERE id = #{id}")
    Optional<CouponTemplate> findTemplateById(@Param("id") long id);

    @Insert("INSERT INTO coupon_template (id, name, amount, min_spend, total_count, start_at, end_at, status) "
            + "VALUES (#{id}, #{name}, #{amount}, #{minSpend}, #{totalCount}, #{startAt}, #{endAt}, 'ACTIVE')")
    void insertTemplate(@Param("id") long id, @Param("name") String name, @Param("amount") BigDecimal amount,
            @Param("minSpend") BigDecimal minSpend, @Param("totalCount") int totalCount,
            @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Update("UPDATE coupon_template SET status = #{status} WHERE id = #{id}")
    int updateTemplateStatus(@Param("id") long id, @Param("status") String status);

    // User coupons
    @Select("SELECT uc.id, uc.user_id AS userId, uc.template_id AS templateId, uc.status, "
            + "uc.locked_order_id AS lockedOrderId, uc.used_at AS usedAt, "
            + "ct.name, ct.amount, ct.min_spend AS minSpend, ct.end_at AS endAt "
            + "FROM user_coupon uc JOIN coupon_template ct ON ct.id = uc.template_id "
            + "WHERE uc.user_id = #{userId} AND uc.status = #{status} "
            + "ORDER BY uc.id DESC")
    List<UserCoupon> findUserCouponsByStatus(@Param("userId") long userId, @Param("status") String status);

    @Select("SELECT uc.id, uc.user_id AS userId, uc.template_id AS templateId, uc.status, "
            + "uc.locked_order_id AS lockedOrderId, uc.used_at AS usedAt, "
            + "ct.name, ct.amount, ct.min_spend AS minSpend, ct.end_at AS endAt "
            + "FROM user_coupon uc JOIN coupon_template ct ON ct.id = uc.template_id "
            + "WHERE uc.user_id = #{userId} ORDER BY uc.id DESC")
    List<UserCoupon> findAllUserCoupons(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND template_id = #{templateId} AND status = 'AVAILABLE'")
    int countUserCoupon(@Param("userId") long userId, @Param("templateId") long templateId);

    @Insert("INSERT INTO user_coupon (id, user_id, template_id, status) VALUES (#{id}, #{userId}, #{templateId}, 'AVAILABLE')")
    void insertUserCoupon(@Param("id") long id, @Param("userId") long userId, @Param("templateId") long templateId);

    @Update("UPDATE coupon_template SET issued_count = issued_count + 1 WHERE id = #{templateId} AND issued_count < total_count")
    int incrementIssuedCount(@Param("templateId") long templateId);

    @Update("UPDATE user_coupon SET status = 'LOCKED', locked_order_id = #{orderId} WHERE id = #{id} AND status = 'AVAILABLE'")
    int lockCoupon(@Param("id") long id, @Param("orderId") long orderId);

    @Update("UPDATE user_coupon SET status = 'USED', used_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'LOCKED'")
    int useCoupon(@Param("id") long id);

    @Update("UPDATE user_coupon SET status = 'AVAILABLE', locked_order_id = NULL WHERE id = #{id} AND status = 'LOCKED'")
    int releaseCoupon(@Param("id") long id);

    record CouponTemplate(long id, String name, BigDecimal amount, BigDecimal minSpend,
            int totalCount, int issuedCount, LocalDateTime startAt, LocalDateTime endAt, String status) {}

    record UserCoupon(long id, long userId, long templateId, String status,
            Long lockedOrderId, LocalDateTime usedAt,
            String name, BigDecimal amount, BigDecimal minSpend, LocalDateTime endAt) {}
}
