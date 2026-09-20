package com.relax.home;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private final HomeMapper homeMapper;

    HomeService(HomeMapper homeMapper) {
        this.homeMapper = homeMapper;
    }

    public HomeView home() {
        List<HomeMapper.CategoryBrief> categories = homeMapper.findTopCategories(10);
        List<HomeMapper.ProjectBrief> projects = homeMapper.findFeaturedProjects(6);
        List<HomeMapper.TechnicianBrief> technicians = homeMapper.findFeaturedTechnicians(6);
        return new HomeView(categories, projects, technicians);
    }

    public record HomeView(
            List<HomeMapper.CategoryBrief> categories,
            List<HomeMapper.ProjectBrief> featuredProjects,
            List<HomeMapper.TechnicianBrief> nearbyTechnicians) {}
}
