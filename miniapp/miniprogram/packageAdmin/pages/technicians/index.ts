import { request } from "../../../services/http";

interface Technician {
  id: string;
  userId: string;
  nickname: string;
  serviceName: string;
  realName: string;
  phone: string;
  status: string;
  onlineStatus: string;
  createdAt: string;
}

interface Application {
  id: string;
  userId: string;
  serviceName: string;
  realName: string;
  phone: string;
  intro: string;
  experienceYears: number;
  serviceAreaCodes?: string;
  photoFileId?: string | number;
  certificateFileId?: string | number;
  status: string;
  rejectReason: string | null;
  createdAt: string;
}

Page({
  data: {
    tab: "technicians" as "technicians" | "applications",
    loading: true,
    technicians: [] as Technician[],
    applications: [] as Application[],

    // 新增技师弹窗
    showAddModal: false,
    addForm: {
      phone: "",
      password: "",
      serviceName: "",
      realName: "",
      experienceYears: "3",
      intro: "",
    },
    submittingAdd: false,

    // 重置密码弹窗
    showResetModal: false,
    resetTarget: null as Technician | null,
    resetPassword: "",
    submittingReset: false,

    // 驳回弹窗
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
        request<Technician[]>({ url: "/api/v1/admin/technicians" }).catch(() => []),
        request<Application[]>({ url: "/api/v1/admin/technicians/applications/pending" }).catch(() => []),
      ]);
      this.setData({
        technicians: technicians || [],
        applications: applications || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  switchTab(e: WechatMiniprogram.TouchEvent) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  // ════════ 新增技师 ════════
  openAddModal() {
    this.setData({
      showAddModal: true,
      addForm: {
        phone: "",
        password: "",
        serviceName: "",
        realName: "",
        experienceYears: "3",
        intro: "",
      },
    });
  },

  closeAddModal() {
    this.setData({ showAddModal: false });
  },

  handleAddInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`addForm.${field}`]: e.detail.value,
    });
  },

  async handleConfirmAdd() {
    const { phone, password, serviceName, realName, experienceYears, intro } = this.data.addForm;
    if (!/^1\d{10}$/.test((phone || "").trim())) {
      wx.showToast({ title: "请输入11位手机号", icon: "none" });
      return;
    }
    if (!password || password.trim().length < 6) {
      wx.showToast({ title: "密码不能少于6位", icon: "none" });
      return;
    }
    if (!serviceName || !serviceName.trim()) {
      wx.showToast({ title: "请输入技师艺名", icon: "none" });
      return;
    }

    this.setData({ submittingAdd: true });
    try {
      await request({
        url: "/api/v1/admin/technicians",
        method: "POST",
        data: {
          phone: phone.trim(),
          password: password.trim(),
          serviceName: serviceName.trim(),
          realName: (realName || "").trim() || serviceName.trim(),
          experienceYears: parseInt(experienceYears) || 1,
          intro: (intro || "").trim() || "平台认证专业技师",
        },
      });

      wx.showToast({ title: "技师添加成功", icon: "success" });
      this.setData({ showAddModal: false, submittingAdd: false });
      await this.loadData();
    } catch (err: any) {
      this.setData({ submittingAdd: false });
      wx.showModal({ title: "添加失败", content: err?.message || "操作异常", showCancel: false });
    }
  },

  // ════════ 重置密码 ════════
  openResetModal(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    this.setData({
      showResetModal: true,
      resetTarget: tech,
      resetPassword: "",
    });
  },

  closeResetModal() {
    this.setData({ showResetModal: false, resetTarget: null });
  },

  handleResetPasswordInput(e: WechatMiniprogram.Input) {
    this.setData({ resetPassword: e.detail.value });
  },

  async handleConfirmReset() {
    const { resetTarget, resetPassword } = this.data;
    if (!resetTarget) return;
    if (!resetPassword || resetPassword.trim().length < 6) {
      wx.showToast({ title: "密码不能少于6位", icon: "none" });
      return;
    }

    this.setData({ submittingReset: true });
    try {
      await request({
        url: `/api/v1/admin/technicians/${resetTarget.id}/reset-password`,
        method: "POST",
        data: { password: resetPassword.trim() },
      });
      wx.showToast({ title: "密码已重置", icon: "success" });
      this.setData({ showResetModal: false, resetTarget: null, submittingReset: false });
    } catch (err: any) {
      this.setData({ submittingReset: false });
      wx.showToast({ title: err?.message || "重置失败", icon: "none" });
    }
  },

  // ════════ 删除技师 ════════
  async handleDeleteTech(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "确认停用/删除技师",
        content: `确定停用技师「${tech.serviceName}」？停用后该技师将无法登录与接单。`,
        confirmColor: "#FF382E",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      await request({
        url: `/api/v1/admin/technicians/${tech.id}`,
        method: "DELETE",
      });
      wx.showToast({ title: "已停用技师", icon: "success" });
      await this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  // ════════ 状态启用/停用切换 ════════
  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    const newStatus = tech.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try {
      await request({
        url: `/api/v1/admin/technicians/${tech.id}/status`,
        method: "PUT",
        data: { status: newStatus },
      });
      wx.showToast({ title: newStatus === "ACTIVE" ? "已启用" : "已停用", icon: "success" });
      this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  goToPricing(e: WechatMiniprogram.TouchEvent) {
    const tech = e.currentTarget.dataset.item as Technician;
    wx.navigateTo({ url: `/packageAdmin/pages/tech-pricing/index?technicianId=${tech.id}&name=${tech.serviceName}` });
  },

  // ════════ 入驻审核 ════════
  async handleApprove(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "审核通过", content: "确认通过该入驻申请？通过后技师即可开始接单。", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await request({ url: `/api/v1/admin/technicians/applications/${id}/approve`, method: "POST" });
      wx.showToast({ title: "审核已通过", icon: "success" });
      this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
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
      wx.showToast({ title: "已驳回申请", icon: "success" });
      this.setData({ showReject: false });
      this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  previewImage(e: WechatMiniprogram.TouchEvent) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    wx.previewImage({ urls: [url] });
  },

  makePhoneCall(e: WechatMiniprogram.TouchEvent) {
    const phone = e.currentTarget.dataset.phone;
    if (!phone) return;
    wx.makePhoneCall({ phoneNumber: phone });
  },
});
