import { createAfterSale } from "../../services/order";

const TYPES = ["服务质量", "技师行为", "收费问题", "未按时到达", "安全问题", "其他"];

Page({
  data: {
    orderNo: "",
    types: TYPES,
    selectedType: "",
    content: "",
    submitting: false,
  },

  onLoad(query: Record<string, string>) {
    this.setData({ orderNo: query.orderNo || "" });
  },

  handleTypeTap(e: WechatMiniprogram.TouchEvent) {
    this.setData({ selectedType: e.currentTarget.dataset.type });
  },

  handleContentInput(e: WechatMiniprogram.Input) {
    this.setData({ content: e.detail.value });
  },

  async handleSubmit() {
    const { orderNo, selectedType, content } = this.data;
    if (!selectedType) return wx.showToast({ title: "请选择问题类型", icon: "none" });
    if (!content.trim()) return wx.showToast({ title: "请描述问题", icon: "none" });
    this.setData({ submitting: true });
    try {
      await createAfterSale(orderNo, selectedType, content.trim());
      wx.showToast({ title: "已提交", icon: "success" });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "提交失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
