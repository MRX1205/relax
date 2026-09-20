package com.relax.file;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import jakarta.annotation.PreDestroy;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"staging", "production"})
class CosObjectStorage implements ObjectStorage {

    private final String bucket;
    private final COSClient cosClient;

    CosObjectStorage(FileStorageProperties properties) {
        FileStorageProperties.Cos cos = properties.cos();
        if (cos.region().isBlank() || cos.bucket().isBlank() || cos.secretId().isBlank() || cos.secretKey().isBlank()) {
            throw new IllegalStateException("COS_REGION, COS_BUCKET, COS_SECRET_ID and COS_SECRET_KEY must be configured");
        }
        this.bucket = cos.bucket();
        this.cosClient = new COSClient(new BasicCOSCredentials(cos.secretId(), cos.secretKey()),
                new ClientConfig(new Region(cos.region())));
    }

    @Override
    public String provider() {
        return "COS";
    }

    @Override
    public UploadGrant createUploadGrant(FileAsset asset, String rawUploadToken, LocalDateTime expiresAt) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, asset.objectKey(), HttpMethodName.PUT);
        request.setExpiration(toDate(expiresAt));
        request.setContentType(asset.mimeType());
        return new UploadGrant("PUT", cosClient.generatePresignedUrl(request).toString(),
                Map.of("Content-Type", asset.mimeType()), expiresAt);
    }

    @Override
    public StoredObject inspect(FileAsset asset) {
        ObjectMetadata metadata = cosClient.getObjectMetadata(bucket, asset.objectKey());
        GetObjectRequest request = new GetObjectRequest(bucket, asset.objectKey());
        request.setRange(0, 7);
        try (var object = cosClient.getObject(request); var input = object.getObjectContent()) {
            return new StoredObject(metadata.getContentLength(), metadata.getContentType(), input.readNBytes(8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect COS object", exception);
        }
    }

    @Override
    public DownloadGrant createDownloadGrant(FileAsset asset, LocalDateTime expiresAt) {
        return new DownloadGrant(cosClient.generatePresignedUrl(bucket, asset.objectKey(), toDate(expiresAt), HttpMethodName.GET)
                .toString(), expiresAt);
    }

    @PreDestroy
    void shutdown() {
        cosClient.shutdown();
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }
}
