package com.relax.region;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AddressControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsDongguanAreasAndRejectsOutsideAddress() throws Exception {
        mockMvc.perform(get("/api/v1/regions/dongguan/service-areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(32));

        String token = login("address-outside-user");
        mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("440106", "113.75", "23.02", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADDRESS_OUT_OF_SERVICE"));

        mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("441900004", "120.00", "30.00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADDRESS_OUT_OF_SERVICE"));
    }

    @Test
    void enforcesOwnershipAndMaintainsOneDefaultAddress() throws Exception {
        String ownerToken = login("address-owner-user");
        String otherToken = login("address-other-user");

        String firstId = createAddress(ownerToken, "441900004", false);
        String secondId = createAddress(ownerToken, "441900003", false);

        mockMvc.perform(get("/api/v1/addresses").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));

        mockMvc.perform(put("/api/v1/addresses/{id}", secondId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson("441900003", "113.78", "23.03", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        mockMvc.perform(delete("/api/v1/addresses/{id}", secondId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ADDRESS_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/addresses/{id}", secondId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/addresses").header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    private String createAddress(String token, String regionCode, boolean isDefault) throws Exception {
        String body = mockMvc.perform(post("/api/v1/addresses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson(regionCode, "113.75", "23.02", isDefault)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String addressJson(String regionCode, String longitude, String latitude, boolean isDefault) {
        return "{\"contactName\":\"张三\",\"contactPhone\":\"13800138002\","
                + "\"regionCode\":\"" + regionCode + "\",\"detail\":\"鸿福路 1 号\","
                + "\"longitude\":" + longitude + ",\"latitude\":" + latitude + ","
                + "\"label\":\"家\",\"isDefault\":" + isDefault + "}";
    }

    private String login(String code) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).path("data");
        return data.path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
