import { getAdminBanners, createBanner, updateBannerStatus } from "../../services/order";

Page({
  data: {
    loading: true,
    banners: [] as Banner[],
    showForm: false,
    formTitle: "",
    formSort: "0",
    saving: false,
  },

  onLoad() { this.loadBanners(); },

  async loadBanners() {
    try {
      const banners = await getAdminBanners();
      this.setData({ banners, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  handleAdd() { this.setData({ showForm: true, formTitle: "", formSort: "0" }); },
  handleCancel() { this.setData({ showForm: false }); },
  handleTitleInput(e: WechatMiniprogram.Input) { this.setData({ formTitle: e.detail.value }); },
  handleSortInput(e: WechatMiniprogram.Input) { this.setData({ formSort: e.detail.value }); },

  async handleSave() {
    if (!this.data.formTitle.trim()) return wx.showToast({ title: "请输入标题", icon: "none" });
    this.setData({ saving: true });
    try {
      await createBanner({ title: this.data.formTitle.trim(), sort: parseInt(this.data.formSort) || 0 });
      this.setData({ showForm: false });
      this.loadBanners();
    } catch { wx.showToast({ title: "保存失败", icon: "none" }); }
    finally { this.setData({ saving: false }); }
  },

  async handleToggle(e: WechatMiniprogram.TouchEvent) {
    const banner = e.currentTarget.dataset.item as Banner;
    const newStatus = banner.status === "ENABLED" ? "DISABLED" : "ENABLED";
    try {
      await updateBannerStatus(banner.id, newStatus);
      this.loadBanners();
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },
});
