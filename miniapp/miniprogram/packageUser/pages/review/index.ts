import { createReview } from "../../services/order";

Page({
  data: {
    orderNo: "",
    score: 5,
    content: "",
    submitting: false,
  },

  onLoad(query: Record<string, string>) {
    this.setData({ orderNo: query.orderNo || "" });
  },

  handleScoreTap(e: WechatMiniprogram.TouchEvent) {
    this.setData({ score: parseInt(e.currentTarget.dataset.score) });
  },

  handleContentInput(e: WechatMiniprogram.Input) {
    this.setData({ content: e.detail.value });
  },

  async handleSubmit() {
    if (!this.data.orderNo) return;
    this.setData({ submitting: true });
    try {
      await createReview(this.data.orderNo, this.data.score, this.data.content);
      wx.showToast({ title: "评价成功", icon: "success" });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "评价失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
