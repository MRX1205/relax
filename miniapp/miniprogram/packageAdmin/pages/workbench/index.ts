import { bindCurrentWechat, getCachedAccount, loadCurrentAccount, logout } from "../../../services/auth";
import { requireRole } from "../../../utils/auth-guard";
import { request } from "../../../services/http";
import { getPublicPaymentMode, PaymentModeInfo } from "../../services/payment";

interface StatsOverview {
  totalOrders: number;
  totalRevenue: number | string;
  totalUsers: number;
  totalTechnicians: number;
  todayOrders: number;
  todayRevenue: number | string;
}

Page({
  data: {
    authorized: false,
    account: null as Account | null,
    stats: {
      totalOrders: 0,
      totalRevenue: "0.00",
      totalUsers: 0,
      totalTechnicians: 0,
      todayOrders: 0,
      todayRevenue: "0.00",
    } as StatsOverview,
    pendingRefundCount: 0,
    pendingTechCount: 0,
    paymentModeInfo: {
      mode: "MOCK",
      label: "模拟支付",
      description: "开发体验模式",
    } as PaymentModeInfo,
    loadingStats: true,

    // 核心业务调度模块
    opsMenus: [
      { code: "orders", name: "订单全域调度", desc: "状态跟踪 · 技师改派", icon: "order", url: "/packageAdmin/pages/order-list/index", badge: "" },
      { code: "refunds", name: "退款审批中心", desc: "用户退款审核", icon: "refund", url: "/packageAdmin/pages/refunds/index", badge: "" },
      { code: "settlements", name: "技师结算出款", desc: "账单生成 · 付款登记", icon: "settle", url: "/packageAdmin/pages/settlements/index", badge: "" },
      { code: "stats", name: "经营数据大盘", desc: "单量趋势 · 技师排行", icon: "stats", url: "/packageAdmin/pages/stats/index", badge: "" },
    ],

    // 供给与系统配置模块
    configMenus: [
      { code: "technicians", name: "技师审核与管理", desc: "入驻资质 · 提成与状态", icon: "tech", url: "/packageAdmin/pages/technicians/index", badge: "" },
      { code: "users", name: "用户会员管理", desc: "注册顾客 · 状态冻结/解冻", icon: "user", url: "/packageAdmin/pages/users/index", badge: "" },
      { code: "projects", name: "服务项目配置", desc: "平台项目 · 定价上下架", icon: "project", url: "/packageAdmin/pages/projects/index", badge: "" },
      { code: "categories", name: "服务分类目录", desc: "分类管理与展示排序", icon: "category", url: "/packageAdmin/pages/categories/index", badge: "" },
      { code: "banners", name: "首页轮播管理", desc: "运营海报 · 页面跳转", icon: "banner", url: "/packageAdmin/pages/banners/index", badge: "" },
      { code: "payment", name: "支付与结算模式", desc: "Mock / 微信 / 现场支付", icon: "pay", url: "/packageAdmin/pages/payment-config/index", badge: "核心" },
      { code: "system", name: "系统基本设置", desc: "平台名称 · VIP入口开关", icon: "setting", url: "/packageAdmin/pages/system-settings/index", badge: "" },
    ],
  },

  async onShow() {
    const ok = await requireRole("ADMIN", {
      deniedMessage: "您不是平台受邀管理员，无权访问管理控制台",
      fallbackUrl: "/pages/home/index",
    });
    if (!ok) return;

    let account = getCachedAccount();
    try {
      account = await loadCurrentAccount();
    } catch {
      // use cached
    }
    this.setData({ authorized: true, account });

    // 并行加载大盘指标与模式信息
    await Promise.allSettled([
      this.loadDashboardStats(),
      this.loadPendingCounts(),
      this.loadPaymentMode(),
    ]);
  },

  async loadDashboardStats() {
    this.setData({ loadingStats: true });
    try {
      const stats = await request<StatsOverview>({ url: "/api/v1/admin/stats" });
      if (stats) {
        this.setData({ stats, loadingStats: false });
      }
    } catch {
      this.setData({ loadingStats: false });
    }
  },

  async loadPendingCounts() {
    try {
      const [techApps, refunds] = await Promise.allSettled([
        request<any[]>({ url: "/api/v1/admin/technicians/applications" }),
        request<any[]>({ url: "/api/v1/admin/refunds?page=0" }),
      ]);

      const techCount = techApps.status === "fulfilled" && Array.isArray(techApps.value) ? techApps.value.length : 0;
      const refundCount = refunds.status === "fulfilled" && Array.isArray(refunds.value)
        ? refunds.value.filter((r: any) => r.status === "PENDING").length
        : 0;

      const opsMenus = this.data.opsMenus.map(m => {
        if (m.code === "refunds" && refundCount > 0) {
          return { ...m, badge: `${refundCount}笔待审` };
        }
        return m;
      });

      const configMenus = this.data.configMenus.map(m => {
        if (m.code === "technicians" && techCount > 0) {
          return { ...m, badge: `${techCount}人申请` };
        }
        return m;
      });

      this.setData({
        pendingTechCount: techCount,
        pendingRefundCount: refundCount,
        opsMenus,
        configMenus,
      });
    } catch {}
  },

  async loadPaymentMode() {
    try {
      const modeInfo = await getPublicPaymentMode();
      if (modeInfo) {
        this.setData({ paymentModeInfo: modeInfo });
      }
    } catch {}
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

  async handleLogout() {
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "退出管理台",
        content: "确定退出当前管理员账号？",
        confirmColor: "#FF382E",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;
    try {
      await logout();
    } catch {}
    wx.reLaunch({ url: "/pages/home/index" });
  },
});
