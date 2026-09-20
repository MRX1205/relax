package com.relax.auth;

import java.util.Set;

public record CurrentUser(long id, String openId, Set<String> roles, Set<String> permissions) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
