import { getCachedAccount, loadCurrentAccount, updateProfile, logout, clearSession } from "../../services/auth";
import { getAccessToken, request } from "../../services/http";
import { uploadPrivateFile } from "../../services/file";
import { environment } from "../../config/environment";

Page({
  data: {
    account: null as Account | null,
    editing: false,
    editNickname: "",
    saving: false,

    // 角色身份判定
    isTechnician: false,
    isAdmin: false,

    // 管理员安全验证弹窗
    adminModalVisible: false,
    adminPinInput: "",
    verifyingPin: false,
  },

  async onShow() {
    if (!getAccessToken()) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }

    let account = getCachedAccount();
    if (!account) {
      try {
        account = await loadCurrentAccount();
      } catch {
        wx.reLaunch({ url: "/pages/login/index" });
        return;
      }
    }
    if (account) {
      const roles = account.roles || [];
      const isTechnician = roles.includes("TECHNICIAN") || roles.includes("SUPER_ADMIN");
      const isAdmin = roles.includes("ADMIN") || roles.includes("SUPER_ADMIN");

      this.setData({
        account,
        editNickname: account.nickname || "",
        isTechnician,
        isAdmin,
      });
    }
  },

  handleEditNickname() {
    this.setData({ editing: true });
  },

  handleNicknameInput(e: WechatMiniprogram.Input) {
    this.setData({ editNickname: e.detail.value });
  },

  async handleSaveNickname() {
    const nickname = this.data.editNickname.trim();
    if (!nickname || this.data.saving) return;
    this.setData({ saving: true });
    try {
      const account = await updateProfile(nickname, this.data.account?.avatarUrl || "");
      this.setData({ account, editing: false });
    } catch {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleChangeAvatar() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      const filePath = res.tempFiles[0].tempFilePath;
      wx.showLoading({ title: "上传中" });
      const file = await uploadPrivateFile(filePath, "AVATAR");
      const avatarUrl = `${environment.apiBaseUrl}/api/v1/public/files/${file.id}`;
      const account = await updateProfile(this.data.account?.nickname || "", avatarUrl);
      this.setData({ account });
    } catch {
      wx.showToast({ title: "头像更新失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },

  // === 导航动作 ===
  goToOrders() {
    wx.navigateTo({ url: "/packageUser/pages/order-list/index" });
  },

  goToVip() {
    wx.navigateTo({ url: "/packageUser/pages/vip/index" });
  },

  goToInvite() {
    wx.navigateTo({ url: "/packageUser/pages/invite/index" });
  },

  goToFollows() {
    wx.switchTab({ url: "/pages/tab-mid/index" });
  },

  goToFavorites() {
    wx.switchTab({ url: "/pages/tab-browse/index" });
  },

  goToAddresses() {
    wx.navigateTo({ url: "/packageUser/pages/address-list/index" });
  },

  goToNotifications() {
    wx.navigateTo({ url: "/packageUser/pages/notifications/index" });
  },

  goToCoupons() {
    wx.navigateTo({ url: "/packageUser/pages/coupons/index" });
  },

  goToRoleSwitch() {
    wx.navigateTo({ url: "/pages/role-switch/index" });
  },

  goToTechWorkbench() {
    wx.navigateTo({ url: "/packageTech/pages/workbench/index" });
  },

  goToTechAccess() {
    wx.navigateTo({ url: "/packageTech/pages/access/index" });
  },

  bindPhone() {
    wx.navigateTo({ url: "/pages/phone/index" });
  },

  // === 管理员二级口令安全验证 (2FA) ===
  openAdminAuth() {
    this.setData({ adminModalVisible: true, adminPinInput: "" });
  },

  closeAdminAuth() {
    this.setData({ adminModalVisible: false });
  },

  handlePinInput(e: WechatMiniprogram.Input) {
    this.setData({ adminPinInput: e.detail.value });
  },

  async confirmAdminPin() {
    const pin = this.data.adminPinInput.trim();
    if (!pin) {
      wx.showToast({ title: "请输入安全口令", icon: "none" });
      return;
    }

    this.setData({ verifyingPin: true });
    try {
      await request({
        url: "/api/v1/auth/verify-admin-pin",
        method: "POST",
        data: { pin },
      });
      this.setData({ adminModalVisible: false, verifyingPin: false });
      wx.showToast({ title: "口令验证通过", icon: "success" });
      setTimeout(() => {
        wx.navigateTo({ url: "/packageAdmin/pages/workbench/index" });
      }, 500);
    } catch (err: any) {
      this.setData({ verifyingPin: false });
      wx.showToast({ title: err?.message || "口令验证失败", icon: "none" });
    }
  },

  goToAgreement(e: WechatMiniprogram.TouchEvent) {
    const type = e.currentTarget.dataset.type as AgreementType;
    wx.navigateTo({ url: `/pages/agreement/index?type=${type}` });
  },

  async handleLogout() {
    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "确认退出",
        content: "退出后需要重新登录",
        success: res => resolve(res.confirm),
      });
    });
    if (!confirmed) return;
    try {
      await logout();
    } catch {
      clearSession();
    }
    wx.reLaunch({ url: "/pages/login/index" });
  },
});
