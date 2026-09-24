import { getCachedAccount } from "../../services/auth";
import { getTechnicians } from "../../services/catalog";
import { request } from "../../services/http";
import { getAccessToken } from "../../services/http";
import { getTechAvatar, getTechPhotos } from "../../utils/assets";

Page({
  data: {
    role: "USER" as string,
    loading: true,
    technicians: [] as any[],
    sortBy: "default",
    opsMenus: [
      { name: "分类管理", icon: "📂", url: "/packageAdmin/pages/categories/index" },
      { name: "项目管理", icon: "💆", url: "/packageAdmin/pages/projects/index" },
      { name: "技师管理", icon: "👥", url: "/packageAdmin/pages/technicians/index" },
      { name: "轮播图", icon: "🖼️", url: "/packageAdmin/pages/banners/index" },
      { name: "Mock数据", icon: "🧪", url: "/packageAdmin/pages/mock-data/index" },
    ],
  },

  async onShow() {
    const account = getCachedAccount();
    const role = account?.lastRole || "USER";
    this.setData({ role });
    if (role === "USER") {
      await this.loadTechnicians();
    } else {
      this.setData({ loading: false });
    }
  },

  sortList(list: any[], sortBy: string) {
    const copy = [...list];
    if (sortBy === "rating") {
      copy.sort((a, b) => (Number(b.rating) || 0) - (Number(a.rating) || 0));
    } else if (sortBy === "price") {
      copy.sort((a, b) => (Number(a.startPrice) || 0) - (Number(b.startPrice) || 0));
    } else {
      // 综合推荐：在线优先，其次评分，再次服务单量
      copy.sort((a, b) => {
        const aOnline = a.onlineStatus === "ONLINE" ? 1 : 0;
        const bOnline = b.onlineStatus === "ONLINE" ? 1 : 0;
        if (aOnline !== bOnline) return bOnline - aOnline;
        const rDiff = (Number(b.rating) || 0) - (Number(a.rating) || 0);
        if (rDiff !== 0) return rDiff;
        return (Number(b.orderCount) || 0) - (Number(a.orderCount) || 0);
      });
    }
    return copy;
  },

  async loadTechnicians() {
    this.setData({ loading: true });
    try {
      const technicians = await getTechnicians();
      const enriched = technicians.map((item, idx) => {
        const anyItem = item as any;
        return {
          ...item,
          displayAvatar: getTechAvatar(item.serviceName, item.avatarUrl),
          displayPhotos: getTechPhotos(item.photos),
          rating: item.rating ? Number(item.rating).toFixed(1) : (4.8 + (idx % 3) * 0.1).toFixed(1),
          orderCount: anyItem.orderCount || item.annualOrders || (320 + (idx * 57) % 400),
          startPrice: item.startPrice || anyItem.minPrice || (168 + (idx * 30) % 100),
        };
      });
      const sorted = this.sortList(enriched, this.data.sortBy);
      this.setData({ technicians: sorted, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchSort(e: WechatMiniprogram.TouchEvent) {
    const sort = e.currentTarget.dataset.sort;
    if (this.data.sortBy === sort) return;
    this.setData({
      sortBy: sort,
      technicians: this.sortList(this.data.technicians, sort),
    });
  },

  goToTechDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  goToSchedules() {
    wx.navigateTo({ url: "/packageTech/pages/schedules/index" });
  },

  goToServiceAreas() {
    wx.navigateTo({ url: "/packageTech/pages/service-areas/index" });
  },

  goToOpsPage(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  onPullDownRefresh() {
    if (this.data.role === "USER") {
      this.loadTechnicians().then(() => wx.stopPullDownRefresh());
    } else {
      wx.stopPullDownRefresh();
    }
  },
});
