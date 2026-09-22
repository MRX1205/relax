import { loginWithWechat, loginWithPassword, loginRoleWithWechat, switchRole } from "../../services/auth";
import { getPublicSystemSettings } from "../../services/system";

Page({
  data: {
    loading: false,
    error: "",
    loginTab: "user" as "user" | "staff",
    phone: "",
    password: "",
    agreed: true,
    appName: "东莞到家",
  },

  async onLoad() {
    try {
      const settings = await getPublicSystemSettings();
      if (settings?.appName) {
        this.setData({ appName: settings.appName });
        wx.setNavigationBarTitle({ title: settings.appName });
      }
    } catch {}
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as "user" | "staff";
    this.setData({ loginTab: tab, error: "" });
  },

  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  handlePhoneInput(e: WechatMiniprogram.Input) {
    this.setData({ phone: e.detail.value, error: "" });
  },

  handlePasswordInput(e: WechatMiniprogram.Input) {
    this.setData({ password: e.detail.value, error: "" });
  },

  // 微信授权快捷登录（用户端）
  async handleUserWechatLogin() {
    if (!this.checkAgreement()) return;
    if (this.data.loading) return;

    this.setData({ loading: true, error: "" });
    try {
      const result = await loginWithWechat();
      if (result.account.roles.includes("USER")) {
        await switchRole("USER").catch(() => {});
      }
      wx.showToast({ title: "登录成功", icon: "success" });
      setTimeout(() => {
        wx.switchTab({ url: "/pages/home/index" });
      }, 500);
    } catch (err: any) {
      this.setData({ error: err?.message || "登录失败，请重试" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 账号密码登录（服务端）
  async handleStaffPasswordLogin() {
    if (!this.checkAgreement()) return;
    const { phone, password } = this.data;
    if (!phone || !phone.trim()) {
      wx.showToast({ title: "请输入手机号", icon: "none" });
      return;
    }
    if (!password || !password.trim()) {
      wx.showToast({ title: "请输入密码", icon: "none" });
      return;
    }
    if (this.data.loading) return;

    this.setData({ loading: true, error: "" });
    try {
      const result = await loginWithPassword(phone.trim(), password.trim(), "STAFF");
      wx.showToast({ title: "验证成功", icon: "success" });
      setTimeout(() => {
        this.dispatchStaffRoute(result.account);
      }, 500);
    } catch (err: any) {
      this.setData({ error: err?.message || "登录失败，请检查账号密码" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 微信快捷登录（服务端：微信号已绑定管理员/技师账号）
  async handleStaffWechatLogin() {
    if (!this.checkAgreement()) return;
    if (this.data.loading) return;

    this.setData({ loading: true, error: "" });
    try {
      const result = await loginRoleWithWechat("STAFF");
      wx.showToast({ title: "验证成功", icon: "success" });
      setTimeout(() => {
        this.dispatchStaffRoute(result.account);
      }, 500);
    } catch (err: any) {
      this.setData({ error: err?.message || "微信登录失败，请确认是否已绑定服务端账号" });
    } finally {
      this.setData({ loading: false });
    }
  },

  dispatchStaffRoute(account: Account) {
    const roles = account.roles || [];
    if (roles.includes("ADMIN") || roles.includes("SUPER_ADMIN")) {
      wx.reLaunch({ url: "/packageAdmin/pages/workbench/index" });
    } else if (roles.includes("TECHNICIAN")) {
      wx.reLaunch({ url: "/packageTech/pages/workbench/index" });
    } else {
      wx.showModal({
        title: "提示",
        content: "当前账号尚未开通服务端权限，已为您进入顾客端。",
        showCancel: false,
        success: () => {
          wx.switchTab({ url: "/pages/home/index" });
        },
      });
    }
  },

  checkAgreement(): boolean {
    if (!this.data.agreed) {
      wx.showToast({ title: "请先阅读并同意用户协议与隐私政策", icon: "none" });
      return false;
    }
    return true;
  },
});
