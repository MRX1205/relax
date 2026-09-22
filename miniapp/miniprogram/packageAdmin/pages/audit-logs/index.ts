import { request } from "../../../services/http";

interface AuditEntry {
  id: string | number;
  operatorId: string | number;
  action: string;
  targetType: string;
  targetId: string;
  detail: string;
  ipAddress: string;
  createdAt: string;
}

Page({
  data: {
    loading: true,
    logs: [] as AuditEntry[],
  },

  onLoad() {
    this.loadLogs();
  },

  async loadLogs() {
    this.setData({ loading: true });
    try {
      const logs = await request<AuditEntry[]>({ url: "/api/v1/admin/audit-logs" });
      this.setData({ logs: logs || [], loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  async onPullDownRefresh() {
    await this.loadLogs();
    wx.stopPullDownRefresh();
  },
});
