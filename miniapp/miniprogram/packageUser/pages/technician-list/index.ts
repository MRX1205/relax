import { getTechnicians } from "../../../services/catalog";

Page({
  data: {
    loading: true,
    technicians: [] as TechnicianItem[],
    page: 0,
    hasMore: true,
  },

  onLoad() {
    this.loadTechnicians(true);
  },

  async loadTechnicians(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true, technicians: [] });
    if (!this.data.hasMore && !reset) return;
    try {
      const list = await getTechnicians(this.data.page);
      this.setData({
        technicians: reset ? list : [...this.data.technicians, ...list],
        page: this.data.page + 1,
        hasMore: list.length >= 20,
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${e.currentTarget.dataset.id}` });
  },

  onReachBottom() {
    this.loadTechnicians();
  },
});
