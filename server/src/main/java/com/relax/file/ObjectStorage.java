package com.relax.file;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.core.io.Resource;

public interface ObjectStorage {

    String provider();

    UploadGrant createUploadGrant(FileAsset asset, String rawUploadToken, LocalDateTime expiresAt);

    StoredObject inspect(FileAsset asset);

    DownloadGrant createDownloadGrant(FileAsset asset, LocalDateTime expiresAt);

    default void storeLocal(FileAsset asset, byte[] content) {
        throw new UnsupportedOperationException("Direct upload is only available for local storage");
    }

    default Resource localResource(FileAsset asset) {
        throw new UnsupportedOperationException("Direct download is only available for local storage");
    }

    record UploadGrant(String method, String url, Map<String, String> headers, LocalDateTime expiresAt) {
    }

    record DownloadGrant(String url, LocalDateTime expiresAt) {
    }

    record StoredObject(long size, String mimeType, byte[] prefix) {
    }
}
