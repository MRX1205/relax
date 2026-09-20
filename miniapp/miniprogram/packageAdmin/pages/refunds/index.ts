import { getAdminRefunds, approveRefund, rejectRefund } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING: "待审核", PROCESSING: "处理中", SUCCESS: "已退款", REJECTED: "已拒绝",
};

Page({
  data: {
    loading: true,
    refunds: [] as RefundView[],
    page: 0,
    hasMore: true,
  },

  onLoad() { this.loadRefunds(true); },

  async loadRefunds(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true });
    try {
      const refunds = await getAdminRefunds(this.data.page);
      this.setData({
        refunds: reset ? refunds : [...this.data.refunds, ...refunds],
        page: this.data.page + 1,
        hasMore: refunds.length >= 20,
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },

  async handleApprove(e: WechatMiniprogram.TouchEvent) {
    const refundNo = e.currentTarget.dataset.no;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "审核退款", content: "确认通过该退款申请？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await approveRefund(refundNo);
      wx.showToast({ title: "已通过", icon: "success" });
      this.loadRefunds(true);
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  async handleReject(e: WechatMiniprogram.TouchEvent) {
    const refundNo = e.currentTarget.dataset.no;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "拒绝退款", content: "确认拒绝该退款申请？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await rejectRefund(refundNo, "不符合退款条件");
      wx.showToast({ title: "已拒绝", icon: "success" });
      this.loadRefunds(true);
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  onReachBottom() { this.loadRefunds(); },
});
