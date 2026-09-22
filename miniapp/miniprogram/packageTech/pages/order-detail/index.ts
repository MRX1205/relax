import {
  getTechOrderDetail,
  acceptOrder,
  rejectOrder,
  departOrder,
  arriveOrder,
  startService,
  completeService,
} from "../../services/order";
import { formatOrderStatus, OrderStatusInfo } from "../../../utils/order-status";

const NEXT_ACTIONS: Record<string, { label: string; action: string }[]> = {
  PAID: [
    { label: "确认接单", action: "accept" },
    { label: "无法接单(拒单)", action: "reject" },
  ],
  ACCEPTED: [
    { label: "一键完成服务", action: "complete" },
    { label: "我已出发", action: "depart" },
  ],
  DEPARTED: [
    { label: "一键完成服务", action: "complete" },
    { label: "我已到达", action: "arrive" },
  ],
  ARRIVED: [
    { label: "一键完成服务", action: "complete" },
    { label: "开始服务", action: "start" },
  ],
  IN_SERVICE: [{ label: "确认完成服务", action: "complete" }],
};

Page({
  data: {
    loading: true,
    detail: null as OrderDetailView | null,
    statusInfo: null as OrderStatusInfo | null,
    formattedLogs: [] as Array<{ text: string; time: string; reason?: string }>,
    actions: [] as { label: string; action: string }[],
    processing: false,
  },

  onLoad(query: Record<string, string>) {
    const orderNo = query.orderNo || "";
    this.loadDetail(orderNo);
  },

  async loadDetail(orderNo: string) {
    if (!orderNo) return;
    this.setData({ loading: true });
    try {
      const detail = await getTechOrderDetail(orderNo);
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
        actions: NEXT_ACTIONS[s] || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  openLocationMap() {
    const addr = this.data.detail?.addressSnapshot;
    if (!addr || !addr.latitude || !addr.longitude) {
      wx.showToast({ title: "该订单暂无客户精确定位", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: Number(addr.latitude),
      longitude: Number(addr.longitude),
      name: (addr.contactName || "客户") + "的服务地址",
      address: `${addr.regionName || ""} ${addr.detail || ""}`.trim(),
      scale: 16,
    });
  },

  callCustomer() {
    const phone = this.data.detail?.addressSnapshot?.contactPhone;
    if (!phone) {
      wx.showToast({ title: "暂无客户电话", icon: "none" });
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

  async handleAction(e: WechatMiniprogram.TouchEvent) {
    const action = e.currentTarget.dataset.action as string;
    const orderNo = this.data.detail?.order.orderNo;
    if (!orderNo || this.data.processing) return;

    if (action === "reject") {
      const confirmed = await new Promise<boolean>(r => {
        wx.showModal({ title: "拒单提示", content: "确认拒绝该订单吗？拒单后订单将重新派单或全额退回客户", success: res => r(res.confirm) });
      });
      if (!confirmed) return;
    }

    this.setData({ processing: true });
    try {
      switch (action) {
        case "accept":
          await acceptOrder(orderNo);
          wx.showToast({ title: "已接单", icon: "success" });
          break;
        case "reject":
          await rejectOrder(orderNo, "技师临时有事无法接单");
          wx.showToast({ title: "已拒单", icon: "none" });
          break;
        case "depart":
          await departOrder(orderNo);
          wx.showToast({ title: "已出发", icon: "success" });
          break;
        case "arrive":
          await arriveOrder(orderNo);
          wx.showToast({ title: "已到达", icon: "success" });
          break;
        case "start":
          await startService(orderNo);
          wx.showToast({ title: "开始服务", icon: "success" });
          break;
        case "complete":
          await completeService(orderNo);
          wx.showToast({ title: "服务完成", icon: "success" });
          break;
      }
      await this.loadDetail(orderNo);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "操作失败", icon: "none" });
    } finally {
      this.setData({ processing: false });
    }
  },
});
