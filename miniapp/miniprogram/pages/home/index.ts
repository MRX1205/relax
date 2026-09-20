import { getHomeData } from "../../services/catalog";
import { getCachedAccount, loadCurrentAccount } from "../../services/auth";
import { request } from "../../services/http";
import { getAccessToken } from "../../services/http";

Page({
  data: {
    role: "USER" as string,
    loading: true,
    home: null as any,
    account: null as any,
    techProfile: null as any,
    techHasApplication: false,
    techApplicationStatus: "",
    techStats: { pending: 0, today: 0 },
    adminStats: { todayOrders: 0, totalOrders: 0, totalUsers: 0, totalTechnicians: 0 },
    adminMenus: [
      { icon: "📂", name: "分类管理", url: "/packageAdmin/pages/categories/index" },
      { icon: "💆", name: "项目管理", url: "/packageAdmin/pages/projects/index" },
      { icon: "👥", name: "技师管理", url: "/packageAdmin/pages/technicians/index" },
      { icon: "📋", name: "订单管理", url: "/packageAdmin/pages/order-list/index" },
      { icon: "↩️", name: "退款审核", url: "/packageAdmin/pages/refunds/index" },
      { icon: "💰", name: "结算管理", url: "/packageAdmin/pages/settlements/index" },
      { icon: "🖼️", name: "轮播图", url: "/packageAdmin/pages/banners/index" },
    ],
  },

  async onShow() {
    const hasToken = !!getAccessToken();
    
    if (hasToken) {
      try {
        const account = await loadCurrentAccount();
        if (account) {
          const role = account.lastRole || "USER";
          this.setData({ account, role });
          await this.loadDataByRole(role);
          return;
        }
      } catch {
        // 获取失败，使用缓存
      }
    }
    
    const account = getCachedAccount();
    const role = account?.lastRole || "USER";
    this.setData({ account: hasToken ? account : null, role });
    await this.loadDataByRole(role);
  },

  async loadDataByRole(role: string) {
    if (role === "USER") {
      await this.loadHome();
    } else if (role === "TECHNICIAN") {
      await this.loadTechWorkbench();
    } else {
      await this.loadAdminWorkbench();
    }
  },

  async loadHome() {
    this.setData({ loading: true });
    try {
      const home = await getHomeData();
      this.setData({ home, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  goToProjectList() {
    wx.switchTab({ url: "/pages/tab-browse/index" });
  },

  goToProjectDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  goToTechnicianList() {
    wx.switchTab({ url: "/pages/tab-mid/index" });
  },

  goToTechnicianDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  async loadTechWorkbench() {
    this.setData({ loading: true });
    try {
      const profile = await request<any>({ url: "/api/v1/technician/profile" });
      this.setData({ techProfile: profile });
      try {
        const stats = await request<any>({ url: "/api/v1/technician/orders/stats" });
        if (stats) this.setData({ techStats: stats });
      } catch { /* 统计加载失败不影响主流程 */ }
      this.setData({ loading: false });
    } catch {
      try {
        const app = await request<any>({ url: "/api/v1/technician/application" });
        if (app) {
          this.setData({ techHasApplication: true, techApplicationStatus: app.status, loading: false });
        } else {
          this.setData({ loading: false });
        }
      } catch {
        this.setData({ loading: false });
      }
    }
  },

  async toggleOnline() {
    if (!this.data.techProfile) return;
    const newStatus = this.data.techProfile.onlineStatus === "ONLINE" ? "OFFLINE" : "ONLINE";
    try {
      await request({ url: "/api/v1/technician/online-status", method: "PUT", data: { onlineStatus: newStatus } });
      this.setData({ "techProfile.onlineStatus": newStatus });
      wx.showToast({ title: newStatus === "ONLINE" ? "已上线" : "已下线", icon: "success" });
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  goToAccess() {
    wx.navigateTo({ url: "/packageTech/pages/access/index" });
  },

  goToTechOrders() {
    wx.switchTab({ url: "/pages/tab-browse/index" });
  },

  goToSchedules() {
    wx.switchTab({ url: "/pages/tab-mid/index" });
  },

  goToIncome() {
    wx.switchTab({ url: "/pages/tab-last/index" });
  },

  async loadAdminWorkbench() {
    this.setData({ loading: true });
    try {
      const stats = await request<any>({ url: "/api/v1/admin/stats" });
      this.setData({ adminStats: stats || this.data.adminStats, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  goToAdminPage(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  handleLogin() {
    wx.navigateTo({ url: "/pages/login/index" });
  },

  goToAccount() {
    wx.switchTab({ url: "/pages/account/index" });
  },
});
