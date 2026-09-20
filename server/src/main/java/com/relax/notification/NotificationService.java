package com.relax.notification;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

@Service
public class NotificationService {

    private final NotificationMapper mapper;

    NotificationService(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    public void send(long userId, String type, String title, String content, String orderNo) {
        long id = IdWorker.getId();
        mapper.insert(id, userId, type, title, content, orderNo);
    }

    public List<NotificationMapper.NotificationView> list(long userId, int page, int size) {
        return mapper.findByUser(userId, size, page * size);
    }

    public int markAllRead(long userId) {
        return mapper.markAllRead(userId);
    }

    public int markRead(long notificationId) {
        return mapper.markRead(notificationId);
    }

    public int unreadCount(long userId) {
        return mapper.unreadCount(userId);
    }
}
