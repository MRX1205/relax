package com.relax.file;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Primary
class DatabaseObjectStorage implements ObjectStorage {

    private final FileContentMapper fileContentMapper;

    DatabaseObjectStorage(FileContentMapper fileContentMapper) {
        this.fileContentMapper = fileContentMapper;
    }

    @Override
    public String provider() {
        return "DATABASE";
    }

    @Override
    public UploadGrant createUploadGrant(FileAsset asset, String rawUploadToken, LocalDateTime expiresAt) {
        return new UploadGrant("PUT", "/api/v1/files/" + asset.id() + "/content",
                Map.of("Content-Type", asset.mimeType(), "X-Upload-Token", rawUploadToken), expiresAt);
    }

    @Override
    public void storeLocal(FileAsset asset, byte[] content) {
        fileContentMapper.saveContent(asset.id(), content);
    }

    @Override
    public StoredObject inspect(FileAsset asset) {
        byte[] content = fileContentMapper.findContent(asset.id());
        if (content == null || content.length == 0) {
            throw new IllegalStateException("File content not found in database");
        }
        byte[] prefix = new byte[Math.min(8, content.length)];
        System.arraycopy(content, 0, prefix, 0, prefix.length);
        return new StoredObject(content.length, asset.mimeType(), prefix);
    }

    @Override
    public DownloadGrant createDownloadGrant(FileAsset asset, LocalDateTime expiresAt) {
        return new DownloadGrant("/api/v1/files/" + asset.id() + "/content", expiresAt);
    }

    @Override
    public Resource localResource(FileAsset asset) {
        byte[] content = fileContentMapper.findContent(asset.id());
        if (content == null) {
            throw new IllegalStateException("File content not found");
        }
        return new InputStreamResource(new ByteArrayInputStream(content));
    }
}
