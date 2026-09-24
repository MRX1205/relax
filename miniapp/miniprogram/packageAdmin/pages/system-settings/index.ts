import { getPublicSystemSettings, updateSystemSettings } from "../../../services/system";
import { uploadImageFile } from "../../../services/file";
import { environment } from "../../../config/environment";

Page({
  data: {
    loading: true,
    saving: false,
    uploadingQr: false,
    appName: "东莞到家",
    vipEnabled: false,
    servicePhone: "400-800-6688",
    customerQrUrl: "",
    inviteRewardAmount: "30.00",
  },

  async onLoad() {
    await this.loadSettings();
  },

  async loadSettings() {
    this.setData({ loading: true });
    try {
      const res = await getPublicSystemSettings();
      this.setData({
        appName: res.appName || "东莞到家",
        vipEnabled: !!res.vipEnabled,
        servicePhone: res.servicePhone || "400-800-6688",
        customerQrUrl: res.customerQrUrl || "",
        inviteRewardAmount: res.inviteRewardAmount != null ? String(res.inviteRewardAmount) : "30.00",
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleAppNameInput(e: WechatMiniprogram.Input) {
    this.setData({ appName: e.detail.value });
  },

  handleServicePhoneInput(e: WechatMiniprogram.Input) {
    this.setData({ servicePhone: e.detail.value });
  },

  handleInviteRewardInput(e: WechatMiniprogram.Input) {
    this.setData({ inviteRewardAmount: e.detail.value });
  },

  handleVipToggle(e: any) {
    this.setData({ vipEnabled: e.detail.value });
  },

  async chooseQrCode() {
    try {
      const res = await wx.chooseMedia({
        count: 1,
        mediaType: ["image"],
        sizeType: ["compressed"],
      });
      const filePath = res.tempFiles[0].tempFilePath;
      this.setData({ uploadingQr: true });
      wx.showLoading({ title: "上传二维码中…" });
      const url = await uploadImageFile(filePath, "QR_CODE");
      this.setData({ customerQrUrl: url });
      wx.showToast({ title: "上传成功", icon: "success" });
    } catch (err: any) {
      if (err?.errMsg?.indexOf("cancel") === -1) {
        wx.showToast({ title: "图片上传失败", icon: "none" });
      }
    } finally {
      this.setData({ uploadingQr: false });
      wx.hideLoading();
    }
  },

  previewQr() {
    if (this.data.customerQrUrl) {
      wx.previewImage({
        current: this.data.customerQrUrl,
        urls: [this.data.customerQrUrl],
      });
    }
  },

  clearQr() {
    this.setData({ customerQrUrl: "" });
  },

  async handleSave() {
    const appName = this.data.appName.trim();
    if (!appName) {
      wx.showToast({ title: "平台名称不能为空", icon: "none" });
      return;
    }

    const servicePhone = this.data.servicePhone.trim();
    if (!servicePhone) {
      wx.showToast({ title: "客服电话不能为空", icon: "none" });
      return;
    }

    const inviteReward = parseFloat(this.data.inviteRewardAmount);
    if (isNaN(inviteReward) || inviteReward < 0) {
      wx.showToast({ title: "请输入有效的邀请奖励金额", icon: "none" });
      return;
    }

    this.setData({ saving: true });
    try {
      await updateSystemSettings({
        appName,
        vipEnabled: this.data.vipEnabled,
        servicePhone,
        customerQrUrl: this.data.customerQrUrl,
        inviteRewardAmount: this.data.inviteRewardAmount.trim() || "30.00",
      });
      wx.showToast({ title: "设置已更新并生效", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 600);
    } catch (err: any) {
      this.setData({ saving: false });
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    }
  },
});
