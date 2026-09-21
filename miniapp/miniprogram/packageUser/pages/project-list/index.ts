import { getProjects } from "../../../services/catalog";
import { request } from "../../../services/http";
import { getProjectCover } from "../../../utils/assets";

interface Category { id: string; name: string; }

Page({
  data: {
    loading: true,
    projects: [] as any[],
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
      const categoryId = this.data.selectedCategoryId;
      const rawProjects = await getProjects(categoryId, this.data.page);
      const enriched = rawProjects.map(p => ({
        ...p,
        displayCover: getProjectCover(p.name, p.categoryName, p.coverFileId, (p as any).coverUrl),
      }));
      this.setData({
        projects: reset ? enriched : [...this.data.projects, ...enriched],
        page: this.data.page + 1,
        hasMore: rawProjects.length >= 20,
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
