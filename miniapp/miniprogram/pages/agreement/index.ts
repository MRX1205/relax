import { getAgreement, consentToAgreement } from "../../services/agreement";

Page({
  data: {
    loading: true,
    error: "",
    submitting: false,
    agreement: null as Agreement | null,
    context: "" as string,
    showConsent: false,
  },

  onLoad(query: Record<string, string>) {
    const type = (query.type || "USER_AGREEMENT") as AgreementType;
    const context = query.context || "";
    this.setData({ context, showConsent: context === "LOGIN" || context === "ORDER" });
    this.loadAgreement(type);
  },

  async loadAgreement(type: AgreementType) {
    try {
      const agreement = await getAgreement(type);
      wx.setNavigationBarTitle({ title: agreement.title });
      this.setData({ agreement, loading: false });
    } catch (err) {
      this.setData({ error: "加载协议失败", loading: false });
    }
  },

  async handleConsent() {
    if (!this.data.agreement || this.data.submitting) return;
    this.setData({ submitting: true });
    try {
      await consentToAgreement(this.data.agreement.id, this.data.context as "LOGIN" | "ORDER");
      if (this.data.context === "LOGIN") {
        wx.redirectTo({ url: "/pages/phone/index" });
      } else {
        wx.navigateBack();
      }
    } catch (err) {
      wx.showToast({ title: "操作失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },

  handleBack() {
    wx.navigateBack({ fail: () => wx.reLaunch({ url: "/pages/home/index" }) });
  },
});
