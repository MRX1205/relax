package com.relax.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiSecurityErrorWriter errorWriter;
    private final TokenService tokenService;

    AccessTokenFilter(ApiSecurityErrorWriter errorWriter, TokenService tokenService) {
        this.errorWriter = errorWriter;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "TOKEN_INVALID", "登录状态无效，请重新登录");
            return;
        }

        TokenService.AuthenticatedAccount authenticated = tokenService
                .authenticate(authorization.substring(BEARER_PREFIX.length()))
                .orElse(null);
        if (authenticated == null) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "TOKEN_INVALID", "登录状态已失效，请重新登录");
            return;
        }
        if (!"ACTIVE".equals(authenticated.account().status())) {
            errorWriter.write(response, HttpStatus.FORBIDDEN.value(), "ACCOUNT_DISABLED", "账号已被停用");
            return;
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authenticated.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        authenticated.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        CurrentUser principal = new CurrentUser(
                authenticated.account().id(),
                authenticated.account().wechatOpenId(),
                authenticated.roles(),
                authenticated.permissions());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
        filterChain.doFilter(request, response);
    }
}
