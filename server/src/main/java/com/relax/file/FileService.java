package com.relax.file;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.auth.CurrentUser;
import com.relax.auth.TokenService;
import com.relax.audit.AuditService;
import com.relax.common.api.BusinessException;

@Service
public class FileService {

    private static final Map<String, FileRule> RULES = Map.of(
            "AVATAR", new FileRule(5 * 1024 * 1024L, Set.of("image/jpeg", "image/png")),
            "TECHNICIAN_PHOTO", new FileRule(10 * 1024 * 1024L, Set.of("image/jpeg", "image/png")),
            "TECHNICIAN_CERTIFICATE", new FileRule(10 * 1024 * 1024L, Set.of("image/jpeg", "image/png", "application/pdf")),
            "AFTER_SALE_EVIDENCE", new FileRule(10 * 1024 * 1024L, Set.of("image/jpeg", "image/png")),
            "SETTLEMENT_PROOF", new FileRule(10 * 1024 * 1024L, Set.of("image/jpeg", "image/png", "application/pdf")));

    private final SecureRandom secureRandom = new SecureRandom();
    private final FileMapper fileMapper;
    private final FileStorageProperties properties;
    private final ObjectStorage storage;
    private final TokenService tokenService;
    private final AuditService auditService;

    FileService(FileMapper fileMapper, FileStorageProperties properties, ObjectStorage storage,
            TokenService tokenService, AuditService auditService) {
        this.fileMapper = fileMapper;
        this.properties = properties;
        this.storage = storage;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    @Transactional
    public UploadPolicy createUploadPolicy(long userId, UploadPolicyRequest request) {
        FileRule rule = requireRule(request.purpose(), request.mimeType(), request.size());
        long id = IdWorker.getId();
        String uploadToken = randomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(properties.grantTtl());
        String objectKey = request.purpose().toLowerCase() + "/" + userId + "/" + id + extension(request.mimeType());
        FileAsset asset = new FileAsset(id, userId, request.purpose(), storage.provider(), objectKey,
                request.fileName().strip(), request.mimeType(), request.size(), null, "PENDING",
                tokenService.digest(uploadToken), expiresAt, null, null);
        fileMapper.insert(asset);
        return new UploadPolicy(id, request.purpose(), request.mimeType(), request.size(),
                storage.createUploadGrant(asset, uploadToken, expiresAt));
    }

    @Transactional
    public void uploadLocal(long id, String uploadToken, String contentType, byte[] content) {
        FileAsset asset = requireAsset(id);
        if ((!"LOCAL".equals(asset.storageProvider()) && !"DATABASE".equals(asset.storageProvider())) || !"PENDING".equals(asset.status())
                || asset.uploadExpiresAt() == null || asset.uploadExpiresAt().isBefore(LocalDateTime.now())
                || !tokenService.digest(uploadToken).equals(asset.uploadTokenDigest())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UPLOAD_GRANT_INVALID", "上传凭证无效或已过期");
        }
        if (!asset.mimeType().equals(normalizeContentType(contentType)) || content.length != asset.expectedSize()) {
            throw new BusinessException("FILE_CONTENT_MISMATCH", "上传文件类型或大小与申请不一致");
        }
        validateMagic(asset.mimeType(), prefix(content));
        storage.storeLocal(asset, content);
        fileMapper.markUploaded(id, content.length);
    }

    @Transactional
    public FileView complete(long userId, long id) {
        FileAsset asset = requireOwned(userId, id);
        if ("READY".equals(asset.status())) {
            return toView(asset);
        }
        if (!Set.of("PENDING", "UPLOADED").contains(asset.status())) {
            throw new BusinessException("FILE_STATUS_INVALID", "文件当前状态不能完成上传");
        }
        ObjectStorage.StoredObject stored;
        try {
            stored = storage.inspect(asset);
        } catch (RuntimeException exception) {
            throw new BusinessException("FILE_NOT_UPLOADED", "尚未检测到已上传文件");
        }
        try {
            if (stored.size() != asset.expectedSize()
                    || !asset.mimeType().equals(normalizeContentType(stored.mimeType()))) {
                throw new BusinessException("FILE_CONTENT_MISMATCH", "上传文件类型或大小与申请不一致");
            }
            validateMagic(asset.mimeType(), stored.prefix());
        } catch (BusinessException exception) {
            fileMapper.markRejected(id);
            throw exception;
        }
        fileMapper.markReady(id, stored.size());
        return toView(requireAsset(id));
    }

    @Transactional
    public DirectUploadView uploadDirect(Long userId, String originalFilename, String contentType, byte[] bytes, String purpose) {
        long id = IdWorker.getId();
        String safeName = (originalFilename != null && !originalFilename.isBlank()) ? originalFilename : "image.jpg";
        String safeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String safePurpose = (purpose != null && !purpose.isBlank()) ? purpose.toUpperCase() : "IMAGE";
        String objectKey = safePurpose.toLowerCase() + "/" + (userId != null ? userId : 0L) + "/" + id + "-" + safeName;

        FileAsset asset = new FileAsset(id, userId != null ? userId : 0L, safePurpose, "DATABASE", objectKey,
                safeName, safeType, (long) bytes.length, (long) bytes.length, "READY",
                null, null, LocalDateTime.now(), LocalDateTime.now());
        fileMapper.insert(asset);
        storage.storeLocal(asset, bytes);
        return new DirectUploadView(id, "/api/v1/public/files/" + id, safeName);
    }

    public record DirectUploadView(long id, String url, String fileName) {}

    public List<FileView> files(long userId) {
        return fileMapper.findByOwner(userId).stream().map(this::toView).toList();
    }

    public ObjectStorage.DownloadGrant createDownloadGrant(CurrentUser currentUser, long id, String ipAddress) {
        FileAsset asset = requireReadable(currentUser, id);
        auditService.record(currentUser.id(), "PRIVATE_FILE_ACCESSED", "FILE", Long.toString(id),
                "purpose=" + asset.purpose(), ipAddress);
        return storage.createDownloadGrant(asset, LocalDateTime.now().plus(properties.grantTtl()));
    }

    public LocalFile localFile(CurrentUser currentUser, long id) {
        FileAsset asset = requireReadable(currentUser, id);
        if ((!"LOCAL".equals(asset.storageProvider()) && !"DATABASE".equals(asset.storageProvider()))) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "文件不存在");
        }
        return new LocalFile(storage.localResource(asset), asset.mimeType(), asset.originalName());
    }

    private FileAsset requireReadable(CurrentUser currentUser, long id) {
        FileAsset asset = requireAsset(id);
        if (!"READY".equals(asset.status())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "文件不存在");
        }
        if (asset.ownerUserId() != currentUser.id() && !currentUser.permissions().contains("file:private:read")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FILE_ACCESS_DENIED", "无权访问该文件");
        }
        return asset;
    }

    private FileAsset requireOwned(long userId, long id) {
        FileAsset asset = requireAsset(id);
        if (asset.ownerUserId() != userId) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FILE_ACCESS_DENIED", "无权操作该文件");
        }
        return asset;
    }

    private FileAsset requireAsset(long id) {
        return fileMapper.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "文件不存在"));
    }

    private FileRule requireRule(String purpose, String mimeType, long size) {
        FileRule rule = RULES.get(purpose);
        if (rule == null) {
            throw new BusinessException("FILE_PURPOSE_INVALID", "文件用途无效");
        }
        if (!rule.mimeTypes().contains(mimeType)) {
            throw new BusinessException("FILE_TYPE_INVALID", "该用途不支持此文件类型");
        }
        if (size <= 0 || size > rule.maxSize()) {
            throw new BusinessException("FILE_SIZE_INVALID", "文件大小超出限制");
        }
        return rule;
    }

    private void validateMagic(String mimeType, byte[] prefix) {
        boolean valid = switch (mimeType) {
            case "image/jpeg" -> prefix.length >= 3 && (prefix[0] & 0xFF) == 0xFF
                    && (prefix[1] & 0xFF) == 0xD8 && (prefix[2] & 0xFF) == 0xFF;
            case "image/png" -> prefix.length >= 8 && (prefix[0] & 0xFF) == 0x89
                    && new String(prefix, 1, 3, StandardCharsets.US_ASCII).equals("PNG");
            case "application/pdf" -> prefix.length >= 5
                    && new String(prefix, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("FILE_CONTENT_INVALID", "文件内容与声明类型不一致");
        }
    }

    private byte[] prefix(byte[] content) {
        return java.util.Arrays.copyOf(content, Math.min(content.length, 8));
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.split(";", 2)[0].strip().toLowerCase();
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    private FileView toView(FileAsset asset) {
        return new FileView(asset.id(), asset.purpose(), asset.originalName(), asset.mimeType(),
                asset.actualSize(), asset.status(), asset.createdAt(), asset.completedAt());
    }

    private record FileRule(long maxSize, Set<String> mimeTypes) {
    }

    public record UploadPolicyRequest(String purpose, String fileName, String mimeType, long size) {
    }

    public record UploadPolicy(long fileId, String purpose, String mimeType, long size,
            ObjectStorage.UploadGrant upload) {
    }

    public record FileView(long id, String purpose, String fileName, String mimeType, Long size,
            String status, LocalDateTime createdAt, LocalDateTime completedAt) {
    }

    public record LocalFile(Resource resource, String mimeType, String fileName) {
    }
}
