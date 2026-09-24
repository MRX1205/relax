import { request } from "../../../services/http";

interface MockStatus {
  enabled: boolean;
  mockOrdersCount: number;
  realOrdersCount: number;
  lastReset: string;
  adminPhone: string;
}

Page({
  data: {
    loading: true,
    actionLoading: false,
    status: null as MockStatus | null,
  },

  async onLoad() {
    await this.loadStatus();
  },

  async onPullDownRefresh() {
    await this.loadStatus();
    wx.stopPullDownRefresh();
  },

  async loadStatus() {
    this.setData({ loading: true });
    try {
      const status = await request<MockStatus>({ url: "/api/v1/admin/mock/status" });
      this.setData({ status, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  async toggleMock() {
    this.setData({ actionLoading: true });
    try {
      const status = await request<MockStatus>({
        url: "/api/v1/admin/mock/toggle",
        method: "POST",
      });
      this.setData({ status, actionLoading: false });
      wx.showToast({
        title: status.enabled ? "已开启Mock演示数据" : "已切换为纯净真实数据",
        icon: "success",
      });
    } catch (err: any) {
      this.setData({ actionLoading: false });
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  async cleanMockData() {
    wx.showModal({
      title: "清理演示数据",
      content: "确认清空所有模拟测试订单与评价？清空后全系统仅保留真实客户数据，管理员账号将被安全保留。",
      confirmColor: "#FF3B30",
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: "正在清理..." });
            await request({ url: "/api/v1/admin/mock/clean", method: "POST" });
            wx.hideLoading();
            wx.showToast({ title: "演示数据已清空", icon: "success" });
            await this.loadStatus();
          } catch (err: any) {
            wx.hideLoading();
            wx.showToast({ title: err?.message || "清理失败", icon: "none" });
          }
        }
      },
    });
  },

  async seedMockData() {
    wx.showModal({
      title: "生成演示数据",
      content: "将为系统重新注入 7 条覆盖各服务状态的演示订单与真实感评价，便于汇报与测试演示。",
      confirmColor: "#007AFF",
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: "正在生成..." });
            await request({ url: "/api/v1/admin/mock/seed", method: "POST" });
            wx.hideLoading();
            wx.showToast({ title: "演示数据注入成功", icon: "success" });
            await this.loadStatus();
          } catch (err: any) {
            wx.hideLoading();
            wx.showToast({ title: err?.message || "生成失败", icon: "none" });
          }
        }
      },
    });
  },
});

