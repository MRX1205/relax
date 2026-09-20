import { getCachedAccount } from "../../services/auth";
import { request } from "../../services/http";
import { getAccessToken } from "../../services/http";

Page({
  data: {
    role: "USER" as string,
    loading: true,
    orders: [] as any[],
    income: [] as any[],
    incomeStats: { total: 0, pending: 0, settled: 0 },
    financeMenus: [
      { name: "退款审核", desc: "审核退款申请", icon: "↩️", url: "/packageAdmin/pages/refunds/index" },
      { name: "结算管理", desc: "技师结算和付款", icon: "💰", url: "/packageAdmin/pages/settlements/index" },
      { name: "数据统计", desc: "经营数据看板", icon: "📊", url: "/packageAdmin/pages/stats/index" },
    ],
  },

  async onShow() {
    const account = getCachedAccount();
    const role = account?.lastRole || "USER";
    this.setData({ role });
    if (!getAccessToken()) {
      this.setData({ loading: false });
      return;
    }
    if (role === "USER") {
      await this.loadUserOrders();
    } else if (role === "TECHNICIAN") {
      await this.loadIncome();
    } else {
      this.setData({ loading: false });
    }
  },

  async loadUserOrders() {
    this.setData({ loading: true });
    try {
      const orders = await request<any[]>({ url: "/api/v1/orders" });
      this.setData({ orders, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  async loadIncome() {
    this.setData({ loading: true });
    try {
      const income = await request<any[]>({ url: "/api/v1/technician/incomes" });
      const total = income.reduce((s: number, i: any) => s + (i.payableAmount || 0), 0);
      const pending = income.filter((i: any) => i.status === "PENDING").reduce((s: number, i: any) => s + (i.payableAmount || 0), 0);
      const settled = income.filter((i: any) => i.status === "SETTLED").reduce((s: number, i: any) => s + (i.payableAmount || 0), 0);
      this.setData({ income, incomeStats: { total, pending, settled }, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  goToOrderDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/order-detail/index?id=${id}` });
  },

  goToFinancePage(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  goToFullIncome() {
    wx.navigateTo({ url: "/packageTech/pages/income/index" });
  },

  onPullDownRefresh() {
    const load = this.data.role === "USER" ? this.loadUserOrders() : this.data.role === "TECHNICIAN" ? this.loadIncome() : Promise.resolve();
    load.then(() => wx.stopPullDownRefresh());
  },
});
