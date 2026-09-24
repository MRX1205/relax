import { request, downloadAndOpenDocument } from "../../../services/http";

interface OverviewStats {
  totalOrders: number;
  totalRevenue: number;
  totalUsers: number;
  totalTechnicians: number;
  todayOrders: number;
  todayRevenue: number;
}

interface StatusCount {
  status: string;
  count: number;
  statusName?: string;
}

interface TechRanking {
  technicianId: number;
  name: string;
  orderCount: number;
  revenue: number;
}

interface DailyTrend {
  date: string;
  orderCount: number;
  validOrders: number;
}

Page({
  data: {
    loading: true,
    overview: null as OverviewStats | null,
    statusCounts: [] as StatusCount[],
    techRanking: [] as TechRanking[],
    dailyTrend: [] as DailyTrend[],

    // 筛选维度: 'ALL' (全量历史), 'DAY' (按单日), 'MONTH' (按整月)
    filterMode: "ALL" as "ALL" | "DAY" | "MONTH",
    selectedDate: "",
    selectedMonth: "",

    // 技师订单钻取弹窗
    showTechModal: false,
    selectedTech: null as TechRanking | null,
    techOrders: [] as any[],
    techOrdersLoading: false,
  },

  async onLoad() {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, "0");
    const dd = String(now.getDate()).padStart(2, "0");
    this.setData({
      selectedDate: `${yyyy}-${mm}-${dd}`,
      selectedMonth: `${yyyy}-${mm}`,
    });
    await this.loadStats();
  },

  async loadStats() {
    this.setData({ loading: true });
    try {
      const { filterMode, selectedDate, selectedMonth } = this.data;
      let queryParam = "";
      if (filterMode === "DAY" && selectedDate) {
        queryParam = `date=${selectedDate}`;
      } else if (filterMode === "MONTH" && selectedMonth) {
        queryParam = `month=${selectedMonth}`;
      }

      const q = queryParam ? `?${queryParam}` : "";
      const qTech = queryParam ? `?limit=10&${queryParam}` : "?limit=10";

      const [overview, statusCounts, techRanking, dailyTrend] = await Promise.all([
        request<OverviewStats>({ url: `/api/v1/admin/stats${q}` }).catch(() => null),
        request<any[]>({ url: `/api/v1/admin/stats/order-status${q}` }).catch(() => []),
        request<TechRanking[]>({ url: `/api/v1/admin/stats/tech-ranking${qTech}` }).catch(() => []),
        request<DailyTrend[]>({ url: `/api/v1/admin/stats/daily-trend?days=7` }).catch(() => []),
      ]);

      const statusNames: Record<string, string> = {
        PENDING_PAYMENT: "待支付",
        PAID: "已支付",
        ACCEPTED: "已接单",
        DEPARTED: "已出发",
        ARRIVED: "已到达",
        IN_SERVICE: "服务中",
        COMPLETED: "已完成",
        CANCELLED: "已取消",
        EXPIRED: "已过期",
        REFUNDING: "退款中",
        REFUNDED: "已退款",
      };

      const mappedStatuses = (statusCounts || []).map((sc: any) => ({
        ...sc,
        statusName: statusNames[sc.status] || sc.status,
      }));

      this.setData({
        overview,
        statusCounts: mappedStatuses,
        techRanking: techRanking || [],
        dailyTrend: dailyTrend || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  setFilterMode(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "ALL" | "DAY" | "MONTH";
    if (this.data.filterMode === mode) return;
    this.setData({ filterMode: mode }, () => {
      this.loadStats();
    });
  },

  handleDateChange(e: WechatMiniprogram.PickerChange) {
    const val = e.detail.value as string;
    this.setData({ selectedDate: val, filterMode: "DAY" }, () => {
      this.loadStats();
    });
  },

  handleMonthChange(e: WechatMiniprogram.PickerChange) {
    const val = e.detail.value as string;
    this.setData({ selectedMonth: val, filterMode: "MONTH" }, () => {
      this.loadStats();
    });
  },

  async handleTechTap(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.tech as TechRanking;
    if (!tech) return;

    this.setData({
      showTechModal: true,
      selectedTech: tech,
      techOrders: [],
      techOrdersLoading: true,
    });

    try {
      const { filterMode, selectedDate, selectedMonth } = this.data;
      let query = `technicianId=${tech.technicianId}`;
      if (filterMode === "DAY" && selectedDate) {
        query += `&date=${selectedDate}`;
      } else if (filterMode === "MONTH" && selectedMonth) {
        query += `&month=${selectedMonth}`;
      }

      const list = await request<any[]>({
        url: `/api/v1/admin/stats/technician-orders?${query}`,
      });

      this.setData({
        techOrders: list || [],
        techOrdersLoading: false,
      });
    } catch {
      this.setData({ techOrdersLoading: false });
      wx.showToast({ title: "加载技师订单失败", icon: "none" });
    }
  },

  closeTechModal() {
    this.setData({ showTechModal: false, selectedTech: null, techOrders: [] });
  },

  async handleExport() {
    const { filterMode, selectedDate, selectedMonth } = this.data;
    let query = "";
    if (filterMode === "DAY" && selectedDate) {
      query = `date=${selectedDate}`;
    } else if (filterMode === "MONTH" && selectedMonth) {
      query = `month=${selectedMonth}`;
    }
    const path = `/api/v1/admin/stats/export${query ? `?${query}` : ""}`;
    const name = `经营数据报表_${filterMode === "DAY" ? selectedDate : (filterMode === "MONTH" ? selectedMonth : "全期")}.xlsx`;
    try {
      await downloadAndOpenDocument(path, name, "xlsx");
    } catch {
      // handled inside downloadAndOpenDocument
    }
  },

  noop() {},

  getStatusName(status: string) {
    const names: Record<string, string> = {
      PENDING_PAYMENT: "待支付",
      PAID: "已支付",
      ACCEPTED: "已接单",
      DEPARTED: "已出发",
      ARRIVED: "已到达",
      IN_SERVICE: "服务中",
      COMPLETED: "已完成",
      CANCELLED: "已取消",
      EXPIRED: "已过期",
      REJECTED: "已拒单",
      REFUNDING: "退款中",
      REFUNDED: "已退款",
    };
    return names[status] || status;
  },
});
