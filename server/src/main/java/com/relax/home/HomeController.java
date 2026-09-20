package com.relax.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    private final HomeService homeService;

    HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/home")
    ApiResponse<HomeService.HomeView> home() {
        return ApiResponse.success(homeService.home());
    }
}
