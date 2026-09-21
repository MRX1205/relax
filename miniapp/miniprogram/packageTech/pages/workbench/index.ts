import { bindCurrentWechat, getCachedAccount } from "../../../services/auth";
import { request } from "../../../services/http";

interface TechnicianProfile {
  id: string; userId: string; serviceName: string; realName: string;
  phone: string; status: string; onlineStatus: string;
}

Page({
  data: {
    account: null as Account | null,
    profile: null as TechnicianProfile | null,
    loading: true,
    hasApplication: false,
    applicationStatus: "",
    menus: [
      { code: "projects", name: "服务项目", desc: "自建项目与上下架管理", url: "/packageTech/pages/projects/index" },
      { code: "profile", name: "资料与相册", desc: "编辑艺名/风采相册/常驻定位", url: "/packageTech/pages/profile-edit/index" },
      { code: "orders", name: "接单管理", desc: "查看和履约服务订单", url: "/packageTech/pages/order-list/index" },
      { code: "income", name: "收入明细", desc: "查看收入和结算", url: "/packageTech/pages/income/index" },
      { code: "schedules", name: "排班管理", desc: "设置可服务时段", url: "/packageTech/pages/schedules/index" },
      { code: "service-areas", name: "服务区域", desc: "查看已开通区域", url: "/packageTech/pages/service-areas/index" },
    ],
  },

  async onShow() {
    const account = getCachedAccount();
    this.setData({ account });
    try {
      const profile = await request<TechnicianProfile>({ url: "/api/v1/technician/profile" });
      this.setData({ profile, loading: false });
    } catch {
      try {
        const app = await request<{ status: string } | null>({ url: "/api/v1/technician/application" });
        if (app) { this.setData({ hasApplication: true, applicationStatus: app.status, loading: false }); }
        else { this.setData({ loading: false }); }
      } catch { this.setData({ loading: false }); }
    }
  },

  async handleBindWechat() {
    wx.showLoading({ title: "正在绑定微信…" });
    try {
      const updatedAccount = await bindCurrentWechat();
      this.setData({ account: updatedAccount });
      wx.hideLoading();
      wx.showToast({ title: "微信绑定成功！", icon: "success" });
    } catch (err: any) {
      wx.hideLoading();
      const msg = err?.message || err?.errMsg || "绑定失败，请重试";
      wx.showModal({ title: "绑定提示", content: msg, showCancel: false });
    }
  },

  async toggleOnline() {
    if (!this.data.profile) return;
    const newStatus = this.data.profile.onlineStatus === "ONLINE" ? "OFFLINE" : "ONLINE";
    try {
      await request({ url: "/api/v1/technician/online-status", method: "PUT", data: { onlineStatus: newStatus } });
      this.setData({ "profile.onlineStatus": newStatus });
      wx.showToast({ title: newStatus === "ONLINE" ? "已上线" : "已下线", icon: "success" });
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: e.currentTarget.dataset.url }); },
  goToAccess() { wx.navigateTo({ url: "/packageTech/pages/access/index" }); },
  returnToUserMode() { wx.switchTab({ url: "/pages/home/index" }); },
});
