import { getPaymentConfig, updatePaymentConfig } from "../../services/payment";

Page({
  data: {
    loading: true,
    saving: false,
    selectedMode: "MOCK" as "MOCK" | "OFFLINE" | "WXPAY",
    formData: {
      appId: "",
      mchId: "",
      apiKey: "",
      serialNo: "",
      notifyUrl: "https://realxback.lyhlz.cn/api/v1/payments/wechat/notify",
      privateKey: "",
    },
  },

  async onLoad() {
    await this.loadConfig();
  },

  async loadConfig() {
    this.setData({ loading: true });
    try {
      const config = await getPaymentConfig();
      const rawMode = (config["payment.mode"] || "").toUpperCase();
      let mode: "MOCK" | "OFFLINE" | "WXPAY" = "MOCK";
      if (rawMode === "OFFLINE" || rawMode === "WXPAY" || rawMode === "MOCK") {
        mode = rawMode as any;
      } else if (config["wxpay.enabled"] === "true") {
        mode = "WXPAY";
      }

      this.setData({
        selectedMode: mode,
        formData: {
          appId: config["wxpay.app-id"] || config["wxpay.appId"] || "",
          mchId: config["wxpay.mch-id"] || config["wxpay.mchId"] || "",
          apiKey: "",
          serialNo: config["wxpay.serial-no"] || config["wxpay.serialNo"] || "",
          notifyUrl: config["wxpay.notify-url"] || config["wxpay.notifyUrl"] || "https://realxback.lyhlz.cn/api/v1/payments/wechat/notify",
          privateKey: "",
        },
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleSelectMode(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "MOCK" | "OFFLINE" | "WXPAY";
    if (mode) {
      this.setData({ selectedMode: mode });
    }
  },

  handleFieldInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field;
    if (field) {
      this.setData({ [`formData.${field}`]: e.detail.value });
    }
  },

  async handleSave() {
    this.setData({ saving: true });
    try {
      const { selectedMode, formData } = this.data;
      const payload: any = {
        paymentMode: selectedMode,
        enabled: selectedMode === "WXPAY",
      };
      if (formData.appId && formData.appId.trim()) payload.appId = formData.appId.trim();
      if (formData.mchId && formData.mchId.trim()) payload.mchId = formData.mchId.trim();
      if (formData.apiKey && formData.apiKey.trim()) payload.apiKey = formData.apiKey.trim();
      if (formData.serialNo && formData.serialNo.trim()) payload.serialNo = formData.serialNo.trim();
      if (formData.privateKey && formData.privateKey.trim()) payload.privateKey = formData.privateKey.trim();
      if (formData.notifyUrl && formData.notifyUrl.trim()) payload.notifyUrl = formData.notifyUrl.trim();

      await updatePaymentConfig(payload);
      wx.showToast({ title: "配置已更新", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 600);
    } catch (err: any) {
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
