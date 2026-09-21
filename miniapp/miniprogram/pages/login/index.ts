import { loginWithWechat, loginWithPhone, getCachedAccount, switchRole } from "../../services/auth";
import { environment } from "../../config/environment";
import { setCustomApiBaseUrl, clearCustomApiBaseUrl } from "../../config/api-environment";

Page({
  data: {
    loading: false,
    error: "",
    loginTab: "user" as "user" | "staff",
    phone: "",
    password: "",
    agreed: true,
    cachedAccount: null as Account | null,

    // 网络与环境配置
    showEnvModal: false,
    currentApiBaseUrl: "",
    inputApiUrl: "",
    envVersion: "",
    presets: [
      { label: "电脑本地模拟器", url: "http://127.0.0.1:8080" },
      { label: "手机同一WiFi局域网", url: "http://192.168.1.7:8080" },
      { label: "云端生产正式域名", url: "https://realxback.lyhlz.cn" },
    ],
  },

  onLoad(options: { relogin?: string }) {
    const account = getCachedAccount();
    this.setData({
      cachedAccount: account,
      currentApiBaseUrl: environment.apiBaseUrl,
      inputApiUrl: environment.apiBaseUrl,
      envVersion: environment.version,
    });
  },

  onShow() {
    this.setData({
      currentApiBaseUrl: environment.apiBaseUrl,
      inputApiUrl: environment.apiBaseUrl,
      envVersion: environment.version,
    });
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as "user" | "staff";
    this.setData({ loginTab: tab, error: "" });
  },

  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  handlePhoneInput(e: WechatMiniprogram.Input) {
    this.setData({ phone: e.detail.value });
  },

  handlePasswordInput(e: WechatMiniprogram.Input) {
    this.setData({ password: e.detail.value });
  },

  // 微信授权一键快捷登录（普通用户端）
  async handleWechatLogin() {
    if (!this.checkAgreement()) return;
    if (this.data.loading) return;

    this.setData({ loading: true, error: "" });
    try {
      const result = await loginWithWechat();
      // 普通用户端登录，若有多个身份，确保当前角色切换为 USER
      if (result.account.roles.includes("USER")) {
        await switchRole("USER").catch(() => {});
      }
      wx.showToast({ title: "登录成功", icon: "success" });
      setTimeout(() => {
        this.navigateAfterLogin();
      }, 600);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "登录失败，请重试" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 快速体验：普通顾客
  async handleQuickUserLogin() {
    if (!this.checkAgreement()) return;
    this.setData({ loading: true, error: "" });
    try {
      const result = await loginWithWechat("test-user-001");
      if (result.account.roles.includes("USER")) {
        await switchRole("USER").catch(() => {});
      }
      wx.showToast({ title: "已进入顾客端", icon: "success" });
      setTimeout(() => {
        this.navigateAfterLogin();
      }, 600);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "快捷登录失败" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 服务端登录（手机号或微信快捷接入）
  async handleStaffLogin() {
    if (!this.checkAgreement()) return;
    if (this.data.loading) return;

    this.setData({ loading: true, error: "" });
    try {
      let account: Account;
      const phone = this.data.phone.trim();
      if (phone && phone.length === 11) {
        // 使用手机号登录
        const res = await loginWithPhone(phone);
        account = res.account;
      } else {
        // 快捷微信认证
        const res = await loginWithWechat();
        account = res.account;
      }

      // 自动切换为技师或管理员工作台
      if (account.roles.includes("TECHNICIAN")) {
        await switchRole("TECHNICIAN").catch(() => {});
      } else if (account.roles.includes("ADMIN") || account.roles.includes("SUPER_ADMIN")) {
        await switchRole("ADMIN").catch(() => {});
      }
      wx.showToast({ title: "服务端接入成功", icon: "success" });
      setTimeout(() => {
        this.navigateAfterLogin();
      }, 600);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "服务端登录失败" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 快速进入：技师接单工作台
  async handleQuickTechLogin() {
    if (!this.checkAgreement()) return;
    this.setData({ loading: true, error: "" });
    try {
      // 预置金牌技师手机号 13800003333
      const res = await loginWithPhone("13800003333");
      await switchRole("TECHNICIAN").catch(() => {});
      wx.showToast({ title: "已接入技师工作台", icon: "success" });
      setTimeout(() => {
        this.navigateAfterLogin();
      }, 600);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "技师登录失败" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 快速进入：BOSS 运营中心
  async handleQuickAdminLogin() {
    if (!this.checkAgreement()) return;
    this.setData({ loading: true, error: "" });
    try {
      // 预置超级管理员手机号 13800000000
      const res = await loginWithPhone("13800000000");
      await switchRole("SUPER_ADMIN").catch(() => {});
      wx.showToast({ title: "已接入BOSS管理中心", icon: "success" });
      setTimeout(() => {
        this.navigateAfterLogin();
      }, 600);
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "管理员登录失败" });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 检查协议勾选
  checkAgreement(): boolean {
    if (!this.data.agreed) {
      wx.showToast({ title: "请先阅读并同意用户协议与隐私政策", icon: "none" });
      return false;
    }
    return true;
  },

  navigateAfterLogin() {
    wx.reLaunch({ url: "/pages/home/index" });
  },

  // ══ 网络与环境配置弹窗 ══
  openEnvModal() {
    this.setData({
      showEnvModal: true,
      inputApiUrl: this.data.currentApiBaseUrl,
    });
  },

  closeEnvModal() {
    this.setData({ showEnvModal: false });
  },

  handleApiUrlInput(e: WechatMiniprogram.Input) {
    this.setData({ inputApiUrl: e.detail.value });
  },

  selectPresetUrl(e: WechatMiniprogram.TouchEvent) {
    const url = e.currentTarget.dataset.url as string;
    this.setData({ inputApiUrl: url });
  },

  saveEnvUrl() {
    const url = this.data.inputApiUrl.trim();
    if (!url || !url.startsWith("http")) {
      wx.showToast({ title: "请输入以 http:// 或 https:// 开头的有效地址", icon: "none" });
      return;
    }
    setCustomApiBaseUrl(url);
    this.setData({
      currentApiBaseUrl: url,
      showEnvModal: false,
    });
    wx.showToast({ title: "后端地址已切换", icon: "success" });
  },

  resetEnvUrl() {
    clearCustomApiBaseUrl();
    this.setData({
      currentApiBaseUrl: environment.apiBaseUrl,
      inputApiUrl: environment.apiBaseUrl,
      showEnvModal: false,
    });
    wx.showToast({ title: "已恢复默认配置", icon: "none" });
  },
});
