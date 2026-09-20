import { request } from "../../../services/http";

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
  },

  async onLoad() {
    await this.loadStats();
  },

  async loadStats() {
    this.setData({ loading: true });
    try {
      const [overview, statusCounts, techRanking, dailyTrend] = await Promise.all([
        request<OverviewStats>({ url: "/api/v1/admin/stats" }),
        request<StatusCount[]>({ url: "/api/v1/admin/stats/order-status" }),
        request<TechRanking[]>({ url: "/api/v1/admin/stats/tech-ranking?limit=5" }),
        request<DailyTrend[]>({ url: "/api/v1/admin/stats/daily-trend?days=7" }),
      ]);
      this.setData({ overview, statusCounts, techRanking, dailyTrend, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  formatMoney(amount: number) {
    return (amount / 100).toFixed(2);
  },

  getStatusName(status: string) {
    const names: Record<string, string> = {
      PENDING_PAYMENT: '待支付',
      PAID: '已支付',
      ACCEPTED: '已接单',
      DEPARTED: '已出发',
      ARRIVED: '已到达',
      IN_SERVICE: '服务中',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      EXPIRED: '已过期',
      REJECTED: '已拒单',
    };
    return names[status] || status;
  },
});
