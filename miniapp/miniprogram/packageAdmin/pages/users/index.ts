import { request } from "../../../services/http";

interface AccessUserItem {
  userId: number;
  phone: string;
  nickname: string;
  avatarUrl: string;
  status: "ACTIVE" | "DISABLED";
  roles: string[];
  adminEnabled: boolean;
  permissionGroups: string[];
}

Page({
  data: {
    loading: true,
    keyword: "",
    users: [] as AccessUserItem[],
    showDetailModal: false,
    selectedUser: null as AccessUserItem | null,
    loadingUserOrders: false,
    userOrders: [] as any[],
  },

  onLoad() {
    this.loadUsers();
  },

  async onPullDownRefresh() {
    await this.loadUsers();
    wx.stopPullDownRefresh();
  },

  handleKeywordInput(e: WechatMiniprogram.Input) {
    this.setData({ keyword: e.detail.value });
  },

  handleClearSearch() {
    this.setData({ keyword: "" });
    this.loadUsers();
  },

  handleSearch() {
    this.loadUsers();
  },

  async loadUsers() {
    this.setData({ loading: true });
    try {
      const kw = this.data.keyword.trim();
      const url = kw
        ? `/api/v1/admin/access/users?keyword=${encodeURIComponent(kw)}`
        : "/api/v1/admin/access/users";
      const list = await request<any[]>({ url });
      const mapped = (list || []).map((u: any) => ({
        ...u,
        userId: u.userId || u.id,
      }));
      this.setData({ users: mapped, loading: false });
    } catch {
      this.setData({ loading: false });
      wx.showToast({ title: "加载用户列表失败", icon: "none" });
    }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const userId = e.currentTarget.dataset.id as number;
    const currentStatus = e.currentTarget.dataset.status as string;
    const targetStatus = currentStatus === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const actionTxt = targetStatus === "DISABLED" ? "冻结该账号" : "解除冻结";

    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "提示",
        content: `确定要${actionTxt}吗？${targetStatus === "DISABLED" ? '冻结后用户将无法正常下单或登录。' : ''}`,
        confirmColor: targetStatus === "DISABLED" ? "#E54D42" : "#07C160",
        success: res => resolve(res.confirm),
      });
    });

    if (!confirmed) return;

    try {
      wx.showLoading({ title: "处理中..." });
      await request({
        url: `/api/v1/admin/access/users/${userId}/status`,
        method: "PUT",
        data: { status: targetStatus },
      });
      wx.hideLoading();
      wx.showToast({ title: `已${targetStatus === "DISABLED" ? '冻结' : '解冻'}`, icon: "success" });

      const updated = this.data.users.map(u => (u.userId === userId ? { ...u, status: targetStatus as any } : u));
      const selected = this.data.selectedUser && this.data.selectedUser.userId === userId
        ? { ...this.data.selectedUser, status: targetStatus as any }
        : this.data.selectedUser;
      this.setData({ users: updated, selectedUser: selected });
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  async openUserDetail(e: WechatMiniprogram.TouchEvent) {
    const user = e.currentTarget.dataset.user as AccessUserItem;
    if (!user) return;
    this.setData({
      selectedUser: user,
      showDetailModal: true,
      loadingUserOrders: true,
      userOrders: [],
    });

    try {
      const orders = await request<any[]>({
        url: `/api/v1/admin/orders?page=0&size=5`,
      });
      // 筛选出属于该用户的订单
      const userOrders = (orders || []).filter((o: any) => o.userId === user.userId || o.contactPhone === user.phone);
      this.setData({
        userOrders,
        loadingUserOrders: false,
      });
    } catch {
      this.setData({ loadingUserOrders: false });
    }
  },

  closeDetailModal() {
    this.setData({ showDetailModal: false, selectedUser: null });
  },

  viewUserOrders() {
    const user = this.data.selectedUser;
    if (!user) return;
    this.closeDetailModal();
    wx.navigateTo({
      url: `/packageAdmin/pages/order-list/index?userId=${user.userId}&phone=${user.phone || ''}`,
    });
  },

  copyUID() {
    if (!this.data.selectedUser) return;
    wx.setClipboardData({
      data: String(this.data.selectedUser.userId),
      success: () => wx.showToast({ title: "UID已复制", icon: "success" }),
    });
  },

  noop() {},
});
