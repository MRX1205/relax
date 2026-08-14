package com.relax.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.matchesPattern;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPublicHealthEnvelopeAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "health-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "health-test"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.service").value("relax-server"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.requestId").value("health-test"));
    }

    @Test
    void replacesInvalidRequestIdWithGeneratedId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "!!!"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "X-Request-Id",
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                ));
    }
}
