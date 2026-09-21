import { getOrderDetail, requestRefund } from "../../services/order";

const REFUND_REASONS = [
  "行程有变，无法等待",
  "技师未按时到达",
  "服务质量不满意",
  "误操作下单",
  "其他原因",
];

Page({
  data: {
    loading: true,
    submitting: false,
    orderNo: "",
    detail: null as OrderDetailView | null,
    selectedReason: "",
    customReason: "",
    refundAmount: 0,
    maxRefundAmount: 0,
    reasonIndex: -1,
  },

  async onLoad(query: Record<string, string>) {
    const orderNo = query.orderNo || "";
    this.setData({ orderNo });
    await this.loadOrderDetail(orderNo);
  },

  async loadOrderDetail(orderNo: string) {
    this.setData({ loading: true });
    try {
      const detail = await getOrderDetail(orderNo);
      const maxAmount = detail.amount?.payableAmount || 0;
      this.setData({
        detail,
        maxRefundAmount: maxAmount,
        refundAmount: maxAmount,
        loading: false,
      });
    } catch {
      wx.showToast({ title: "加载失败", icon: "none" });
      this.setData({ loading: false });
    }
  },

  handleReasonSelect(e: WechatMiniprogram.TouchEvent) {
    const index = Number(e.currentTarget.dataset.index);
    const selectedReason = REFUND_REASONS[index];
    this.setData({ reasonIndex: index, selectedReason, customReason: "" });
  },

  handleCustomReasonInput(e: WechatMiniprogram.Input) {
    this.setData({ customReason: e.detail.value, reasonIndex: -1, selectedReason: "" });
  },

  handleAmountInput(e: WechatMiniprogram.Input) {
    const val = parseFloat(e.detail.value) || 0;
    const clamped = Math.min(val, this.data.maxRefundAmount);
    this.setData({ refundAmount: Math.round(clamped * 100) / 100 });
  },

  async handleSubmit() {
    const reason = this.data.selectedReason || this.data.customReason.trim();
    if (!reason) {
      wx.showToast({ title: "请选择或填写退款原因", icon: "none" });
      return;
    }
    if (this.data.refundAmount <= 0) {
      wx.showToast({ title: "退款金额不能为0", icon: "none" });
      return;
    }

    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "确认申请退款",
        content: `退款金额 ¥${this.data.refundAmount}，退款原因：${reason}`,
        confirmText: "确认申请",
        success: res => resolve(res.confirm),
      });
    });
    if (!confirmed) return;

    this.setData({ submitting: true });
    try {
      await requestRefund(this.data.orderNo, this.data.refundAmount, reason);
      wx.showToast({ title: "退款申请已提交", icon: "success" });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "提交失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
