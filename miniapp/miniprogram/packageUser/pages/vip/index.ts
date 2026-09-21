import { getCachedAccount } from "../../../services/auth";

Page({
  data: {
    account: null as Account | null,
    isVip: false,
    vipExpireDate: "2027-09-21",
    selectedPlan: "YEAR",
    plans: [
      { id: "MONTH", title: "月度VIP", price: "29", original: "49", tip: "0.9元/天" },
      { id: "QUARTER", title: "季度VIP", price: "79", original: "147", tip: "送20元券" },
      { id: "YEAR", title: "年度VIP", price: "199", original: "348", tip: "立省149元 · 超值" },
    ],
    privileges: [
      { icon: "💎", title: "全场项目9折", desc: "每次预约均享9折尊享优惠" },
      { icon: "🚗", title: "免出行交通费", desc: "平台全额补贴技师上门出行费" },
      { icon: "🎫", title: "月赠50元礼包", desc: "每月自动到账无门槛现金券" },
      { icon: "⚡", title: "优先派单通道", desc: "高峰期专享系统极速响应通道" },
      { icon: "🎧", title: "专属VIP客服", desc: "一对一贴心售后与咨询支持" },
    ],
    submitting: false,
  },

  onLoad() {
    const account = getCachedAccount();
    const isVip = wx.getStorageSync("relax.user.isVip") === true;
    this.setData({
      account,
      isVip,
    });
  },

  selectPlan(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    this.setData({ selectedPlan: id });
  },

  handleSubscribe() {
    const plan = this.data.plans.find(p => p.id === this.data.selectedPlan);
    if (!plan) return;

    this.setData({ submitting: true });
    wx.showLoading({ title: "发起支付..." });

    setTimeout(() => {
      wx.hideLoading();
      this.setData({ submitting: false, isVip: true });
      wx.setStorageSync("relax.user.isVip", true);
      wx.showModal({
        title: "开通成功 🎉",
        content: `恭喜您已开通【${plan.title}】！9折折扣与免出行费特权已立即生效。`,
        showCancel: false,
        confirmColor: "#D4AF37",
      });
    }, 1200);
  },
});
