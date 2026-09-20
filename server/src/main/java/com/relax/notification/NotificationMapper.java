package com.relax.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notification (id, user_id, type, title, content, related_order_no) "
            + "VALUES (#{id}, #{userId}, #{type}, #{title}, #{content}, #{relatedOrderNo})")
    void insert(@Param("id") long id, @Param("userId") long userId, @Param("type") String type,
            @Param("title") String title, @Param("content") String content,
            @Param("relatedOrderNo") String relatedOrderNo);

    @Select("SELECT id, user_id AS userId, type, title, content, related_order_no AS relatedOrderNo, "
            + "read_at AS readAt, send_status AS sendStatus, created_at AS createdAt "
            + "FROM notification WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<NotificationView> findByUser(@Param("userId") long userId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE notification SET read_at = CURRENT_TIMESTAMP WHERE user_id = #{userId} AND read_at IS NULL")
    int markAllRead(@Param("userId") long userId);

    @Update("UPDATE notification SET read_at = CURRENT_TIMESTAMP WHERE id = #{id} AND read_at IS NULL")
    int markRead(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND read_at IS NULL")
    int unreadCount(@Param("userId") long userId);

    record NotificationView(long id, long userId, String type, String title, String content,
            String relatedOrderNo, LocalDateTime readAt, String sendStatus, LocalDateTime createdAt) {}
}
