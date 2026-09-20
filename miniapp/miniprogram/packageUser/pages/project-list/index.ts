import { getProjects } from "../../../services/catalog";
import { request } from "../../../services/http";

interface Category { id: string; name: string; }

Page({
  data: {
    loading: true,
    projects: [] as ProjectBrief[],
    categories: [] as Category[],
    selectedCategoryId: "",
    page: 0,
    hasMore: true,
  },

  async onLoad(query: Record<string, string>) {
    const categories = await request<Category[]>({ url: "/api/v1/categories" });
    this.setData({ categories, selectedCategoryId: query.categoryId || "" });
    this.loadProjects(true);
  },

  async loadProjects(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true, projects: [] });
    if (!this.data.hasMore && !reset) return;
    try {
      const categoryId = parseInt(this.data.selectedCategoryId) || 0;
      const projects = await getProjects(categoryId, this.data.page);
      this.setData({
        projects: reset ? projects : [...this.data.projects, ...projects],
        page: this.data.page + 1,
        hasMore: projects.length >= 20,
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  handleCategoryTap(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id || "";
    this.setData({ selectedCategoryId: id });
    this.loadProjects(true);
  },

  handleProjectTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  onReachBottom() {
    this.loadProjects();
  },
});
