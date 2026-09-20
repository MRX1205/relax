import { getCachedAccount } from "../../services/auth";
import { request } from "../../services/http";

interface TechnicianProfile {
  id: string;
  userId: string;
  serviceName: string;
  realName: string;
  phone: string;
  status: string;
  onlineStatus: string;
}

interface TechOrder {
  id: string;
  orderNo: string;
  status: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  projectName: string;
  userName: string;
}

Page({
  data: {
    account: null as Account | null,
    profile: null as TechnicianProfile | null,
    loading: true,
    hasApplication: false,
    applicationStatus: "",
    pendingOrders: [] as TechOrder[],
    todayOrders: [] as TechOrder[],
    stats: {
      todayOrders: 0,
      todayIncome: 0,
      pendingCount: 0,
    },
  },

  async onShow() {
    const account = getCachedAccount();
    this.setData({ account });
    await this.loadProfile();
  },

  async loadProfile() {
    try {
      const profile = await request<TechnicianProfile>({ url: "/api/v1/technician/profile" });
      this.setData({ profile, loading: false });
      this.loadOrders();
    } catch {
      try {
        const app = await request<{ status: string } | null>({ url: "/api/v1/technician/application" });
        if (app) {
          this.setData({ hasApplication: true, applicationStatus: app.status, loading: false });
        } else {
          this.setData({ loading: false });
        }
      } catch {
        this.setData({ loading: false });
      }
    }
  },

  async loadOrders() {
    try {
      const orders = await request<TechOrder[]>({ url: "/api/v1/technician/orders/today" });
      const today = new Date().toISOString().split("T")[0];
      const todayOrders = orders.filter(o => o.serviceDate === today);
      const pendingOrders = orders.filter(o => o.status === "PAID" || o.status === "CONFIRMED");
      this.setData({
        todayOrders,
        pendingOrders,
        "stats.todayOrders": todayOrders.length,
        "stats.pendingCount": pendingOrders.length,
      });
    } catch {}
  },

  async toggleOnline() {
    if (!this.data.profile) return;
    const newStatus = this.data.profile.onlineStatus === "ONLINE" ? "OFFLINE" : "ONLINE";
    try {
      await request({
        url: "/api/v1/technician/online-status",
        method: "PUT",
        data: { onlineStatus: newStatus },
      });
      this.setData({ "profile.onlineStatus": newStatus });
      wx.showToast({ title: newStatus === "ONLINE" ? "已上线" : "已下线", icon: "success" });
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  goToOrders() {
    wx.navigateTo({ url: "/pages/orders/index" });
  },

  goToSchedules() {
    wx.navigateTo({ url: "/pages/tech-schedules/index" });
  },

  goToIncome() {
    wx.navigateTo({ url: "/pages/tech-income/index" });
  },

  goToAccess() {
    wx.navigateTo({ url: "/packageTech/pages/access/index" });
  },

  goToOrderDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageTech/pages/order-detail/index?id=${id}` });
  },
});
