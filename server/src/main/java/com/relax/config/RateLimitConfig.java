package com.relax.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class RateLimitConfig {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    @Bean
    Filter rateLimitFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) req;
                HttpServletResponse response = (HttpServletResponse) res;

                String key = getClientIp(request);
                RateWindow window = windows.computeIfAbsent(key, k -> new RateWindow());
                if (!window.tryAcquire()) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁\"}");
                    return;
                }
                chain.doFilter(req, res);
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    static class RateWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }
}
