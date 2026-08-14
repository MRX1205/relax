import { environment } from "../../config/environment";
import { request } from "../../services/http";

type ServiceState = "idle" | "checking" | "online" | "offline";

Page({
  data: {
    environmentVersion: environment.version,
    serviceState: "idle" as ServiceState,
    serviceMessage: "尚未检查后端连接",
    checking: false,
  },

  async checkService() {
    this.setData({
      checking: true,
      serviceState: "checking" as ServiceState,
      serviceMessage: "正在连接后端服务",
    });

    try {
      const health = await request<HealthStatus>({ url: "/api/v1/health" });
      this.setData({
        serviceState: "online" as ServiceState,
        serviceMessage: `${health.service} 已连接`,
      });
    } catch (error) {
      this.setData({
        serviceState: "offline" as ServiceState,
        serviceMessage: error instanceof Error ? error.message : "后端连接失败",
      });
    } finally {
      this.setData({ checking: false });
    }
  },
});

