import { getCachedAccount } from "../../services/auth";
import { request } from "../../services/http";

Page({
  data: {
    account: null as Account | null,
    loading: true,
    stats: {
      totalOrders: 0,
      todayOrders: 0,
      totalRevenue: 0,
      totalUsers: 0,
      totalTechnicians: 0,
      pendingRefunds: 0,
    },
    quickMenus: [
      { code: "categories", name: "分类管理", desc: "服务分类的增删改", icon: "📂", url: "/packageAdmin/pages/categories/index" },
      { code: "projects", name: "项目管理", desc: "项目上架、定价", icon: "💆", url: "/packageAdmin/pages/projects/index" },
      { code: "technicians", name: "技师管理", desc: "入驻审核、定价", icon: "👥", url: "/packageAdmin/pages/technicians/index" },
      { code: "orders", name: "订单管理", desc: "查看和处理订单", icon: "📋", url: "/packageAdmin/pages/order-list/index" },
      { code: "refunds", name: "退款审核", desc: "审核退款申请", icon: "↩️", url: "/packageAdmin/pages/refunds/index" },
      { code: "settlements", name: "结算管理", desc: "技师结算和付款", icon: "💰", url: "/packageAdmin/pages/settlements/index" },
      { code: "banners", name: "轮播图", desc: "管理首页轮播图", icon: "🖼️", url: "/packageAdmin/pages/banners/index" },
    ],
  },

  async onShow() {
    const account = getCachedAccount();
    this.setData({ account });
    await this.loadStats();
  },

  async loadStats() {
    try {
      const stats = await request<any>({ url: "/api/v1/admin/dashboard" });
      this.setData({ stats, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  goToOrders() {
    wx.navigateTo({ url: "/packageAdmin/pages/order-list/index" });
  },

  goToRefunds() {
    wx.navigateTo({ url: "/packageAdmin/pages/refunds/index" });
  },
});
