package com.relax.system;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/mock")
@PreAuthorize("hasAuthority('admin:manage')")
public class MockDataController {

    private final MockDataService mockDataService;

    MockDataController(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    @GetMapping("/status")
    ApiResponse<MockDataService.MockStatus> status() {
        return ApiResponse.success(mockDataService.getStatus());
    }

    @PostMapping("/toggle")
    ApiResponse<MockDataService.MockStatus> toggle() {
        return ApiResponse.success(mockDataService.toggle());
    }

    @PostMapping("/reset")
    ApiResponse<Void> reset() {
        mockDataService.resetMockData();
        return ApiResponse.success(null);
    }
}
