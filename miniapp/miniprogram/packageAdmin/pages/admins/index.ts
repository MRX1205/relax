import { request } from "../../../services/http";
import { getCachedAccount } from "../../../services/auth";

interface AdminUser {
  id: string | number;
  nickname: string;
  phone: string;
  status: string;
  roles: string[];
  permissionGroups: string[];
}

interface GroupInfo {
  id: string | number;
  code: string;
  name: string;
  permissions: string[];
}

Page({
  data: {
    loading: true,
    admins: [] as AdminUser[],
    groups: [] as GroupInfo[],
    currentUserId: "",

    // 新增弹窗
    showAddModal: false,
    addForm: {
      phone: "",
      password: "",
      nickname: "",
      selectedGroups: ["SUPPLY", "ORDERS"] as string[],
    },
    submittingAdd: false,

    // 重置密码弹窗
    showResetModal: false,
    resetTarget: null as AdminUser | null,
    resetPassword: "",
    submittingReset: false,

    // 修改权限弹窗
    showEditModal: false,
    editTarget: null as AdminUser | null,
    editSelectedGroups: [] as string[],
    submittingEdit: false,
  },

  async onLoad() {
    const acc = getCachedAccount();
    if (acc) {
      this.setData({ currentUserId: String(acc.id) });
    }
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [admins, groups] = await Promise.all([
        request<AdminUser[]>({ url: "/api/v1/admin/access/admins" }).catch(() => []),
        request<GroupInfo[]>({ url: "/api/v1/admin/access/groups" }).catch(() => []),
      ]);

      const defaultGroups = [
        { id: 1, code: "SUPPLY", name: "供给管理", permissions: ["technician:audit", "project:write"] },
        { id: 2, code: "ORDERS", name: "订单管理", permissions: ["order:manage", "order:read"] },
        { id: 3, code: "FINANCE", name: "财务管理", permissions: ["order:refund", "settlement:manage"] },
        { id: 4, code: "OPERATIONS", name: "运营管理", permissions: ["content:manage", "region:manage"] },
      ];

      this.setData({
        admins: admins || [],
        groups: (groups && groups.length > 0) ? groups : defaultGroups,
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  // ════════ 新增管理员 ════════
  openAddModal() {
    this.setData({
      showAddModal: true,
      addForm: {
        phone: "",
        password: "",
        nickname: "",
        selectedGroups: ["SUPPLY", "ORDERS"],
      },
    });
  },

  closeAddModal() {
    this.setData({ showAddModal: false });
  },

  handleAddInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`addForm.${field}`]: e.detail.value });
  },

  toggleAddGroup(e: WechatMiniprogram.TouchEvent) {
    const code = e.currentTarget.dataset.code as string;
    const selected = [...this.data.addForm.selectedGroups];
    const idx = selected.indexOf(code);
    if (idx >= 0) {
      selected.splice(idx, 1);
    } else {
      selected.push(code);
    }
    this.setData({ "addForm.selectedGroups": selected });
  },

  async handleConfirmAdd() {
    const { phone, password, nickname, selectedGroups } = this.data.addForm;
    if (!/^1\d{10}$/.test((phone || "").trim())) {
      wx.showToast({ title: "请输入11位手机号", icon: "none" });
      return;
    }
    if (!password || password.trim().length < 6) {
      wx.showToast({ title: "密码不能少于6位", icon: "none" });
      return;
    }

    this.setData({ submittingAdd: true });
    try {
      await request({
        url: "/api/v1/admin/access/admins",
        method: "POST",
        data: {
          phone: phone.trim(),
          password: password.trim(),
          nickname: (nickname || "").trim() || "管理员",
          groupCodes: selectedGroups,
        },
      });

      wx.showToast({ title: "管理员添加成功", icon: "success" });
      this.setData({ showAddModal: false, submittingAdd: false });
      await this.loadData();
    } catch (err: any) {
      this.setData({ submittingAdd: false });
      wx.showModal({ title: "添加失败", content: err?.message || "操作异常", showCancel: false });
    }
  },

  // ════════ 重置密码 ════════
  openResetModal(e: WechatMiniprogram.TouchEvent) {
    const admin = e.currentTarget.dataset.item as AdminUser;
    this.setData({
      showResetModal: true,
      resetTarget: admin,
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
        url: `/api/v1/admin/access/admins/${resetTarget.id}/reset-password`,
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

  // ════════ 修改权限组 ════════
  openEditModal(e: WechatMiniprogram.TouchEvent) {
    const admin = e.currentTarget.dataset.item as AdminUser;
    if (admin.roles.includes("SUPER_ADMIN")) {
      wx.showToast({ title: "超级管理员具备全局特权", icon: "none" });
      return;
    }
    this.setData({
      showEditModal: true,
      editTarget: admin,
      editSelectedGroups: [...(admin.permissionGroups || [])],
    });
  },

  closeEditModal() {
    this.setData({ showEditModal: false, editTarget: null });
  },

  toggleEditGroup(e: WechatMiniprogram.TouchEvent) {
    const code = e.currentTarget.dataset.code as string;
    const selected = [...this.data.editSelectedGroups];
    const idx = selected.indexOf(code);
    if (idx >= 0) {
      selected.splice(idx, 1);
    } else {
      selected.push(code);
    }
    this.setData({ editSelectedGroups: selected });
  },

  async handleConfirmEdit() {
    const { editTarget, editSelectedGroups } = this.data;
    if (!editTarget) return;

    this.setData({ submittingEdit: true });
    try {
      await request({
        url: `/api/v1/admin/access/users/${editTarget.id}`,
        method: "PUT",
        data: {
          enabled: true,
          groupCodes: editSelectedGroups,
        },
      });
      wx.showToast({ title: "权限已更新", icon: "success" });
      this.setData({ showEditModal: false, editTarget: null, submittingEdit: false });
      await this.loadData();
    } catch (err: any) {
      this.setData({ submittingEdit: false });
      wx.showToast({ title: err?.message || "更新失败", icon: "none" });
    }
  },

  // ════════ 移除管理员 ════════
  async handleDeleteAdmin(e: WechatMiniprogram.TouchEvent) {
    const admin = e.currentTarget.dataset.item as AdminUser;
    if (admin.roles.includes("SUPER_ADMIN")) {
      wx.showToast({ title: "超级管理员受安全保护，不可删除", icon: "none" });
      return;
    }
    if (String(admin.id) === this.data.currentUserId) {
      wx.showToast({ title: "不能移除当前登录的操作账号", icon: "none" });
      return;
    }

    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "移除管理员",
        content: `确定取消「${admin.nickname || admin.phone}」的管理员身份？取消后该账号将不再具备后台访问权限。`,
        confirmColor: "#FF382E",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      await request({
        url: `/api/v1/admin/access/admins/${admin.id}`,
        method: "DELETE",
      });
      wx.showToast({ title: "管理员已移除", icon: "success" });
      await this.loadData();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "操作失败", icon: "none" });
    }
  },

  noop() {},
});
