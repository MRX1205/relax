import { bindCurrentWechat, getCachedAccount, loadCurrentAccount } from "../../../services/auth";
import { requireRole } from "../../../utils/auth-guard";

Page({
  data: {
    authorized: false,
    account: null as Account | null,
    menus: [
      { code: "categories", name: "分类管理", desc: "服务分类的增删改", url: "/packageAdmin/pages/categories/index" },
      { code: "projects", name: "项目管理", desc: "项目上架、定价和上下架", url: "/packageAdmin/pages/projects/index" },
      { code: "technicians", name: "技师管理", desc: "入驻审核、定价和状态管理", url: "/packageAdmin/pages/technicians/index" },
      { code: "orders", name: "订单管理", desc: "查看和处理订单", url: "/packageAdmin/pages/order-list/index" },
      { code: "refunds", name: "退款审核", desc: "审核退款申请", url: "/packageAdmin/pages/refunds/index" },
      { code: "settlements", name: "结算管理", desc: "技师结算和付款", url: "/packageAdmin/pages/settlements/index" },
      { code: "banners", name: "轮播图", desc: "管理首页轮播图", url: "/packageAdmin/pages/banners/index" },
    ],
  },

  async onShow() {
    const ok = await requireRole("ADMIN", {
      deniedMessage: "您不是平台受邀管理员，无权访问管理控制台",
      fallbackUrl: "/pages/home/index",
    });
    if (ok) {
      let account = getCachedAccount();
      try {
        account = await loadCurrentAccount();
      } catch {
        // use cached
      }
      this.setData({ authorized: true, account });
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

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    if (!this.data.authorized) return;
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  returnToUserMode() {
    wx.switchTab({ url: "/pages/home/index" });
  },
});
