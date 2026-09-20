import { loginWithWechat, getCachedAccount } from "../../services/auth";

Page({
  data: {
    loading: false,
    error: "",
  },

  onLoad() {
    // 检查是否已经登录
    const account = getCachedAccount();
    if (account) {
      this.navigateAfterLogin(account);
    }
  },

  async handleLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true, error: "" });
    try {
      const result = await loginWithWechat();
      wx.showToast({ title: "登录成功", icon: "success" });
      // 延迟跳转，避免路由冲突
      setTimeout(() => {
        this.navigateAfterLogin(result.account);
      }, 1000);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "登录失败，请重试" });
    } finally {
      this.setData({ loading: false });
    }
  },

  navigateAfterLogin(account: Account) {
    // 使用 reLaunch 避免页面栈问题
    wx.reLaunch({ url: "/pages/home/index" });
  },
});
