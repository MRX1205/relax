import { getMyOrders } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待支付", PAID: "已支付", ACCEPTED: "已接单", DEPARTED: "已出发",
  ARRIVED: "已到达", IN_SERVICE: "服务中", COMPLETED: "已完成",
  CANCELLED: "已取消", EXPIRED: "已过期", REJECTED: "已拒单",
};

const STATUS_ICONS: Record<string, string> = {
  PENDING_PAYMENT: "⏳", PAID: "✅", ACCEPTED: "👍", DEPARTED: "🚗",
  ARRIVED: "📍", IN_SERVICE: "💆", COMPLETED: "🎉",
  CANCELLED: "❌", EXPIRED: "⏰", REJECTED: "🚫",
};

Page({
  data: {
    loading: true,
    orders: [] as any[],
    page: 0,
    hasMore: true,
  },

  async onLoad() {
    await this.loadOrders(true);
  },

  async onShow() {
    await this.loadOrders(true);
  },

  async loadOrders(reset = false) {
    if (reset) {
      this.setData({ page: 0, hasMore: true });
    }
    this.setData({ loading: true });
    try {
      const orders = await getMyOrders(this.data.page);
      const list = reset ? orders : [...this.data.orders, ...orders];
      this.setData({
        orders: list,
        hasMore: orders.length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  statusLabel(status: string): string {
    return STATUS_LABELS[status] || status;
  },

  statusIcon(status: string): string {
    return STATUS_ICONS[status] || "📋";
  },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    const orderNo = e.currentTarget.dataset.no;
    wx.navigateTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderNo}` });
  },

  async onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      await this.loadOrders(false);
    }
  },

  async onPullDownRefresh() {
    await this.loadOrders(true);
    wx.stopPullDownRefresh();
  },
});
