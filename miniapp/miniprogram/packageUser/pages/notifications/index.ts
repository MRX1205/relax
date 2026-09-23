import { request } from "../../../services/http";

interface RawNotification {
  id: number | string;
  userId: number | string;
  type: string;
  title: string;
  content: string;
  relatedOrderNo: string | null;
  imageUrl?: string | null;
  readAt: string | null;
  sendStatus?: string;
  createdAt: string;
}

interface ProcessedNotification extends RawNotification {
  tagText: string;
  tagClass: string;
  icon: string;
  category: "ANNOUNCEMENT" | "ORDER" | "OTHER";
  formattedTime: string;
  expanded?: boolean;
}

function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return "";
  try {
    const d = new Date(dateStr.replace(" ", "T"));
    if (isNaN(d.getTime())) return dateStr;
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);

    if (diffMin < 1) return "刚刚";
    if (diffMin < 60) return `${diffMin}分钟前`;
    if (diffHour < 24 && now.getDate() === d.getDate()) {
      const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`);
      return `今天 ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
    const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`);
    const month = pad(d.getMonth() + 1);
    const day = pad(d.getDate());
    const hours = pad(d.getHours());
    const minutes = pad(d.getMinutes());
    return `${month}-${day} ${hours}:${minutes}`;
  } catch {
    return dateStr;
  }
}

function processNotification(n: RawNotification): ProcessedNotification {
  const type = (n.type || "").toUpperCase();
  let tagText = "系统通知";
  let tagClass = "tag-system";
  let icon = "🔔";
  let category: "ANNOUNCEMENT" | "ORDER" | "OTHER" = "OTHER";

  if (type === "ANNOUNCEMENT" || type === "BROADCAST") {
    tagText = "官方公告";
    tagClass = "tag-announcement";
    icon = "📢";
    category = "ANNOUNCEMENT";
  } else if (type === "ORDER_STATUS" || type === "DISPATCH" || type === "ORDER") {
    tagText = "订单动态";
    tagClass = "tag-order";
    icon = "📋";
    category = "ORDER";
  } else if (type === "REFUND") {
    tagText = "退款通知";
    tagClass = "tag-refund";
    icon = "↩️";
    category = "ORDER";
  } else if (type === "COUPON") {
    tagText = "福利礼券";
    tagClass = "tag-coupon";
    icon = "🎁";
    category = "ANNOUNCEMENT";
  }

  return {
    ...n,
    tagText,
    tagClass,
    icon,
    category,
    formattedTime: formatRelativeTime(n.createdAt),
  };
}

Page({
  data: {
    loading: true,
    allNotifications: [] as ProcessedNotification[],
    filteredNotifications: [] as ProcessedNotification[],
    currentTab: "ALL" as "ALL" | "ANNOUNCEMENT" | "ORDER",
    page: 0,
    hasMore: true,
    unreadCount: 0,
    announcementCount: 0,
    orderCount: 0,
  },

  async onLoad() {
    await this.loadNotifications(true);
  },

  async onPullDownRefresh() {
    await this.loadNotifications(true);
    wx.stopPullDownRefresh();
  },

  setTab(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as "ALL" | "ANNOUNCEMENT" | "ORDER";
    if (this.data.currentTab === tab) return;
    this.setData({ currentTab: tab });
    this.applyTabFilter(tab, this.data.allNotifications);
  },

  applyTabFilter(tab: "ALL" | "ANNOUNCEMENT" | "ORDER", list: ProcessedNotification[]) {
    let filtered = list;
    if (tab === "ANNOUNCEMENT") {
      filtered = list.filter((n) => n.category === "ANNOUNCEMENT");
    } else if (tab === "ORDER") {
      filtered = list.filter((n) => n.category === "ORDER");
    }
    this.setData({ filteredNotifications: filtered });
  },

  async loadNotifications(reset = false) {
    const page = reset ? 0 : this.data.page;
    if (reset) {
      this.setData({ page: 0, hasMore: true });
    }
    this.setData({ loading: true });

    try {
      const rawList = await request<RawNotification[]>({
        url: `/api/v1/notifications?page=${page}&size=20`,
      });
      const processed = (rawList || []).map(processNotification);
      const combined = reset ? processed : [...this.data.allNotifications, ...processed];

      const unreadCount = combined.filter((n) => !n.readAt).length;
      const announcementCount = combined.filter((n) => n.category === "ANNOUNCEMENT").length;
      const orderCount = combined.filter((n) => n.category === "ORDER").length;

      this.setData({
        allNotifications: combined,
        unreadCount,
        announcementCount,
        orderCount,
        hasMore: (rawList || []).length >= 20,
        loading: false,
      });

      this.applyTabFilter(this.data.currentTab, combined);
    } catch {
      this.setData({ loading: false });
    }
  },

  async markAllRead() {
    if (this.data.unreadCount === 0) return;
    try {
      wx.showLoading({ title: "正在同步..." });
      await request<void>({ url: "/api/v1/notifications/read-all", method: "POST" });
      const updated = this.data.allNotifications.map((n) => ({
        ...n,
        readAt: n.readAt || new Date().toISOString(),
      }));
      this.setData({
        allNotifications: updated,
        unreadCount: 0,
      });
      this.applyTabFilter(this.data.currentTab, updated);
      wx.hideLoading();
      wx.showToast({ title: "已全部标为已读", icon: "success" });
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  async handleCardTap(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const notification = this.data.allNotifications.find((n) => String(n.id) === String(id));
    if (!notification) return;

    // 如果未读，标记为已读
    if (!notification.readAt) {
      try {
        await request<void>({ url: `/api/v1/notifications/${notification.id}/read`, method: "POST" });
        const updated = this.data.allNotifications.map((n) =>
          String(n.id) === String(id) ? { ...n, readAt: new Date().toISOString() } : n
        );
        const unreadCount = updated.filter((n) => !n.readAt).length;
        this.setData({ allNotifications: updated, unreadCount });
        this.applyTabFilter(this.data.currentTab, updated);
      } catch {}
    }

    // 若有订单链接，跳转订单详情
    if (notification.relatedOrderNo) {
      wx.navigateTo({ url: `/packageUser/pages/order-detail/index?orderNo=${notification.relatedOrderNo}` });
    }
  },

  previewImage(e: WechatMiniprogram.TouchEvent) {
    const url = e.currentTarget.dataset.url;
    if (url) {
      wx.previewImage({
        urls: [url],
        current: url,
      });
    }
  },

  toggleExpand(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const updated = this.data.allNotifications.map((n) =>
      String(n.id) === String(id) ? { ...n, expanded: !n.expanded } : n
    );
    this.setData({ allNotifications: updated });
    this.applyTabFilter(this.data.currentTab, updated);
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 });
      this.loadNotifications(false);
    }
  },
});
