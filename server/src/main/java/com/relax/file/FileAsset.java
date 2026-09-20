package com.relax.file;

import java.time.LocalDateTime;

public record FileAsset(
        long id,
        long ownerUserId,
        String purpose,
        String storageProvider,
        String objectKey,
        String originalName,
        String mimeType,
        long expectedSize,
        Long actualSize,
        String status,
        String uploadTokenDigest,
        LocalDateTime uploadExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}
