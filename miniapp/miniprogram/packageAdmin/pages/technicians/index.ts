import { request } from "../../../services/http";

interface Technician {
  id: string; userId: string; nickname: string; serviceName: string;
  realName: string; phone: string; status: string; onlineStatus: string;
}
interface Application {
  id: string; userId: string; serviceName: string; realName: string;
  phone: string; intro: string; experienceYears: number; status: string; rejectReason: string | null;
}

Page({
  data: {
    tab: "technicians" as "technicians" | "applications",
    loading: true,
    technicians: [] as Technician[],
    applications: [] as Application[],
    showReject: false,
    rejectId: "",
    rejectReason: "",
  },

  onLoad() {
    this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [technicians, applications] = await Promise.all([
        request<Technician[]>({ url: "/api/v1/admin/technicians" }),
        request<Application[]>({ url: "/api/v1/admin/technicians/applications/pending" }).catch(() => []),
      ]);
      this.setData({ technicians, applications, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  async handleApprove(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "审核通过", content: "确认通过该入驻申请？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await request({ url: `/api/v1/admin/technicians/applications/${id}/approve`, method: "POST" });
      wx.showToast({ title: "已通过", icon: "success" });
      this.loadData();
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  showRejectDialog(e: WechatMiniprogram.TouchEvent) {
    this.setData({ showReject: true, rejectId: e.currentTarget.dataset.id, rejectReason: "" });
  },

  handleRejectReasonInput(e: WechatMiniprogram.Input) {
    this.setData({ rejectReason: e.detail.value });
  },

  hideReject() {
    this.setData({ showReject: false });
  },

  async confirmReject() {
    try {
      await request({
        url: `/api/v1/admin/technicians/applications/${this.data.rejectId}/reject`,
        method: "POST",
        data: { reason: this.data.rejectReason || "不符合入驻要求" },
      });
      wx.showToast({ title: "已拒绝", icon: "success" });
      this.setData({ showReject: false });
      this.loadData();
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    const newStatus = tech.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try {
      await request({
        url: `/api/v1/admin/technicians/${tech.id}/status`,
        method: "PUT",
        data: { status: newStatus },
      });
      this.loadData();
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },

  goToPricing(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    wx.navigateTo({ url: `/packageAdmin/pages/tech-pricing/index?technicianId=${tech.id}&name=${tech.serviceName}` });
  },
});
