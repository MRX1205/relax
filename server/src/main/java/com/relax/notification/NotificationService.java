package com.relax.notification;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import com.relax.iam.IamMapper;

@Service
public class NotificationService {

    private final NotificationMapper mapper;
    private final IamMapper iamMapper;

    NotificationService(NotificationMapper mapper, IamMapper iamMapper) {
        this.mapper = mapper;
        this.iamMapper = iamMapper;
    }

    public void send(long userId, String type, String title, String content, String orderNo) {
        send(userId, type, title, content, orderNo, null);
    }

    public void send(long userId, String type, String title, String content, String orderNo, String imageUrl) {
        long id = IdWorker.getId();
        mapper.insert(id, userId, type, title, content, orderNo, imageUrl);
    }

    public int broadcast(String type, String title, String content, Long targetUserId) {
        return broadcast(type, title, content, targetUserId, null);
    }

    public int broadcast(String type, String title, String content, Long targetUserId, String imageUrl) {
        String msgType = (type != null && !type.isBlank()) ? type.trim() : "ANNOUNCEMENT";
        if (targetUserId != null && targetUserId > 0) {
            send(targetUserId, msgType, title, content, null, imageUrl);
            return 1;
        }
        List<Long> userIds = iamMapper.findAllActiveUserIds();
        for (Long uid : userIds) {
            send(uid, msgType, title, content, null, imageUrl);
        }
        return userIds.size();
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
