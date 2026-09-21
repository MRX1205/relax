import { getTechOrders } from "../../services/order";
import { formatOrderStatus } from "../../../utils/order-status";

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
      const orders = await getTechOrders(this.data.page);
      const rawList = reset ? orders : [...this.data.orders, ...orders];
      const list = (rawList || []).map((order: any) => {
        const sInfo = formatOrderStatus(order.status);
        return {
          ...order,
          statusText: sInfo.text,
          statusColor: sInfo.color,
          statusBg: sInfo.bg,
          statusIcon: sInfo.icon,
        };
      });
      this.setData({
        orders: list,
        page: (reset ? 0 : this.data.page) + 1,
        hasMore: (orders || []).length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    const orderNo = e.currentTarget.dataset.no;
    wx.navigateTo({ url: `/packageTech/pages/order-detail/index?orderNo=${orderNo}` });
  },

  async onPullDownRefresh() {
    await this.loadOrders(true);
    wx.stopPullDownRefresh();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders(false);
    }
  },
});
