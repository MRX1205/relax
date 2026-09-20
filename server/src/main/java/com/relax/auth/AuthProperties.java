package com.relax.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("relax.auth")
public record AuthProperties(Duration tokenTtl, String bootstrapSuperAdminOpenId) {

    public AuthProperties {
        tokenTtl = tokenTtl == null ? Duration.ofDays(7) : tokenTtl;
        bootstrapSuperAdminOpenId = bootstrapSuperAdminOpenId == null ? "" : bootstrapSuperAdminOpenId;
    }
}
