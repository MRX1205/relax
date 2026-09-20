import { getTechIncomes, getTechSettlements } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  PENDING: "待结算", SETTLED: "已结算", DRAFT: "草稿", PAID: "已付款", VOID: "已作废",
};

Page({
  data: {
    tab: "income" as "income" | "settlements",
    loading: true,
    incomes: [] as IncomeView[],
    settlements: [] as SettlementView[],
    totalPending: 0,
  },

  async onLoad() {
    try {
      const [incomes, settlements] = await Promise.all([
        getTechIncomes(), getTechSettlements(),
      ]);
      const totalPending = incomes.filter(i => i.status === "PENDING").reduce((s, i) => s + i.payableAmount, 0);
      this.setData({ incomes, settlements, totalPending, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },
});
