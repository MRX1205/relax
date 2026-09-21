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

  async loadTechnicians() {
    this.setData({ loading: true });
    try {
      const technicians = await getTechnicians();
      const enriched = technicians.map(item => ({
        ...item,
        displayAvatar: getTechAvatar(item.serviceName, item.avatarUrl),
        displayPhotos: getTechPhotos(item.photos),
      }));
      this.setData({ technicians: enriched, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchSort(e: WechatMiniprogram.TouchEvent) {
    const sort = e.currentTarget.dataset.sort;
    this.setData({ sortBy: sort });
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
