import { getPaymentConfig, updatePaymentConfig, PaymentConfig } from "../../services/payment";

Page({
  data: {
    loading: true,
    saving: false,
    config: {
      "wxpay.appId": "",
      "wxpay.mchId": "",
      "wxpay.serialNo": "",
      "wxpay.notifyUrl": "",
      "wxpay.enabled": "false",
    } as PaymentConfig,
    apiKey: "",
    privateKey: "",
    enabled: false,
    showSuccess: false,
  },

  async onLoad() {
    await this.loadConfig();
  },

  async loadConfig() {
    this.setData({ loading: true });
    try {
      const config = await getPaymentConfig();
      this.setData({
        config,
        enabled: config["wxpay.enabled"] === "true",
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleInput(field: string) {
    return (e: WechatMiniprogram.Input) => {
      this.setData({ [`config.${field}`]: e.detail.value });
    };
  },

  handleApiKeyInput(e: WechatMiniprogram.Input) {
    this.setData({ apiKey: e.detail.value });
  },

  handlePrivateKeyInput(e: WechatMiniprogram.Input) {
    this.setData({ privateKey: e.detail.value });
  },

  handleEnabledChange(e: WechatMiniprogram.SwitchChange) {
    this.setData({ enabled: e.detail.value });
  },

  async handleSave() {
    this.setData({ saving: true });
    try {
      await updatePaymentConfig({
        appId: this.data.config["wxpay.appId"],
        mchId: this.data.config["wxpay.mchId"],
        apiKey: this.data.apiKey || undefined,
        serialNo: this.data.config["wxpay.serialNo"],
        privateKey: this.data.privateKey || undefined,
        notifyUrl: this.data.config["wxpay.notifyUrl"],
        enabled: this.data.enabled,
      });
      this.setData({ showSuccess: true });
      setTimeout(() => this.setData({ showSuccess: false }), 2000);
    } catch (err) {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
