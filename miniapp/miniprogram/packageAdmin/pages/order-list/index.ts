import {
  getAdminOrders,
  getAdminOrderDetail,
  adminCancelOrder,
  adminReassign,
  updateOrderNote,
} from "../../services/order";
import { request, downloadAndOpenDocument } from "../../../services/http";
import { formatOrderStatus } from "../../../utils/order-status";

Page({
  data: {
    loading: true,
    orders: [] as any[],
    rawOrders: [] as any[],
    page: 0,
    hasMore: true,
    showDetail: false,
    detail: null as any | null,
    detailStatusInfo: null as any | null,
    noteInput: "",
    activeTab: "ALL",
    tabs: [
      { key: "ALL", label: "全部" },
      { key: "PENDING_PAYMENT", label: "待支付" },
      { key: "PROCESSING", label: "履约中" },
      { key: "COMPLETED", label: "已完成" },
      { key: "CANCELLED", label: "已取消" },
    ],

    // 时间筛选维度
    dateFilterMode: "ALL" as "ALL" | "DAY" | "MONTH",
    selectedDate: "",
    selectedMonth: "",
  },

  onLoad() {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, "0");
    const dd = String(now.getDate()).padStart(2, "0");
    this.setData({
      selectedDate: `${yyyy}-${mm}-${dd}`,
      selectedMonth: `${yyyy}-${mm}`,
    });
    this.loadOrders(true);
  },

  onShow() {
    this.loadOrders(true);
  },

  async loadOrders(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true });
    this.setData({ loading: true });
    try {
      const { activeTab, dateFilterMode, selectedDate, selectedMonth, page } = this.data;
      const currentPage = reset ? 0 : page;
      const statusParam = activeTab === "ALL" ? "" : activeTab;
      const dateParam = dateFilterMode === "DAY" ? selectedDate : "";
      const monthParam = dateFilterMode === "MONTH" ? selectedMonth : "";

      const orders = await getAdminOrders(currentPage, statusParam, dateParam, monthParam);
      const rawList = reset ? orders : [...this.data.rawOrders, ...orders];
      const list = (rawList || []).map((order: any) => {
        const sInfo = formatOrderStatus(order.status);
        return {
          ...order,
          statusText: sInfo.text,
          statusColor: sInfo.color,
          statusBg: sInfo.bg,
          statusIcon: sInfo.icon,
        };
      });

      this.setData({
        rawOrders: rawList,
        orders: list,
        page: currentPage + 1,
        hasMore: (orders || []).length >= 20,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  setDateFilterMode(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "ALL" | "DAY" | "MONTH";
    if (this.data.dateFilterMode === mode) return;
    this.setData({ dateFilterMode: mode }, () => {
      this.loadOrders(true);
    });
  },

  handleDateChange(e: WechatMiniprogram.PickerChange) {
    const val = e.detail.value as string;
    this.setData({ selectedDate: val, dateFilterMode: "DAY" }, () => {
      this.loadOrders(true);
    });
  },

  handleMonthChange(e: WechatMiniprogram.PickerChange) {
    const val = e.detail.value as string;
    this.setData({ selectedMonth: val, dateFilterMode: "MONTH" }, () => {
      this.loadOrders(true);
    });
  },

  handleTabChange(e: WechatMiniprogram.TouchEvent) {
    const key = e.currentTarget.dataset.key;
    this.setData({ activeTab: key }, () => {
      this.loadOrders(true);
    });
  },

  async handleExport() {
    const { activeTab, dateFilterMode, selectedDate, selectedMonth } = this.data;
    let query = "";
    if (activeTab && activeTab !== "ALL") {
      query += `status=${activeTab}`;
    }
    if (dateFilterMode === "DAY" && selectedDate) {
      query += `${query ? "&" : ""}date=${selectedDate}`;
    } else if (dateFilterMode === "MONTH" && selectedMonth) {
      query += `${query ? "&" : ""}month=${selectedMonth}`;
    }
    const path = `/api/v1/admin/orders/export${query ? `?${query}` : ""}`;
    const name = `订单明细_${dateFilterMode === "DAY" ? selectedDate : (dateFilterMode === "MONTH" ? selectedMonth : "全期")}.xlsx`;
    try {
      await downloadAndOpenDocument(path, name, "xlsx");
    } catch {
      // Handled
    }
  },

  async handleTap(e: WechatMiniprogram.TouchEvent) {
    const orderNo = e.currentTarget.dataset.no;
    try {
      wx.showLoading({ title: "加载中..." });
      const detail = await getAdminOrderDetail(orderNo);
      wx.hideLoading();
      const sInfo = formatOrderStatus(detail.order.status);
      this.setData({
        detail,
        detailStatusInfo: sInfo,
        showDetail: true,
        noteInput: detail.order.note || "",
      });
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "加载失败", icon: "none" });
    }
  },

  closeDetail() {
    this.setData({ showDetail: false, detail: null, detailStatusInfo: null });
  },

  noop() {},

  handleNoteInput(e: WechatMiniprogram.Input) {
    this.setData({ noteInput: e.detail.value });
  },

  async handleSaveNote() {
    if (!this.data.detail) return;
    try {
      await updateOrderNote(this.data.detail.order.orderNo, this.data.noteInput);
      wx.showToast({ title: "备注已保存", icon: "success" });
      this.data.detail.order.note = this.data.noteInput;
    } catch {
      wx.showToast({ title: "保存失败", icon: "none" });
    }
  },

  openLocationMap() {
    const addr = this.data.detail?.addressSnapshot;
    if (!addr || !addr.latitude || !addr.longitude) {
      wx.showToast({ title: "暂无精确定位坐标", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: Number(addr.latitude),
      longitude: Number(addr.longitude),
      name: `${addr.contactName || "客户"}的服务地址`,
      address: `${addr.regionName || ""} ${addr.detail || ""}`.trim(),
      scale: 16,
    });
  },

  callPhone(e: WechatMiniprogram.TouchEvent) {
    const phone = e.currentTarget.dataset.phone;
    if (!phone) {
      wx.showToast({ title: "暂无电话", icon: "none" });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },

  copyText(e: WechatMiniprogram.TouchEvent) {
    const text = e.currentTarget.dataset.text;
    if (!text) return;
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: "已复制", icon: "success" }),
    });
  },

  async handleCancel() {
    if (!this.data.detail) return;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "取消订单",
        content: "确认以管理员身份强制取消该订单并退款？",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;
    try {
      await adminCancelOrder(this.data.detail.order.orderNo, "管理员操作取消");
      wx.showToast({ title: "已取消", icon: "success" });
      this.closeDetail();
      this.loadOrders(true);
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  async handleReassign() {
    if (!this.data.detail) return;
    try {
      const technicians = await request<Array<{ id: number; serviceName: string }>>({
        url: "/api/v1/admin/technicians",
      });
      if (!technicians || technicians.length === 0) {
        wx.showToast({ title: "暂无可选技师", icon: "none" });
        return;
      }
      const names = technicians.map(t => t.serviceName);
      const res = await new Promise<{ confirm: boolean; index: number }>(r => {
        wx.showActionSheet({
          itemList: names,
          success: s => r({ confirm: true, index: s.tapIndex }),
          fail: () => r({ confirm: false, index: 0 }),
        });
      });
      if (!res.confirm) return;
      const selected = technicians[res.index];
      await adminReassign(this.data.detail.order.orderNo, String(selected.id), "管理员改派");
      wx.showToast({ title: `已改派至 ${selected.serviceName}`, icon: "success" });
      this.closeDetail();
      this.loadOrders(true);
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  async onPullDownRefresh() {
    await this.loadOrders(true);
    wx.stopPullDownRefresh();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders();
    }
  },
});
