import { request } from "../../../services/http";

interface CouponTemplate {
  id: string;
  name: string;
  amount: number;
  minSpend: number;
  totalCount: number;
  issuedCount: number;
  startAt: string | null;
  endAt: string | null;
  status: string;
}

interface UserCoupon {
  id: string;
  templateId: string;
  status: string;
  name: string;
  amount: number;
  minSpend: number;
  endAt: string | null;
}

Page({
  data: {
    tab: "available" as "available" | "mine",
    loading: true,
    availableCoupons: [] as CouponTemplate[],
    myCoupons: [] as UserCoupon[],
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [available, mine] = await Promise.all([
        request<CouponTemplate[]>({ url: "/api/v1/coupons/available" }),
        request<UserCoupon[]>({ url: "/api/v1/coupons/mine" }),
      ]);
      this.setData({
        availableCoupons: available,
        myCoupons: mine,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  async handleClaim(e: WechatMiniprogram.TouchEvent) {
    const templateId = e.currentTarget.dataset.id;
    try {
      await request<void>({ url: `/api/v1/coupons/${templateId}/claim`, method: "POST" });
      wx.showToast({ title: "领取成功", icon: "success" });
      await this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err.message || "领取失败", icon: "none" });
    }
  },

  isExpired(endAt: string | null): boolean {
    if (!endAt) return false;
    return new Date(endAt) < new Date();
  },
});
