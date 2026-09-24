import { getCachedAccount } from "../../../services/auth";

Page({
  data: {
    account: null as Account | null,
    inviteCode: "RX-8869",
    totalEarnings: "90.00",
    invitedCount: 3,
    pendingReward: "30.00",
    withdrawModalVisible: false,
    withdrawAmount: "",
  },

  onLoad() {
    const account = getCachedAccount();
    const code = account ? `RX-${String(account.id).slice(-4)}` : "RX-8869";
    this.setData({
      account,
      inviteCode: code,
    });
  },

  copyCode() {
    wx.setClipboardData({
      data: this.data.inviteCode,
      success: () => {
        wx.showToast({ title: "邀请码已复制", icon: "success" });
      },
    });
  },

  openWithdraw() {
    this.setData({ withdrawModalVisible: true, withdrawAmount: "" });
  },

  closeWithdraw() {
    this.setData({ withdrawModalVisible: false });
  },

  handleAmountInput(e: WechatMiniprogram.Input) {
    this.setData({ withdrawAmount: e.detail.value });
  },

  confirmWithdraw() {
    const amt = parseFloat(this.data.withdrawAmount);
    if (isNaN(amt) || amt <= 0) {
      wx.showToast({ title: "请输入有效金额", icon: "none" });
      return;
    }
    const balance = parseFloat(this.data.totalEarnings);
    if (amt > balance) {
      wx.showToast({ title: "提现金额超过奖金余额", icon: "none" });
      return;
    }

    wx.showLoading({ title: "提交提现申请..." });
    setTimeout(() => {
      wx.hideLoading();
      this.setData({
        withdrawModalVisible: false,
        totalEarnings: (balance - amt).toFixed(2),
      });
      wx.showModal({
        title: "提现申请已受理",
        content: `¥${amt.toFixed(2)} 已发起企业付款至您的微信零钱，预计 1-2 小时内到账`,
        showCancel: false,
        confirmColor: "#E54D42",
      });
    }, 1000);
  },

  onShareAppMessage() {
    return {
      title: "送你20元按摩推拿新人券！专业技师一键到家",
      path: `/pages/home/index?inviteCode=${this.data.inviteCode}`,
      imageUrl: "",
    };
  },

  onShareTimeline() {
    return {
      title: "东莞到家 · 正规按摩·上门服务，领新人无门槛代金券",
      query: `inviteCode=${this.data.inviteCode}`,
    };
  },

  noop() {},
});
