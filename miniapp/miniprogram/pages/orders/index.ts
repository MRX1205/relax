import { getCachedAccount } from "../../services/auth";
import { request } from "../../services/http";

interface OrderItem {
  id: string;
  orderNo: string;
  status: string;
  projectName: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  amount: number;
  technicianName?: string;
  userName?: string;
  createdAt: string;
}

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: "待支付", color: "#E65100" },
  PAID: { text: "已支付", color: "#1565C0" },
  CONFIRMED: { text: "已确认", color: "#2E7D32" },
  TECHNICIAN_DEPARTING: { text: "技师出发中", color: "#6A1B9A" },
  TECHNICIAN_ARRIVED: { text: "技师已到达", color: "#00695C" },
  IN_SERVICE: { text: "服务中", color: "#EF6C00" },
  COMPLETED: { text: "已完成", color: "#2E7D32" },
  CANCELLED: { text: "已取消", color: "#757575" },
  REFUNDING: { text: "退款中", color: "#C62828" },
  REFUNDED: { text: "已退款", color: "#757575" },
};

const ACTIVE_STATUSES = new Set(["PENDING_PAYMENT", "PAID", "CONFIRMED", "TECHNICIAN_DEPARTING", "TECHNICIAN_ARRIVED", "IN_SERVICE"]);

Page({
  data: {
    role: "USER" as string,
    orders: [] as OrderItem[],
    filteredOrders: [] as OrderItem[],
    loading: true,
    currentTab: "all",
    tabs: [
      { key: "all", text: "全部" },
      { key: "active", text: "进行中" },
      { key: "completed", text: "已完成" },
    ],
  },

  async onShow() {
    const account = getCachedAccount();
    const role = account?.lastRole || "USER";
    this.setData({ role });
    await this.loadOrders();
  },

  async loadOrders() {
    this.setData({ loading: true });
    try {
      const role = this.data.role;
      let url = "/api/v1/orders";
      if (role === "TECHNICIAN") url = "/api/v1/technician/orders";
      if (role === "ADMIN" || role === "SUPER_ADMIN") url = "/api/v1/admin/orders";

      const orders = await request<OrderItem[]>({ url });
      this.setData({ orders, loading: false });
      this.filterOrders();
    } catch {
      this.setData({ loading: false });
    }
  },

  filterOrders() {
    const { orders, currentTab } = this.data;
    let filtered: OrderItem[];
    if (currentTab === "active") {
      filtered = orders.filter(o => ACTIVE_STATUSES.has(o.status));
    } else if (currentTab === "completed") {
      filtered = orders.filter(o => !ACTIVE_STATUSES.has(o.status));
    } else {
      filtered = orders;
    }
    this.setData({ filteredOrders: filtered });
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ currentTab: e.currentTarget.dataset.tab });
    this.filterOrders();
  },

  getStatusText(status: string) {
    return STATUS_MAP[status]?.text || status;
  },

  getStatusColor(status: string) {
    return STATUS_MAP[status]?.color || "#757575";
  },

  goToDetail(e: WechatMiniprogram.TouchEvent) {
    const orderNo = e.currentTarget.dataset.orderNo;
    const role = this.data.role;
    if (role === "TECHNICIAN") {
      wx.navigateTo({ url: `/packageTech/pages/order-detail/index?orderNo=${orderNo}` });
    } else if (role === "ADMIN" || role === "SUPER_ADMIN") {
      wx.navigateTo({ url: `/packageAdmin/pages/order-list/index?orderNo=${orderNo}` });
    } else {
      wx.navigateTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderNo}` });
    }
  },

  onPullDownRefresh() {
    this.loadOrders().then(() => wx.stopPullDownRefresh());
  },
});
