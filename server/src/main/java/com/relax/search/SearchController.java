package com.relax.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.relax.common.api.ApiResponse;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final SearchMapper searchMapper;

    public SearchController(SearchMapper searchMapper) {
        this.searchMapper = searchMapper;
    }

    @GetMapping("/search")
    public ApiResponse<SearchResultView> search(@RequestParam(value = "q", required = false, defaultValue = "") String q) {
        String keyword = q.trim();
        if (keyword.isEmpty()) {
            return ApiResponse.success(new SearchResultView(Collections.emptyList(), Collections.emptyList()));
        }
        List<SearchMapper.ProjectResult> projects = searchMapper.searchProjects(keyword);
        List<SearchMapper.TechnicianResult> technicians = searchMapper.searchTechnicians(keyword);
        return ApiResponse.success(new SearchResultView(projects, technicians));
    }

    public record SearchResultView(
            List<SearchMapper.ProjectResult> projects,
            List<SearchMapper.TechnicianResult> technicians) {}
}
