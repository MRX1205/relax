import { getAdminBanners, createBanner, updateBanner, updateBannerStatus } from "../../services/order";
import { uploadPrivateFile } from "../../../services/file";
import { environment } from "../../../config/environment";

Page({
  data: {
    loading: true,
    banners: [] as Banner[],
    showForm: false,
    editingId: null as string | null,
    formTitle: "",
    formSort: "0",
    formImageFileId: null as string | null,
    formImageUrl: "",
    formLinkType: "NONE",
    formLinkValue: "",
    saving: false,
    uploading: false,
    linkTypes: [
      { label: "无跳转", value: "NONE" },
      { label: "项目详情", value: "PROJECT" },
      { label: "技师详情", value: "TECHNICIAN" },
      { label: "外部链接", value: "URL" },
    ],
    linkTypeIndex: 0,
  },

  onLoad() {
    this.loadBanners();
  },

  async loadBanners() {
    try {
      const banners = await getAdminBanners();
      this.setData({ banners, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleAdd() {
    this.setData({
      showForm: true,
      editingId: null,
      formTitle: "",
      formSort: "0",
      formImageFileId: null,
      formImageUrl: "",
      formLinkType: "NONE",
      formLinkValue: "",
      linkTypeIndex: 0,
    });
  },

  handleEdit(e: WechatMiniprogram.TouchEvent) {
    const banner = e.currentTarget.dataset.item as Banner;
    const lIdx = this.data.linkTypes.findIndex(t => t.value === (banner.linkType || "NONE"));
    const imgUrl = banner.imageFileId ? `${environment.apiBaseUrl}/api/v1/public/files/${banner.imageFileId}` : "";
    this.setData({
      showForm: true,
      editingId: banner.id,
      formTitle: banner.title,
      formSort: String(banner.sort || 0),
      formImageFileId: banner.imageFileId,
      formImageUrl: imgUrl,
      formLinkType: banner.linkType || "NONE",
      formLinkValue: banner.linkValue || "",
      linkTypeIndex: lIdx >= 0 ? lIdx : 0,
    });
  },

  handleCancel() {
    this.setData({ showForm: false });
  },

  handleTitleInput(e: WechatMiniprogram.Input) {
    this.setData({ formTitle: e.detail.value });
  },

  handleSortInput(e: WechatMiniprogram.Input) {
    this.setData({ formSort: e.detail.value });
  },

  handleLinkValueInput(e: WechatMiniprogram.Input) {
    this.setData({ formLinkValue: e.detail.value });
  },

  handleLinkTypeChange(e: any) {
    const idx = Number(e.detail.value);
    const selected = this.data.linkTypes[idx];
    this.setData({ linkTypeIndex: idx, formLinkType: selected.value });
  },

  async handleChooseImage() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      const filePath = res.tempFiles[0].tempFilePath;
      this.setData({ uploading: true });
      wx.showLoading({ title: "上传图片中…" });
      const fileAsset = await uploadPrivateFile(filePath, "BANNER_IMAGE");
      const url = `${environment.apiBaseUrl}/api/v1/public/files/${fileAsset.id}`;
      this.setData({
        formImageFileId: fileAsset.id,
        formImageUrl: url,
      });
      wx.showToast({ title: "上传成功", icon: "success" });
    } catch (err) {
      wx.showToast({ title: "图片上传失败", icon: "none" });
    } finally {
      this.setData({ uploading: false });
      wx.hideLoading();
    }
  },

  async handleSave() {
    const title = this.data.formTitle.trim();
    if (!title) return wx.showToast({ title: "请输入标题", icon: "none" });

    this.setData({ saving: true });
    try {
      const payload = {
        title,
        sort: parseInt(this.data.formSort) || 0,
        imageFileId: this.data.formImageFileId || null,
        linkType: this.data.formLinkType === "NONE" ? null : this.data.formLinkType,
        linkValue: this.data.formLinkValue.trim() || null,
      };

      if (this.data.editingId) {
        await updateBanner(this.data.editingId, payload);
        wx.showToast({ title: "更新成功", icon: "success" });
      } else {
        await createBanner(payload);
        wx.showToast({ title: "创建成功", icon: "success" });
      }
      this.setData({ showForm: false });
      this.loadBanners();
    } catch {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleToggle(e: WechatMiniprogram.TouchEvent) {
    const banner = e.currentTarget.dataset.item as Banner;
    const newStatus = banner.status === "ENABLED" ? "DISABLED" : "ENABLED";
    try {
      await updateBannerStatus(banner.id, newStatus);
      this.loadBanners();
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },
});
