import { loginRoleWithWechat, loginWithPassword, switchRole } from "../../services/auth";

Page({
  data: {
    targetRole: "TECHNICIAN" as RoleCode,
    activeTab: "password" as "password" | "wechat",
    phone: "",
    password: "",
    showPassword: false,
    loading: false,
    errorMsg: "",
  },

  onLoad(options: Record<string, string | undefined>) {
    const role = (options.role === "ADMIN" ? "ADMIN" : "TECHNICIAN") as RoleCode;
    this.setData({ targetRole: role });

    wx.setNavigationBarTitle({
      title: role === "ADMIN" ? "管理员控制台登录" : "技师接单工作台登录",
    });
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as "password" | "wechat";
    this.setData({ activeTab: tab, errorMsg: "" });
  },

  handlePhoneInput(e: WechatMiniprogram.Input) {
    this.setData({ phone: e.detail.value.trim(), errorMsg: "" });
  },

  handlePasswordInput(e: WechatMiniprogram.Input) {
    this.setData({ password: e.detail.value, errorMsg: "" });
  },

  clearPhone() {
    this.setData({ phone: "" });
  },

  togglePasswordVisibility() {
    this.setData({ showPassword: !this.data.showPassword });
  },

  async handlePasswordSubmit() {
    const { phone, password, targetRole } = this.data;
    if (!phone || !/^1\d{10}$/.test(phone)) {
      this.setData({ errorMsg: "请输入正确的11位手机号" });
      return;
    }
    if (!password) {
      this.setData({ errorMsg: "请输入登录密码" });
      return;
    }

    this.setData({ loading: true, errorMsg: "" });
    try {
      await loginWithPassword(phone, password, targetRole);
      wx.showToast({ title: "登录成功", icon: "success" });
      this.navigateToWorkbench();
    } catch (err: any) {
      const msg = err?.message || err?.errMsg || "登录失败，请检查手机号与密码";
      this.setData({ errorMsg: msg });
    } finally {
      this.setData({ loading: false });
    }
  },

  async handleWechatSubmit() {
    const { targetRole } = this.data;
    const roleName = targetRole === "ADMIN" ? "管理员" : "技师";

    this.setData({ loading: true, errorMsg: "" });
    try {
      await loginRoleWithWechat(targetRole);
      wx.showToast({ title: "微信登录成功", icon: "success" });
      this.navigateToWorkbench();
    } catch (err: any) {
      const errMsg = err?.message || err?.errMsg || "";
      // 未绑定微信或权限不足时弹出友好的模态框拦截，引导返回用户端或切换密码登录
      wx.showModal({
        title: "无法通过微信登录",
        content: `当前微信号尚未绑定${roleName}账号。\n\n请使用手机号与初始密码登录，进入工作台后一键绑定微信；或者返回顾客端。`,
        confirmText: "密码登录",
        cancelText: "返回用户端",
        confirmColor: targetRole === "ADMIN" ? "#1F2937" : "#059669",
        success: res => {
          if (res.confirm) {
            this.setData({ activeTab: "password", errorMsg: "" });
          } else if (res.cancel) {
            this.returnToUserHome();
          }
        },
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  navigateToWorkbench() {
    const { targetRole } = this.data;
    setTimeout(() => {
      if (targetRole === "ADMIN") {
        wx.reLaunch({ url: "/packageAdmin/pages/workbench/index" });
      } else {
        wx.reLaunch({ url: "/packageTech/pages/workbench/index" });
      }
    }, 400);
  },

  async returnToUserHome() {
    try {
      await switchRole("USER");
    } catch {
      // ignore
    }
    wx.switchTab({ url: "/pages/home/index" });
  },
});
