import { request } from "../../../services/http";
import { getProjectCover, getTechAvatar } from "../../../utils/assets";

interface SearchResponse {
  projects: any[];
  technicians: any[];
}

Page({
  data: {
    keyword: "",
    hasSearched: false,
    loading: false,
    currentTab: "all" as "all" | "projects" | "technicians",
    hotTags: ["全身推拿", "精油SPA", "肩颈调理", "经典足疗", "运动康复", "平台总管"],
    projects: [] as any[],
    technicians: [] as any[],
  },

  onLoad(options: { q?: string }) {
    if (options.q) {
      this.setData({ keyword: options.q });
      this.performSearch(options.q);
    }
  },

  handleInput(e: WechatMiniprogram.Input) {
    const val = e.detail.value;
    this.setData({ keyword: val });
    if (!val.trim()) {
      this.setData({
        hasSearched: false,
        projects: [],
        technicians: [],
      });
    }
  },

  handleClear() {
    this.setData({
      keyword: "",
      hasSearched: false,
      projects: [],
      technicians: [],
    });
  },

  handleBack() {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: "/pages/home/index" }),
    });
  },

  handleSelectTag(e: WechatMiniprogram.TouchEvent) {
    const tag = e.currentTarget.dataset.tag as string;
    this.setData({ keyword: tag });
    this.performSearch(tag);
  },

  handleSearch() {
    this.performSearch(this.data.keyword);
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as "all" | "projects" | "technicians";
    this.setData({ currentTab: tab });
  },

  async performSearch(q: string) {
    const query = (q || "").trim();
    if (!query) return;

    this.setData({ loading: true, hasSearched: true });
    try {
      const res = await request<SearchResponse>({
        url: `/api/v1/search?q=${encodeURIComponent(query)}`,
      });

      const projects = (res.projects || []).map((p: any) => ({
        ...p,
        displayCover: getProjectCover(p.name, p.categoryName, p.coverFileId, p.coverUrl),
      }));

      const technicians = (res.technicians || []).map((t: any) => ({
        ...t,
        displayAvatar: getTechAvatar(t.serviceName, t.avatarUrl),
      }));

      this.setData({
        projects,
        technicians,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
      wx.showToast({ title: "搜索失败，请稍后重试", icon: "none" });
    }
  },

  goToProjectDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${id}` });
  },

  goToTechDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${id}` });
  },
});
