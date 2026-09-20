import { getTechnicians } from "../../services/catalog";

Page({
  data: {
    technicians: [] as any[],
    loading: true,
    page: 0,
    hasMore: true,
    sortBy: "default",
    sortOptions: [
      { key: "default", text: "默认" },
      { key: "rating", text: "评分优先" },
      { key: "orders", text: "接单量" },
      { key: "price", text: "价格" },
    ],
  },

  async onLoad() {
    await this.loadTechnicians();
  },

  async loadTechnicians(append = false) {
    this.setData({ loading: !append });
    try {
      const technicians = await getTechnicians(this.data.page);
      this.setData({
        technicians: append ? [...this.data.technicians, ...technicians] : technicians,
        hasMore: technicians.length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchSort(e: WechatMiniprogram.TouchEvent) {
    this.setData({ sortBy: e.currentTarget.dataset.sort, page: 0 });
    this.loadTechnicians();
  },

  goToDetail(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.setData({ page: this.data.page + 1 });
      this.loadTechnicians(true);
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 0 });
    this.loadTechnicians().then(() => wx.stopPullDownRefresh());
  },
});
