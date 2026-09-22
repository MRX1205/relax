import { bindPhone } from "../../services/auth";

Page({
  data: {
    loading: false,
    error: "",
    manualPhone: "",
  },

  async handleGetPhoneNumber(e: WechatMiniprogram.ButtonGetPhoneNumber) {
    if (!e.detail.code) {
      this.setData({ error: "如微信授权未完成，可直接在下方手动输入手机号绑定" });
      return;
    }
    this.setData({ loading: true, error: "" });
    try {
      const account = await bindPhone(e.detail.code);
      wx.showToast({ title: "绑定成功", icon: "success" });
      setTimeout(() => this.navigateAfterBind(account), 500);
    } catch (err: any) {
      this.setData({ error: err?.message || "微信授权失败，建议在下方手动输入手机号绑定" });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleManualInput(e: WechatMiniprogram.Input) {
    this.setData({ manualPhone: e.detail.value, error: "" });
  },

  async handleManualBind() {
    const phone = this.data.manualPhone.trim();
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: "请输入正确的11位手机号", icon: "none" });
      return;
    }
    this.setData({ loading: true, error: "" });
    try {
      const account = await bindPhone(phone);
      wx.showToast({ title: "绑定成功", icon: "success" });
      setTimeout(() => this.navigateAfterBind(account), 500);
    } catch (err: any) {
      this.setData({ error: err?.message || "绑定失败，请稍后重试" });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleSkip() {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: "/pages/home/index" }),
    });
  },

  navigateAfterBind(account: Account) {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: "/pages/home/index" }),
    });
  },
});
