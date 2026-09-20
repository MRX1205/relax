package com.relax.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowTests {

    @Autowired
    MockMvc mockMvc;

    private String loginAndGetToken(String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int idx = body.indexOf("\"accessToken\":\"");
        if (idx < 0) return "";
        int start = idx + "\"accessToken\":\"".length();
        return body.substring(start, body.indexOf("\"", start));
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void loginCreatesAccountAndReturnsToken() throws Exception {
        String token = loginAndGetToken("test-user-flow-1");
        assert !token.isEmpty();
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void categoriesEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void projectsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void techniciansEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/api/v1/technicians"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void homeEndpointReturnsAggregatedData() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.featuredProjects").isArray());
    }

    @Test
    void agreementsArePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/agreements/USER_AGREEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("USER_AGREEMENT"));
    }

    @Test
    void serviceAreasArePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/regions/dongguan/service-areas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void adminEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void orderCreationRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bannersEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/api/v1/banners"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
    }
}
