import { getPublicSystemSettings, updateSystemSettings } from "../../../services/system";

Page({
  data: {
    loading: true,
    saving: false,
    appName: "东莞到家",
    vipEnabled: false,
  },

  async onLoad() {
    await this.loadSettings();
  },

  async loadSettings() {
    this.setData({ loading: true });
    try {
      const res = await getPublicSystemSettings();
      this.setData({
        appName: res.appName || "东莞到家",
        vipEnabled: !!res.vipEnabled,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleAppNameInput(e: WechatMiniprogram.Input) {
    this.setData({ appName: e.detail.value });
  },

  handleVipToggle(e: any) {
    this.setData({ vipEnabled: e.detail.value });
  },

  async handleSave() {
    const appName = this.data.appName.trim();
    if (!appName) {
      wx.showToast({ title: "平台名称不能为空", icon: "none" });
      return;
    }

    this.setData({ saving: true });
    try {
      await updateSystemSettings({
        appName,
        vipEnabled: this.data.vipEnabled,
      });
      wx.showToast({ title: "设置已更新并生效", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 600);
    } catch (err: any) {
      this.setData({ saving: false });
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    }
  },
});
