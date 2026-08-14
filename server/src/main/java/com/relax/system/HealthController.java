package com.relax.system;

import com.relax.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    ApiResponse<HealthStatus> health() {
        return ApiResponse.success(new HealthStatus("relax-server", "UP", Instant.now()));
    }

    public record HealthStatus(String service, String status, Instant timestamp) {
    }
}

