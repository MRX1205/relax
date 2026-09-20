import { getTechOrders } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待支付", PAID: "待接单", ACCEPTED: "已接单", DEPARTED: "已出发",
  ARRIVED: "已到达", IN_SERVICE: "服务中", COMPLETED: "已完成",
  CANCELLED: "已取消", EXPIRED: "已过期", REJECTED: "已拒单",
};

Page({
  data: {
    loading: true,
    orders: [] as OrderView[],
    page: 0,
    hasMore: true,
  },

  onLoad() { this.loadOrders(true); },
  onShow() { this.loadOrders(true); },

  async loadOrders(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true });
    try {
      const orders = await getTechOrders(this.data.page);
      this.setData({
        orders: reset ? orders : [...this.data.orders, ...orders],
        page: this.data.page + 1,
        hasMore: orders.length >= 20,
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageTech/pages/order-detail/index?orderNo=${e.currentTarget.dataset.no}` });
  },

  onReachBottom() { this.loadOrders(); },
});
