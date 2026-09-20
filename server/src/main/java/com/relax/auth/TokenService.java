package com.relax.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

@Service
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthMapper authMapper;
    private final AuthProperties properties;

    TokenService(AuthMapper authMapper, AuthProperties properties) {
        this.authMapper = authMapper;
        this.properties = properties;
    }

    IssuedToken issue(long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime expiresAt = LocalDateTime.now().plus(properties.tokenTtl());
        authMapper.insertToken(IdWorker.getId(), userId, digest(rawToken), expiresAt);
        return new IssuedToken(rawToken, expiresAt);
    }

    Optional<AuthenticatedAccount> authenticate(String rawToken) {
        return authMapper.findUserByValidToken(digest(rawToken), LocalDateTime.now())
                .map(user -> new AuthenticatedAccount(
                        user,
                        new LinkedHashSet<>(authMapper.findRoleCodes(user.id())),
                        new LinkedHashSet<>(authMapper.findPermissionCodes(user.id()))));
    }

    void revoke(String rawToken) {
        authMapper.revokeToken(digest(rawToken));
    }

    public String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    record IssuedToken(String value, LocalDateTime expiresAt) {
    }

    record AuthenticatedAccount(UserAccount account, Set<String> roles, Set<String> permissions) {
    }
}
