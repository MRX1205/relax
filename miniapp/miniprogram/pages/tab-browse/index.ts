import { getCachedAccount } from "../../services/auth";
import { getProjects } from "../../services/catalog";
import { request } from "../../services/http";
import { getAccessToken } from "../../services/http";
import { getProjectCover } from "../../utils/assets";

Page({
  data: {
    role: "USER" as string,
    loading: true,
    projects: [] as any[],
    currentCategory: 0,
    categories: [] as any[],
    orders: [] as any[],
    orderTab: "all",
  },

  async onShow() {
    const account = getCachedAccount();
    const role = account?.lastRole || "USER";
    this.setData({ role });
    if (role === "USER") {
      await this.loadProjects();
    } else {
      if (!getAccessToken()) {
        this.setData({ loading: false });
        return;
      }
      await this.loadOrders();
    }
  },

  async loadProjects() {
    this.setData({ loading: true });
    try {
      const homeData = await request<any>({ url: "/api/v1/home" });
      const projects = await getProjects(this.data.currentCategory);
      const enriched = (projects || []).map((p: any) => ({
        ...p,
        displayCover: getProjectCover(p.name, p.categoryName, p.coverFileId, p.coverUrl),
      }));
      this.setData({ categories: homeData.categories || [], projects: enriched, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchCategory(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ currentCategory: id });
    this.loadProjects();
  },

  goToProjectDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/project-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  async loadOrders() {
    this.setData({ loading: true });
    try {
      const role = this.data.role;
      let url = "/api/v1/orders";
      if (role === "TECHNICIAN") url = "/api/v1/technician/orders";
      if (role === "ADMIN" || role === "SUPER_ADMIN") url = "/api/v1/admin/orders";
      const orders = await request<any[]>({ url });
      this.setData({ orders, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchOrderTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ orderTab: e.currentTarget.dataset.tab });
  },

  goToOrderDetail(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const role = this.data.role;
    if (role === "TECHNICIAN") {
      wx.navigateTo({ url: `/packageTech/pages/order-detail/index?id=${id}` });
    } else if (role === "ADMIN" || role === "SUPER_ADMIN") {
      wx.navigateTo({ url: `/packageAdmin/pages/order-list/index?id=${id}` });
    } else {
      wx.navigateTo({ url: `/packageUser/pages/order-detail/index?id=${id}` });
    }
  },

  onPullDownRefresh() {
    const load = this.data.role === "USER" ? this.loadProjects() : this.loadOrders();
    load.then(() => wx.stopPullDownRefresh());
  },
});
