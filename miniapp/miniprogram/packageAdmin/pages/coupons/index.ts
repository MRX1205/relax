import { request } from "../../../services/http";

interface CouponTemplate {
  id: number | string;
  name: string;
  amount: number | string;
  minSpend: number | string;
  totalCount: number;
  remainCount?: number;
  status: "ACTIVE" | "INACTIVE";
}

Page({
  data: {
    loading: true,
    templates: [] as CouponTemplate[],
    showCreateModal: false,
    formName: "",
    formAmount: "",
    formMinSpend: "0",
    formTotalCount: "500",
    submitting: false,

    // 定向发放
    showGrantModal: false,
    grantTargetId: "" as string | number,
    targetUserIdInput: "",
    submittingGrant: false,
  },

  async onLoad() {
    await this.loadTemplates();
  },

  async onPullDownRefresh() {
    await this.loadTemplates();
    wx.stopPullDownRefresh();
  },

  async loadTemplates() {
    this.setData({ loading: true });
    try {
      const res = await request<CouponTemplate[]>({
        url: "/api/v1/admin/coupons",
      });
      this.setData({ templates: res || [], loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  openCreateModal() {
    this.setData({
      showCreateModal: true,
      formName: "",
      formAmount: "",
      formMinSpend: "0",
      formTotalCount: "500",
    });
  },

  closeCreateModal() {
    this.setData({ showCreateModal: false });
  },

  handleNameInput(e: WechatMiniprogram.Input) {
    this.setData({ formName: e.detail.value });
  },

  handleAmountInput(e: WechatMiniprogram.Input) {
    this.setData({ formAmount: e.detail.value });
  },

  handleMinSpendInput(e: WechatMiniprogram.Input) {
    this.setData({ formMinSpend: e.detail.value });
  },

  handleTotalCountInput(e: WechatMiniprogram.Input) {
    this.setData({ formTotalCount: e.detail.value });
  },

  async handleCreateSubmit() {
    const name = this.data.formName.trim();
    const amount = parseFloat(this.data.formAmount);
    const minSpend = parseFloat(this.data.formMinSpend) || 0;
    const totalCount = parseInt(this.data.formTotalCount, 10) || 100;

    if (!name) {
      wx.showToast({ title: "请输入卡券名称", icon: "none" });
      return;
    }
    if (isNaN(amount) || amount <= 0) {
      wx.showToast({ title: "请输入有效面额", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      await request({
        url: "/api/v1/admin/coupons",
        method: "POST",
        data: {
          name,
          amount,
          minSpend,
          totalCount,
        },
      });
      wx.showToast({ title: "优惠券创建成功", icon: "success" });
      this.setData({ showCreateModal: false, submitting: false });
      await this.loadTemplates();
    } catch (err: any) {
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "创建失败", icon: "none" });
    }
  },

  async handleGrantAll(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "全员派券确认",
        content: "确定向全平台所有注册顾客发放该优惠券吗？",
        confirmColor: "#FF382E",
        success: res => resolve(res.confirm),
      });
    });
    if (!confirmed) return;

    wx.showLoading({ title: "正在全员派发…" });
    try {
      const res = await request<number>({
        url: `/api/v1/admin/coupons/${id}/grant`,
        method: "POST",
        data: {},
      });
      wx.showToast({
        title: `成功发放至 ${res || 0} 位顾客！`,
        icon: "success",
      });
    } catch (err: any) {
      wx.showToast({ title: err?.message || "派发失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },

  openGrantUserModal(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    this.setData({
      showGrantModal: true,
      grantTargetId: id,
      targetUserIdInput: "",
    });
  },

  closeGrantModal() {
    this.setData({ showGrantModal: false });
  },

  handleTargetUserIdInput(e: WechatMiniprogram.Input) {
    this.setData({ targetUserIdInput: e.detail.value });
  },

  async handleConfirmGrantUser() {
    const uidStr = this.data.targetUserIdInput.trim();
    if (!uidStr) {
      wx.showToast({ title: "请输入顾客 User ID", icon: "none" });
      return;
    }
    const userId = parseInt(uidStr, 10);
    if (isNaN(userId)) {
      wx.showToast({ title: "用户ID必须为数字", icon: "none" });
      return;
    }

    this.setData({ submittingGrant: true });
    try {
      await request({
        url: `/api/v1/admin/coupons/${this.data.grantTargetId}/grant`,
        method: "POST",
        data: { userId },
      });
      wx.showToast({ title: "派发成功", icon: "success" });
      this.setData({ showGrantModal: false, submittingGrant: false });
    } catch (err: any) {
      this.setData({ submittingGrant: false });
      wx.showToast({ title: err?.message || "派发失败", icon: "none" });
    }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const currentStatus = e.currentTarget.dataset.status;
    const nextStatus = currentStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";

    try {
      await request({
        url: `/api/v1/admin/coupons/${id}/status`,
        method: "PUT",
        data: { status: nextStatus },
      });
      wx.showToast({ title: nextStatus === "ACTIVE" ? "已开启" : "已停用", icon: "success" });
      await this.loadTemplates();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  noop() {},
});
