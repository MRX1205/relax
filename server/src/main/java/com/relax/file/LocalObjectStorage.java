package com.relax.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
class LocalObjectStorage implements ObjectStorage {

    private final Path root;

    LocalObjectStorage(FileStorageProperties properties) {
        this.root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
    }

    @Override
    public String provider() {
        return "LOCAL";
    }

    @Override
    public UploadGrant createUploadGrant(FileAsset asset, String rawUploadToken, LocalDateTime expiresAt) {
        return new UploadGrant("PUT", "/api/v1/files/" + asset.id() + "/content",
                Map.of("Content-Type", asset.mimeType(), "X-Upload-Token", rawUploadToken), expiresAt);
    }

    @Override
    public void storeLocal(FileAsset asset, byte[] content) {
        Path path = pathFor(asset);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store local file", exception);
        }
    }

    @Override
    public StoredObject inspect(FileAsset asset) {
        Path path = pathFor(asset);
        try {
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Uploaded file does not exist");
            }
            byte[] prefix;
            try (var input = Files.newInputStream(path)) {
                prefix = input.readNBytes(8);
            }
            return new StoredObject(Files.size(path), asset.mimeType(), prefix);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect local file", exception);
        }
    }

    @Override
    public DownloadGrant createDownloadGrant(FileAsset asset, LocalDateTime expiresAt) {
        return new DownloadGrant("/api/v1/files/" + asset.id() + "/content", expiresAt);
    }

    @Override
    public Resource localResource(FileAsset asset) {
        return new FileSystemResource(pathFor(asset));
    }

    private Path pathFor(FileAsset asset) {
        Path path = root.resolve(asset.objectKey()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("Invalid object key");
        }
        return path;
    }
}
