package com.relax.auth;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest(properties = "relax.auth.bootstrap-super-admin-open-id=mock:1c8ffc689a1d9f14725558dd33201d29")
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TokenService tokenService;

    @Test
    void createsAccountBindsPhoneAndUpdatesProfile() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        Login login = login("auth-profile-user");
        mockMvc.perform(post("/api/v1/auth/bind-phone")
                        .header("Authorization", bearer(login.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"mock-phone-13800138001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13800138001"));

        mockMvc.perform(put("/api/v1/me/profile")
                        .header("Authorization", bearer(login.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"东莞用户\",\"avatarUrl\":\"https://example.test/avatar.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("东莞用户"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    void rejectsInvalidExpiredAndDisabledTokens() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        Login expired = login("auth-expired-user");
        jdbcTemplate.update("UPDATE auth_access_token SET expires_at = DATEADD('MINUTE', -1, CURRENT_TIMESTAMP) "
                + "WHERE token_digest = ?", tokenService.digest(expired.token()));
        mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(expired.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        Login disabled = login("auth-disabled-user");
        jdbcTemplate.update("UPDATE platform_user SET status = 'DISABLED' WHERE id = ?", disabled.userId());
        mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(disabled.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        jdbcTemplate.update("UPDATE platform_user SET status = 'ACTIVE' WHERE id = ?", disabled.userId());
    }

    @Test
    void rejectsForgedRoleAndAdminRequest() throws Exception {
        Login user = login("auth-forged-role-user");
        mockMvc.perform(put("/api/v1/me/last-role")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_GRANTED"));

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", bearer(user.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void appliesDifferentAdminPermissionGroupsOnBackend() throws Exception {
        Login superAdmin = login("phase2-super");
        Login target = login("auth-permission-target");

        updateAdminAccess(superAdmin.token(), target.userId(), "SUPPLY");
        mockMvc.perform(put("/api/v1/admin/service-areas/441900003")
                        .header("Authorization", bearer(target.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        updateAdminAccess(superAdmin.token(), target.userId(), "OPERATIONS");
        mockMvc.perform(put("/api/v1/admin/service-areas/441900003")
                        .header("Authorization", bearer(target.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", bearer(target.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").exists());

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", bearer(target.token())))
                .andExpect(status().isForbidden());
    }

    @Test
    void phoneLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/phone-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800003333\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.account.phone").value("13800003333"))
                .andExpect(jsonPath("$.data.account.roles").isArray());
    }

    @Test
    void passwordLoginSuccessForTechnician() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800003333\",\"password\":\"123456\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.account.phone").value("13800003333"))
                .andExpect(jsonPath("$.data.account.lastRole").value("TECHNICIAN"));
    }

    @Test
    void passwordLoginFailsOnRoleMismatchOrWrongPassword() throws Exception {
        // Wrong password
        mockMvc.perform(post("/api/v1/auth/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800003333\",\"password\":\"wrong-pwd\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PASSWORD_INCORRECT"));

        // User 13800001111 is only a customer, not technician
        mockMvc.perform(post("/api/v1/auth/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800001111\",\"password\":\"123456\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_GRANTED"));
    }

    @Test
    void roleWechatLoginRejectsUnboundAndAllowsBound() throws Exception {
        // Unbound wechat code
        mockMvc.perform(post("/api/v1/auth/role-wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"unbound-wx-tech-code\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WECHAT_NOT_BOUND"));

        // First login technician with password
        String body = mockMvc.perform(post("/api/v1/auth/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800004444\",\"password\":\"123456\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String techToken = objectMapper.readTree(body).path("data").path("accessToken").asText();

        // Bind wechat
        mockMvc.perform(post("/api/v1/auth/bind-wechat")
                        .header("Authorization", bearer(techToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"my-test-tech-wx-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wechatBound").value(true));

        // Now role wechat login should succeed!
        mockMvc.perform(post("/api/v1/auth/role-wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"my-test-tech-wx-code\",\"targetRole\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.account.phone").value("13800004444"));
    }

    private void updateAdminAccess(String token, long userId, String group) throws Exception {
        mockMvc.perform(put("/api/v1/admin/access/users/{userId}", userId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"groupCodes\":[\"" + group + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionGroups[0]").value(group));
    }

    private Login login(String code) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account.roles[0]").value("USER"))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).path("data");
        return new Login(data.path("accessToken").asText(), data.path("account").path("id").asLong());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Login(String token, long userId) {
    }
}
