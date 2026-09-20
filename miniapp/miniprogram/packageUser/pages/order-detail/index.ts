import { getOrderDetail, cancelOrder, requestRefund } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待支付", PAID: "已支付", ACCEPTED: "已接单", DEPARTED: "已出发",
  ARRIVED: "已到达", IN_SERVICE: "服务中", COMPLETED: "已完成",
  CANCELLED: "已取消", EXPIRED: "已过期", REJECTED: "已拒单", REASSIGNED: "已改派",
};

Page({
  data: {
    loading: true,
    detail: null as OrderDetailView | null,
    canCancel: false,
    canReview: false,
    canRefund: false,
    canAfterSale: false,
  },

  async onLoad(query: Record<string, string>) {
    try {
      const detail = await getOrderDetail(query.orderNo!);
      const s = detail.order.status;
      this.setData({
        detail,
        canCancel: s === "PENDING_PAYMENT",
        canReview: s === "COMPLETED",
        canRefund: s === "PAID" || s === "COMPLETED",
        canAfterSale: s === "COMPLETED" || s === "IN_SERVICE",
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(status: string): string { return STATUS_LABELS[status] || status; },

  async handleCancel() {
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "取消订单", content: "确认取消该订单？", success: res => r(res.confirm) });
    });
    if (!confirmed || !this.data.detail) return;
    try {
      await cancelOrder(this.data.detail.order.orderNo);
      wx.showToast({ title: "已取消", icon: "success" });
      this.onLoad({ orderNo: this.data.detail.order.orderNo });
    } catch (err) { wx.showToast({ title: err instanceof Error ? err.message : "取消失败", icon: "none" }); }
  },

  goToReview() {
    wx.navigateTo({ url: `/packageUser/pages/review/index?orderNo=${this.data.detail?.order.orderNo}` });
  },

  goToAfterSale() {
    wx.navigateTo({ url: `/packageUser/pages/after-sale/index?orderNo=${this.data.detail?.order.orderNo}` });
  },

  async handleRefund() {
    if (!this.data.detail?.amount) return;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "申请退款",
        content: `确认申请退款 ¥${this.data.detail!.amount!.payableAmount}？`,
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;
    try {
      await requestRefund(this.data.detail.order.orderNo, this.data.detail.amount.payableAmount, "用户申请退款");
      wx.showToast({ title: "退款申请已提交", icon: "success" });
      this.onLoad({ orderNo: this.data.detail.order.orderNo });
    } catch (err) { wx.showToast({ title: err instanceof Error ? err.message : "退款失败", icon: "none" }); }
  },
});
