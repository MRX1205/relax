import { getProjects } from "../../services/catalog";
import { request } from "../../services/http";
import { getProjectCover } from "../../utils/assets";

Page({
  data: {
    loading: true,
    projects: [] as any[],
    currentCategory: 0,
    categories: [] as any[],
  },

  async onShow() {
    await this.loadInitialData();
  },

  async loadInitialData() {
    this.setData({ loading: true });
    try {
      // 优先获取分类列表
      let categories: any[] = [];
      try {
        const catRes = await request<any[]>({ url: "/api/v1/categories" });
        if (Array.isArray(catRes) && catRes.length > 0) {
          categories = catRes;
        }
      } catch {}

      if (categories.length === 0) {
        try {
          const homeData = await request<any>({ url: "/api/v1/home" });
          categories = homeData.categories || [];
        } catch {}
      }

      this.setData({ categories });
      await this.loadProjects();
    } catch {
      this.setData({ loading: false });
    }
  },

  async loadProjects() {
    this.setData({ loading: true });
    try {
      const projects = await getProjects(this.data.currentCategory);
      const enriched = (projects || []).map((p: any) => ({
        ...p,
        displayCover: getProjectCover(p.name, p.categoryName, p.coverFileId, p.coverUrl),
      }));
      this.setData({ projects: enriched, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchCategory(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id);
    if (this.data.currentCategory === id) return;
    this.setData({ currentCategory: id });
    this.loadProjects();
  },

  goToProjectDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  async onPullDownRefresh() {
    await this.loadInitialData();
    wx.stopPullDownRefresh();
  },
});
