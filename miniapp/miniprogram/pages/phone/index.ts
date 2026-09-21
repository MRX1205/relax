import { bindPhone } from "../../services/auth";

Page({
  data: {
    loading: false,
    error: "",
  },

  async handleGetPhoneNumber(e: WechatMiniprogram.ButtonGetPhoneNumber) {
    if (!e.detail.code) {
      this.setData({ error: "需要授权手机号才能继续使用" });
      return;
    }
    this.setData({ loading: true, error: "" });
    try {
      const account = await bindPhone(e.detail.code);
      this.navigateAfterBind(account);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "绑定手机号失败" });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleSkip() {
    wx.reLaunch({ url: "/pages/home/index" });
  },

  navigateAfterBind(account: Account) {
    wx.reLaunch({ url: "/pages/home/index" });
  },
});
