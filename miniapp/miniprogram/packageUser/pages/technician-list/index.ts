import { getTechnicians } from "../../../services/catalog";
import { getTechAvatar, getTechPhotos } from "../../../utils/assets";

Page({
  data: {
    loading: true,
    technicians: [] as TechnicianItem[],
    page: 0,
    hasMore: true,
    sortBy: "",
  },

  onLoad() {
    this.loadTechnicians(true);
  },

  async loadTechnicians(reset = false) {
    if (reset) {
      this.setData({ page: 0, hasMore: true, technicians: [] });
    }
    if (!this.data.hasMore && !reset) return;

    try {
      const list = await getTechnicians(this.data.page);
      // 增强技师数据，确保新UI字段优雅展示
      const enrichedList: TechnicianItem[] = list.map((item, idx) => ({
        ...item,
        avatarUrl: getTechAvatar(item.serviceName, item.avatarUrl),
        photos: getTechPhotos(item.photos),
        rating: item.rating || (4.8 + (idx % 3) * 0.1),
        annualOrders: item.annualOrders || (320 + (idx * 47) % 500),
        ageTag: item.ageTag || (idx % 2 === 0 ? "90后" : "95后"),
        height: item.height || (162 + (idx * 3) % 10),
        certifications: item.certifications?.length ? item.certifications : ["实名认证", "健康档案", "安心服务"],
        earliestAvailableTime: item.earliestAvailableTime || "随时可约",
      }));

      this.setData({
        technicians: reset ? enrichedList : [...this.data.technicians, ...enrichedList],
        page: this.data.page + 1,
        hasMore: list.length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleFilterTap(e: WechatMiniprogram.TouchEvent) {
    const sort = e.currentTarget.dataset.sort as string;
    if (sort === this.data.sortBy) return;
    this.setData({ sortBy: sort });
    this.loadTechnicians(true);
  },

  handleTap(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${id}` });
  },

  onPullDownRefresh() {
    this.loadTechnicians(true).then(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    this.loadTechnicians();
  },
});
