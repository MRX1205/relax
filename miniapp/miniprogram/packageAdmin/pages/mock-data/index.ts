import { request } from "../../../services/http";

interface MockStatus {
  enabled: boolean;
  lastReset: string | null;
}

Page({
  data: {
    loading: true,
    status: null as MockStatus | null,
  },

  async onLoad() {
    await this.loadStatus();
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
    try {
      const status = await request<MockStatus>({ 
        url: "/api/v1/admin/mock/toggle", 
        method: "POST" 
      });
      this.setData({ status });
      wx.showToast({ 
        title: status.enabled ? "Mock数据已开启" : "Mock数据已关闭", 
        icon: "success" 
      });
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  async resetMockData() {
    wx.showModal({
      title: "确认重置",
      content: "重置将重新生成所有Mock数据，确定继续吗？",
      success: async (res) => {
        if (res.confirm) {
          try {
            await request({ url: "/api/v1/admin/mock/reset", method: "POST" });
            wx.showToast({ title: "重置成功", icon: "success" });
            await this.loadStatus();
          } catch {
            wx.showToast({ title: "重置失败", icon: "none" });
          }
        }
      }
    });
  },
});
