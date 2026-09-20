package com.relax.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "relax.auth.bootstrap-super-admin-open-id=mock:1c8ffc689a1d9f14725558dd33201d29")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String userToken;
    private static String adminToken; // bootstrap admin = also the technician
    private static String orderNo;
    private static String refundNo;
    private static long technicianId;
    private static long projectId;
    private static long addressId;

    // ==================== 1. 登录测试 ====================

    @Test
    @Order(1)
    void userLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"test-user-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.at("/data/accessToken").asText();
        System.out.println("✅ 用户登录成功");
    }

    @Test
    @Order(2)
    void adminLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"phase2-super\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.account.roles").isArray())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = body.at("/data/accessToken").asText();
        System.out.println("✅ 管理员登录成功，角色: " + body.at("/data/account/roles"));
    }

    // ==================== 2. 管理员基础数据 ====================

    @Test
    @Order(10)
    void adminCreateCategory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"推拿按摩\", \"sort\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
        System.out.println("✅ 管理员创建分类成功");
    }

    @Test
    @Order(11)
    void adminCreateProject() throws Exception {
        mockMvc.perform(post("/api/v1/admin/projects")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"全身推拿\", \"categoryId\": 101, \"durationMinutes\": 60, \"basePrice\": 198.00, \"description\": \"专业全身推拿\", \"sort\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
        MvcResult r = mockMvc.perform(get("/api/v1/projects")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        projectId = body.at("/data/0/id").asLong();
        System.out.println("✅ 管理员创建项目成功，projectId: " + projectId);
    }

    @Test
    @Order(12)
    void adminSetupTechnician() throws Exception {
        // 获取管理员的技师ID
        MvcResult r = mockMvc.perform(get("/api/v1/admin/technicians")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        if (body.at("/data").isArray() && body.at("/data").size() > 0) {
            technicianId = body.at("/data/0/id").asLong();
        }
        // 为技师设置项目定价
        mockMvc.perform(put("/api/v1/admin/technicians/" + technicianId + "/pricing/" + projectId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"overridePrice\": 198.00}"))
                .andExpect(status().isOk());
        // 为技师创建排班（2026-09-25 全天）
        mockMvc.perform(post("/api/v1/technician/schedules")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduleDate\": \"2026-09-25\", \"startTime\": \"09:00\", \"endTime\": \"21:00\"}"))
                .andExpect(status().isOk());
        System.out.println("✅ 技师设置完成，technicianId: " + technicianId);
    }

    // ==================== 3. 用户地址 ====================

    @Test
    @Order(30)
    void userCreateAddress() throws Exception {
        mockMvc.perform(post("/api/v1/addresses")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contactName\": \"李四\", \"contactPhone\": \"13900139001\", \"regionCode\": \"441900004\", \"detail\": \"鸿福路1号\", \"longitude\": 113.75, \"latitude\": 23.02, \"isDefault\": false}"))
                .andExpect(status().isOk());
        MvcResult r = mockMvc.perform(get("/api/v1/addresses")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        addressId = body.at("/data/0/id").asLong();
        System.out.println("✅ 用户创建地址成功，addressId: " + addressId);
    }

    // ==================== 4. 订单流程 ====================

    @Test
    @Order(40)
    void userPreviewOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders/preview")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": " + projectId + ", \"technicianId\": " + technicianId + ", \"addressId\": " + addressId + ", \"serviceDate\": \"2026-09-25\", \"startTime\": \"14:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payableAmount").isNumber());
        System.out.println("✅ 用户预览订单成功");
    }

    @Test
    @Order(41)
    void userCreateOrder() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\": " + projectId + ", \"technicianId\": " + technicianId + ", \"addressId\": " + addressId + ", \"serviceDate\": \"2026-09-25\", \"startTime\": \"14:00\", \"note\": \"请准时\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
                .andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        orderNo = body.at("/data/order/orderNo").asText();
        System.out.println("✅ 用户创建订单成功，orderNo: " + orderNo);
    }

    @Test
    @Order(42)
    void userPayOrder() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders/" + orderNo + "/payments")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        String paymentNo = body.at("/data/paymentNo").asText();
        mockMvc.perform(post("/api/v1/payments/" + paymentNo + "/simulate")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        MvcResult d = mockMvc.perform(get("/api/v1/orders/" + orderNo)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode db = objectMapper.readTree(d.getResponse().getContentAsString());
        System.out.println("✅ 用户支付成功，状态: " + db.at("/data/order/status").asText());
    }

    @Test
    @Order(43)
    void userViewOrderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderNo)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.addressSnapshot").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").isNotEmpty());
        System.out.println("✅ 用户查看订单详情成功");
    }

    @Test
    @Order(44)
    void userViewOrderList() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        System.out.println("✅ 用户查看订单列表，共 " + body.at("/data").size() + " 个");
    }

    // ==================== 5. 技师履约（管理员作为技师） ====================

    @Test
    @Order(50)
    void techViewOrderList() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/technician/orders")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        System.out.println("✅ 技师查看订单列表，共 " + body.at("/data").size() + " 个");
    }

    @Test
    @Order(51)
    void techAcceptOrder() throws Exception {
        mockMvc.perform(post("/api/v1/technician/orders/" + orderNo + "/accept")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        MvcResult r = mockMvc.perform(get("/api/v1/technician/orders/" + orderNo)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        System.out.println("✅ 技师接单成功，状态: " + body.at("/data/order/status").asText());
    }

    @Test
    @Order(52)
    void techDepartOrder() throws Exception {
        mockMvc.perform(post("/api/v1/technician/orders/" + orderNo + "/depart")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("✅ 技师出发成功");
    }

    @Test
    @Order(53)
    void techArriveOrder() throws Exception {
        mockMvc.perform(post("/api/v1/technician/orders/" + orderNo + "/arrive")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("✅ 技师到达成功");
    }

    @Test
    @Order(54)
    void techStartService() throws Exception {
        mockMvc.perform(post("/api/v1/technician/orders/" + orderNo + "/start")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("✅ 技师开始服务");
    }

    @Test
    @Order(55)
    void techCompleteService() throws Exception {
        mockMvc.perform(post("/api/v1/technician/orders/" + orderNo + "/complete")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        MvcResult r = mockMvc.perform(get("/api/v1/technician/orders/" + orderNo)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        System.out.println("✅ 技师完成服务，状态: " + body.at("/data/order/status").asText());
    }

    // ==================== 6. 评价 ====================

    @Test
    @Order(60)
    void userReviewOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + orderNo + "/reviews")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\": 5, \"content\": \"服务非常好！\"}"))
                .andExpect(status().isOk());
        System.out.println("✅ 用户评价成功");
    }

    // ==================== 7. 退款 ====================

    @Test
    @Order(70)
    void userRequestRefund() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/orders/" + orderNo + "/refunds")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50.00, \"reason\": \"部分不满意\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        refundNo = body.at("/data/refundNo").asText();
        System.out.println("✅ 用户申请退款成功，refundNo: " + refundNo);
    }

    @Test
    @Order(71)
    void adminApproveRefund() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refunds/" + refundNo + "/approve")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        System.out.println("✅ 管理员审批退款成功");
    }

    // ==================== 8. 技师统计 ====================

    @Test
    @Order(100)
    void techViewStats() throws Exception {
        mockMvc.perform(get("/api/v1/technician/orders/stats")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").isNumber())
                .andExpect(jsonPath("$.data.today").isNumber());
        System.out.println("✅ 技师查看统计数据成功");
    }

    // ==================== 9. 通知 ====================

    @Test
    @Order(110)
    void userViewNotifications() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        System.out.println("✅ 用户查看通知，共 " + body.at("/data").size() + " 条");
    }

    // ==================== 10. 首页 ====================

    @Test
    @Order(120)
    void userViewHome() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk());
        System.out.println("✅ 用户查看首页成功");
    }

    // ==================== 11. 健康检查 ====================

    @Test
    @Order(200)
    void healthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        System.out.println("✅ 健康检查通过");
    }
}
