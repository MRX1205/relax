import { request } from "../../services/http";
import { getAccessToken } from "../../services/http";

Page({
  data: {
    loading: true,
    stats: null as any,
    menus: [
      { name: "退款审核", desc: "审核退款申请", icon: "↩️", url: "/packageAdmin/pages/refunds/index" },
      { name: "结算管理", desc: "技师结算和付款", icon: "💰", url: "/packageAdmin/pages/settlements/index" },
      { name: "数据统计", desc: "经营数据看板", icon: "📊", url: "/packageAdmin/pages/stats/index" },
    ],
  },

  async onShow() {
    if (!getAccessToken()) {
      this.setData({ loading: false });
      return;
    }
    await this.loadStats();
  },

  async loadStats() {
    this.setData({ loading: true });
    try {
      const stats = await request<any>({ url: "/api/v1/admin/stats" });
      this.setData({ stats, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },
});
