package com.relax.file;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
class DatabaseObjectStorage implements ObjectStorage {

    private final JdbcTemplate jdbcTemplate;

    DatabaseObjectStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        jdbcTemplate.update("UPDATE file_asset SET content = ? WHERE id = ?", content, asset.id());
    }

    @Override
    public StoredObject inspect(FileAsset asset) {
        byte[] content = getContent(asset.id());
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
        byte[] content = getContent(asset.id());
        return new InputStreamResource(new ByteArrayInputStream(content));
    }

    private byte[] getContent(long id) {
        List<byte[]> results = jdbcTemplate.query(
                "SELECT content FROM file_asset WHERE id = ?",
                (rs, rowNum) -> rs.getBytes("content"),
                id
        );
        if (results.isEmpty() || results.get(0) == null) {
            throw new IllegalStateException("File content not found in database for id: " + id);
        }
        return results.get(0);
    }
}
