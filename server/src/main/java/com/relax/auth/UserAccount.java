package com.relax.auth;

import java.time.LocalDateTime;

public record UserAccount(
        long id,
        String wechatOpenId,
        String unionId,
        String nickname,
        String avatarUrl,
        String phone,
        String status,
        String lastRole,
        String passwordHash,
        LocalDateTime createdAt) {
}
