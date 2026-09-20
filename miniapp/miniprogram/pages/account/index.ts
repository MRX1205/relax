import { getCachedAccount, loadCurrentAccount, updateProfile, logout, clearSession } from "../../services/auth";
import { getAccessToken } from "../../services/http";
import { uploadPrivateFile } from "../../services/file";

Page({
  data: {
    account: null as Account | null,
    editing: false,
    editNickname: "",
    saving: false,
  },

  async onShow() {
    // 检查是否有token
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
      this.setData({ account, editNickname: account.nickname || "" });
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
    } catch (err) {
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
      const account = await updateProfile(this.data.account?.nickname || "", `/api/v1/public/files/${file.id}`);
      this.setData({ account });
    } catch (err) {
      wx.showToast({ title: "头像更新失败", icon: "none" });
    } finally {
      wx.hideLoading();
    }
  },

  goToRoleSwitch() {
    wx.navigateTo({ url: "/pages/role-switch/index" });
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
