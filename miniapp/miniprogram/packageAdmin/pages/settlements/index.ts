import { getAdminSettlements, markSettlementPaid, voidSettlement } from "../../services/order";

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "草稿", PENDING: "待付款", PAID: "已付款", VOID: "已作废",
};

Page({
  data: {
    loading: true,
    settlements: [] as SettlementView[],
    page: 0,
    hasMore: true,
  },

  onLoad() { this.loadSettlements(true); },

  async loadSettlements(reset = false) {
    if (reset) this.setData({ page: 0, hasMore: true });
    try {
      const settlements = await getAdminSettlements(this.data.page);
      const mapped = settlements.map(s => ({
        ...s,
        statusText: STATUS_LABELS[s.status] || s.status,
      }));
      this.setData({
        settlements: reset ? mapped : [...this.data.settlements, ...mapped],
        page: this.data.page + 1, hasMore: settlements.length >= 20, loading: false,
      });
    } catch { this.setData({ loading: false }); }
  },

  statusLabel(s: string): string { return STATUS_LABELS[s] || s; },

  async handlePay(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "登记付款",
      editable: true,
      placeholderText: "请输入凭证号",
      success: async (res) => {
        if (res.confirm && res.content) {
          try {
            await markSettlementPaid(id, res.content);
            wx.showToast({ title: "已登记", icon: "success" });
            this.loadSettlements(true);
          } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
        }
      },
    });
  },

  async handleVoid(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "作废结算单", content: "确认作废？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await voidSettlement(id);
      wx.showToast({ title: "已作废", icon: "success" });
      this.loadSettlements(true);
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  onReachBottom() { this.loadSettlements(); },
});
