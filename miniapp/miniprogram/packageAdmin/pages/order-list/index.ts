import { getAdminOrders, getAdminOrderDetail, adminCancelOrder, adminReassign, updateOrderNote } from "../../services/order";
import { request } from "../../../../services/http";

const STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: "待支付", PAID: "已支付", CANCELLED: "已取消", EXPIRED: "已过期",
  ACCEPTED: "已接单", IN_SERVICE: "服务中", COMPLETED: "已完成",
};

Page({
  data: {
    loading: true,
    orders: [] as OrderView[],
    page: 0,
    hasMore: true,
    showDetail: false,
    detail: null as OrderDetailView | null,
    noteInput: "",
  },

  onLoad() {
    this.loadOrders(true);
  },

  async loadOrders(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true });
    try {
      const orders = await getAdminOrders(this.data.page);
      this.setData({
        orders: reset ? orders : [...this.data.orders, ...orders],
        page: this.data.page + 1,
        hasMore: orders.length >= 20,
        loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },

  async handleTap(e: WechatMiniprogram.TouchEvent) {
    const orderNo = e.currentTarget.dataset.no;
    try {
      const detail = await getAdminOrderDetail(orderNo);
      this.setData({ detail, showDetail: true, noteInput: detail.order.note || "" });
    } catch { wx.showToast({ title: "加载失败", icon: "none" }); }
  },

  closeDetail() {
    this.setData({ showDetail: false, detail: null });
  },

  handleNoteInput(e: WechatMiniprogram.Input) {
    this.setData({ noteInput: e.detail.value });
  },

  async handleSaveNote() {
    if (!this.data.detail) return;
    try {
      await updateOrderNote(this.data.detail.order.orderNo, this.data.noteInput);
      wx.showToast({ title: "已保存", icon: "success" });
    } catch { wx.showToast({ title: "保存失败", icon: "none" }); }
  },

  async handleCancel() {
    if (!this.data.detail) return;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "取消订单", content: "确认取消该订单？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await adminCancelOrder(this.data.detail.order.orderNo, "管理员取消");
      wx.showToast({ title: "已取消", icon: "success" });
      this.closeDetail();
      this.loadOrders(true);
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  async handleReassign() {
    if (!this.data.detail) return;
    try {
      const technicians = await request<Array<{ id: number; serviceName: string }>>({ url: "/api/v1/admin/technicians" });
      if (!technicians || technicians.length === 0) {
        wx.showToast({ title: "暂无可选技师", icon: "none" });
        return;
      }
      const names = technicians.map(t => t.serviceName);
      const res = await new Promise<{ confirm: boolean; index: number }>(r => {
        wx.showActionSheet({ itemList: names, success: s => r({ confirm: true, index: s.tapIndex }), fail: () => r({ confirm: false, index: 0 }) });
      });
      if (!res.confirm) return;
      const selected = technicians[res.index];
      await adminReassign(this.data.detail.order.orderNo, String(selected.id), "管理员改派");
      wx.showToast({ title: "已改派", icon: "success" });
      this.closeDetail();
      this.loadOrders(true);
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  onReachBottom() {
    this.loadOrders();
  },
});
