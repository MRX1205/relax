import { request } from "../../../services/http";

interface Notification {
  id: string;
  userId: string;
  type: string;
  title: string;
  content: string;
  relatedOrderNo: string | null;
  readAt: string | null;
  createdAt: string;
}

Page({
  data: {
    loading: true,
    notifications: [] as Notification[],
    page: 0,
    hasMore: true,
    unreadCount: 0,
  },

  async onLoad() {
    await this.loadNotifications(true);
  },

  async loadNotifications(reset = false) {
    if (reset) {
      this.setData({ page: 0, hasMore: true });
    }
    this.setData({ loading: true });
    try {
      const notifications = await request<Notification[]>({
        url: `/api/v1/notifications?page=${this.data.page}`,
      });
      const list = reset ? notifications : [...this.data.notifications, ...notifications];
      const unreadCount = list.filter(n => !n.readAt).length;
      this.setData({
        notifications: list,
        unreadCount,
        hasMore: notifications.length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  async markAllRead() {
    try {
      await request<void>({ url: "/api/v1/notifications/read-all", method: "POST" });
      const notifications = this.data.notifications.map(n => ({
        ...n,
        readAt: n.readAt || new Date().toISOString(),
      }));
      this.setData({ notifications, unreadCount: 0 });
    } catch {}
  },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    const notification = e.currentTarget.dataset.item as Notification;
    if (notification.relatedOrderNo) {
      wx.navigateTo({ url: `/packageUser/pages/order-detail/index?orderNo=${notification.relatedOrderNo}` });
    }
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadNotifications(false);
    }
  },
});
