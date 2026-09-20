import { getTechOrderDetail, acceptOrder, rejectOrder, departOrder, arriveOrder, startService, completeService } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待支付", PAID: "待接单", ACCEPTED: "已接单", DEPARTED: "已出发",
  ARRIVED: "已到达", IN_SERVICE: "服务中", COMPLETED: "已完成",
  CANCELLED: "已取消", EXPIRED: "已过期", REJECTED: "已拒单",
};

const NEXT_ACTIONS: Record<string, { label: string; action: string }[]> = {
  PAID: [
    { label: "接单", action: "accept" },
    { label: "拒单", action: "reject" },
  ],
  ACCEPTED: [{ label: "出发", action: "depart" }],
  DEPARTED: [{ label: "到达", action: "arrive" }],
  ARRIVED: [{ label: "开始服务", action: "start" }],
  IN_SERVICE: [{ label: "完成服务", action: "complete" }],
};

Page({
  data: {
    loading: true,
    detail: null as OrderDetailView | null,
    actions: [] as { label: string; action: string }[],
    processing: false,
  },

  async onLoad(query: Record<string, string>) {
    try {
      const detail = await getTechOrderDetail(query.orderNo!);
      this.setData({
        detail,
        actions: NEXT_ACTIONS[detail.order.status] || [],
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },

  async handleAction(e: WechatMiniprogram.TouchEvent) {
    const action = e.currentTarget.dataset.action as string;
    const orderNo = this.data.detail?.order.orderNo;
    if (!orderNo || this.data.processing) return;

    if (action === "reject") {
      const confirmed = await new Promise<boolean>(r => {
        wx.showModal({ title: "拒单", content: "确认拒绝该订单？", success: res => r(res.confirm) });
      });
      if (!confirmed) return;
    }

    this.setData({ processing: true });
    try {
      switch (action) {
        case "accept": await acceptOrder(orderNo); break;
        case "reject": await rejectOrder(orderNo, "技师拒单"); break;
        case "depart": await departOrder(orderNo); break;
        case "arrive": await arriveOrder(orderNo); break;
        case "start": await startService(orderNo); break;
        case "complete": await completeService(orderNo); break;
      }
      wx.showToast({ title: "操作成功", icon: "success" });
      this.onLoad({ orderNo });
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "操作失败", icon: "none" });
    } finally {
      this.setData({ processing: false });
    }
  },
});
