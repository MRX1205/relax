import { getAddresses, deleteAddress } from "../../services/region";

Page({
  data: {
    loading: true,
    addresses: [] as UserAddress[],
  },

  onShow() {
    this.loadAddresses();
  },

  async loadAddresses() {
    try {
      const addresses = await getAddresses();
      this.setData({ addresses, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleSelect(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.setStorageSync("last_used_address_id", String(id));
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    }
  },

  handleAdd() {
    wx.navigateTo({ url: "/packageUser/pages/address-edit/index" });
  },

  handleEdit(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/address-edit/index?id=${id}` });
  },

  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "删除地址",
        content: "确认删除该地址？",
        success: res => resolve(res.confirm),
      });
    });
    if (!confirmed) return;
    try {
      await deleteAddress(id);
      this.loadAddresses();
    } catch {
      wx.showToast({ title: "删除失败", icon: "none" });
    }
  },
});
