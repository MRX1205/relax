package com.relax.file;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("relax.file")
public record FileStorageProperties(String localRoot, Duration grantTtl, Cos cos) {

    public FileStorageProperties {
        localRoot = localRoot == null || localRoot.isBlank() ? "./data/private-files" : localRoot;
        grantTtl = grantTtl == null ? Duration.ofMinutes(10) : grantTtl;
        cos = cos == null ? new Cos("", "", "", "") : cos;
    }

    public record Cos(String region, String bucket, String secretId, String secretKey) {
        public Cos {
            region = region == null ? "" : region;
            bucket = bucket == null ? "" : bucket;
            secretId = secretId == null ? "" : secretId;
            secretKey = secretKey == null ? "" : secretKey;
        }
    }
}
