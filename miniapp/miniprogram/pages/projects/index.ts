import { getProjects } from "../../services/catalog";

Page({
  data: {
    projects: [] as any[],
    loading: true,
    currentCategory: 0,
    categories: [] as any[],
    page: 0,
    hasMore: true,
  },

  async onLoad() {
    await this.loadCategories();
    await this.loadProjects();
  },

  async loadCategories() {
    try {
      const { request } = require("../../services/http");
      const categories = await request({ url: "/api/v1/home" });
      this.setData({ categories: categories.categories || [] });
    } catch {}
  },

  async loadProjects(append = false) {
    this.setData({ loading: !append });
    try {
      const projects = await getProjects(this.data.currentCategory, this.data.page);
      this.setData({
        projects: append ? [...this.data.projects, ...projects] : projects,
        hasMore: projects.length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchCategory(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ currentCategory: id, page: 0 });
    this.loadProjects();
  },

  goToDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadProjects(true);
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 0 });
    this.loadProjects().then(() => wx.stopPullDownRefresh());
  },
});
