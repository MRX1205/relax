import { createAfterSale } from "../../services/order";
import { uploadPrivateFile } from "../../../services/file";
import { environment } from "../../../config/environment";

const TYPES = ["服务质量", "技师行为", "收费问题", "未按时到达", "安全问题", "其他"];

Page({
  data: {
    orderNo: "",
    types: TYPES,
    selectedType: "",
    content: "",
    submitting: false,
    evidenceFiles: [] as { id: string; url: string }[],
    uploading: false,
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

  async handleChooseImage() {
    if (this.data.evidenceFiles.length >= 4) {
      wx.showToast({ title: "最多上传4张凭证", icon: "none" });
      return;
    }
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      const filePath = res.tempFiles[0].tempFilePath;
      this.setData({ uploading: true });
      wx.showLoading({ title: "上传凭证中…" });
      const fileAsset = await uploadPrivateFile(filePath, "AFTER_SALE_EVIDENCE");
      const url = `${environment.apiBaseUrl}/api/v1/public/files/${fileAsset.id}`;
      this.setData({
        evidenceFiles: [...this.data.evidenceFiles, { id: fileAsset.id, url }],
      });
    } catch {
      wx.showToast({ title: "上传失败", icon: "none" });
    } finally {
      this.setData({ uploading: false });
      wx.hideLoading();
    }
  },

  handleRemoveImage(e: WechatMiniprogram.TouchEvent) {
    const idx = Number(e.currentTarget.dataset.index);
    const files = [...this.data.evidenceFiles];
    files.splice(idx, 1);
    this.setData({ evidenceFiles: files });
  },

  async handleSubmit() {
    const { orderNo, selectedType, content, evidenceFiles } = this.data;
    if (!selectedType) return wx.showToast({ title: "请选择问题类型", icon: "none" });
    if (!content.trim()) return wx.showToast({ title: "请描述问题", icon: "none" });
    this.setData({ submitting: true });
    try {
      let finalContent = content.trim();
      if (evidenceFiles.length > 0) {
        finalContent += ` [图片凭证:${evidenceFiles.map(f => f.id).join(",")}]`;
      }
      await createAfterSale(orderNo, selectedType, finalContent);
      wx.showToast({ title: "已提交售后申请", icon: "success" });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "提交失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
