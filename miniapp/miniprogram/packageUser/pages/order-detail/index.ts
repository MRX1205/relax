import { getOrderDetail, cancelOrder } from "../../services/order";
import { formatOrderStatus, OrderStatusInfo } from "../../../utils/order-status";

Page({
  data: {
    loading: true,
    detail: null as OrderDetailView | null,
    statusInfo: null as OrderStatusInfo | null,
    formattedLogs: [] as Array<{ text: string; time: string; reason?: string }>,
    canCancel: false,
    canReview: false,
    canRefund: false,
    canAfterSale: false,
  },

  onLoad(query: Record<string, string>) {
    const orderNo = query.orderNo || "";
    this.loadDetail(orderNo);
  },

  async loadDetail(orderNo: string) {
    if (!orderNo) return;
    this.setData({ loading: true });
    try {
      const detail = await getOrderDetail(orderNo);
      const s = detail.order.status;
      const statusInfo = formatOrderStatus(s);

      const formattedLogs = (detail.statusLogs || []).map(log => ({
        text: formatOrderStatus(log.toStatus).text,
        time: log.createdAt,
        reason: log.reason || undefined,
      }));

      this.setData({
        detail,
        statusInfo,
        formattedLogs,
        canCancel: s === "PENDING_PAYMENT",
        canReview: s === "COMPLETED",
        canRefund: s === "PAID" || s === "COMPLETED",
        canAfterSale: s === "COMPLETED" || s === "IN_SERVICE",
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  openLocationMap() {
    const addr = this.data.detail?.addressSnapshot;
    if (!addr || !addr.latitude || !addr.longitude) {
      wx.showToast({ title: "该地址暂无精确经纬度", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: Number(addr.latitude),
      longitude: Number(addr.longitude),
      name: (addr.contactName || "服务") + "的预约地址",
      address: `${addr.regionName || ""} ${addr.detail || ""}`.trim(),
      scale: 16,
    });
  },

  callTechnician() {
    const phone = this.data.detail?.technicianPhone;
    if (!phone) {
      wx.showToast({ title: "暂无技师电话", icon: "none" });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },

  copyOrderNo() {
    const orderNo = this.data.detail?.order?.orderNo;
    if (!orderNo) return;
    wx.setClipboardData({
      data: orderNo,
      success: () => wx.showToast({ title: "单号已复制", icon: "success" }),
    });
  },

  async handleCancel() {
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "取消订单", content: "确认取消该订单？", success: res => r(res.confirm) });
    });
    if (!confirmed || !this.data.detail) return;
    try {
      await cancelOrder(this.data.detail.order.orderNo);
      wx.showToast({ title: "已取消", icon: "success" });
      this.loadDetail(this.data.detail.order.orderNo);
    } catch (err) { wx.showToast({ title: err instanceof Error ? err.message : "取消失败", icon: "none" }); }
  },

  goToReview() {
    wx.navigateTo({ url: `/packageUser/pages/review/index?orderNo=${this.data.detail?.order.orderNo}` });
  },

  goToAfterSale() {
    wx.navigateTo({ url: `/packageUser/pages/after-sale/index?orderNo=${this.data.detail?.order.orderNo}` });
  },

  handleRefund() {
    const orderNo = this.data.detail?.order.orderNo;
    if (!orderNo) return;
    wx.navigateTo({ url: `/packageUser/pages/refund/index?orderNo=${orderNo}` });
  },
});
