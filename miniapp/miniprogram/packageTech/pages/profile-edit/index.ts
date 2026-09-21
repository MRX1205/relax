import {
  getFullProfile,
  updateFullProfile,
  addPhoto,
  deletePhoto,
  TechPhotoItem,
} from "../../services/profile";

const DEFAULT_TAGS = ["实名认证", "持证理疗师", "安心服务", "五年老店", "手法娴熟", "热情周到"];
const AGE_TAGS = ["95后", "90后", "00后", "85后", "80后"];

Page({
  data: {
    loading: true,
    submitting: false,

    // Form fields
    serviceName: "",
    phone: "",
    intro: "",
    experienceYears: 3,
    avatarUrl: "",
    age: 26,
    ageTag: "95后",
    height: 165,
    weight: 50,

    // Location
    baseAddress: "",
    latitude: 0,
    longitude: 0,

    // Tags & Certifications
    allAvailableTags: DEFAULT_TAGS,
    selectedTags: [] as string[],
    customTagInput: "",

    // Photos
    photos: [] as TechPhotoItem[],

    ageTagOptions: AGE_TAGS,
  },

  onLoad() {
    this.loadProfile();
  },

  async onPullDownRefresh() {
    await this.loadProfile();
    wx.stopPullDownRefresh();
  },

  async loadProfile() {
    this.setData({ loading: true });
    try {
      const res = await getFullProfile();
      const p = res.profile;

      let selectedTags: string[] = [];
      if (p.certificationsJson) {
        try {
          selectedTags = JSON.parse(p.certificationsJson);
        } catch {
          selectedTags = [];
        }
      } else {
        selectedTags = ["实名认证", "持证理疗师", "安心服务"];
      }

      this.setData({
        serviceName: p.serviceName || "",
        phone: p.phone || "",
        intro: p.intro || "",
        experienceYears: p.experienceYears || 1,
        avatarUrl: p.avatarUrl || "",
        age: p.age || 25,
        ageTag: p.ageTag || "95后",
        height: p.height || 165,
        weight: p.weight || 50,
        baseAddress: p.baseAddress || "",
        latitude: p.latitude || 0,
        longitude: p.longitude || 0,
        selectedTags,
        photos: res.photos || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
      wx.showToast({ title: "加载资料失败", icon: "none" });
    }
  },

  handleInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field as string;
    this.setData({ [field]: e.detail.value });
  },

  selectAgeTag(e: WechatMiniprogram.TouchEvent) {
    const tag = e.currentTarget.dataset.tag as string;
    this.setData({ ageTag: tag });
  },

  // === 标签切换与添加 ===
  toggleTag(e: WechatMiniprogram.TouchEvent) {
    const tag = e.currentTarget.dataset.tag as string;
    const current = [...this.data.selectedTags];
    const idx = current.indexOf(tag);
    if (idx > -1) {
      current.splice(idx, 1);
    } else {
      current.push(tag);
    }
    this.setData({ selectedTags: current });
  },

  handleTagInput(e: WechatMiniprogram.Input) {
    this.setData({ customTagInput: e.detail.value });
  },

  addCustomTag() {
    const tag = this.data.customTagInput.trim();
    if (!tag) return;
    if (this.data.selectedTags.includes(tag)) {
      wx.showToast({ title: "标签已存在", icon: "none" });
      return;
    }
    const current = [...this.data.selectedTags, tag];
    const allTags = this.data.allAvailableTags.includes(tag)
      ? this.data.allAvailableTags
      : [...this.data.allAvailableTags, tag];

    this.setData({
      selectedTags: current,
      allAvailableTags: allTags,
      customTagInput: "",
    });
  },

  // === 常驻位置选择 (微信地图选点) ===
  chooseLocation() {
    wx.chooseLocation({
      latitude: this.data.latitude || 23.020536,
      longitude: this.data.longitude || 113.751765,
      success: res => {
        this.setData({
          baseAddress: res.name || res.address,
          latitude: res.latitude,
          longitude: res.longitude,
        });
        wx.showToast({ title: "已更新常驻定位", icon: "success" });
      },
      fail: () => {
        // 用户可能取消选点
      },
    });
  },

  viewLocationOnMap() {
    if (!this.data.latitude || !this.data.longitude) {
      wx.showToast({ title: "请先选择常驻位置", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: Number(this.data.latitude),
      longitude: Number(this.data.longitude),
      name: `${this.data.serviceName || "技师"} 常驻位置`,
      address: this.data.baseAddress || "东莞市",
      scale: 16,
    });
  },

  // === 风采相册管理 ===
  previewPhoto(e: WechatMiniprogram.TouchEvent) {
    const url = e.currentTarget.dataset.url as string;
    const urls = this.data.photos.map(p => p.fileUrl).filter(Boolean);
    if (!urls.length) return;
    wx.previewImage({
      current: url,
      urls,
    });
  },

  async handleDeletePhoto(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "删除照片",
        content: "确认从相册中删除该风采照？",
        confirmColor: "#E54D42",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      wx.showLoading({ title: "删除中..." });
      await deletePhoto(id);
      wx.hideLoading();
      wx.showToast({ title: "已删除", icon: "success" });
      this.setData({
        photos: this.data.photos.filter(p => p.id !== id),
      });
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "删除失败", icon: "none" });
    }
  },

  async handleAddPhoto() {
    const res = await new Promise<WechatMiniprogram.ChooseMediaSuccessCallbackResult | null>(r => {
      wx.chooseMedia({
        count: 1,
        mediaType: ["image"],
        sourceType: ["album", "camera"],
        success: s => r(s),
        fail: () => r(null),
      });
    });

    if (!res || !res.tempFiles || res.tempFiles.length === 0) return;
    const tempPath = res.tempFiles[0].tempFilePath;

    try {
      wx.showLoading({ title: "上传保存中..." });
      // 在本地开发或真机模拟中，直接使用图片临时或预设直链存储
      const photoUrl = tempPath;
      const updatedPhotos = await addPhoto({
        fileUrl: photoUrl,
        photoType: "LIFE",
        sort: this.data.photos.length,
      });
      wx.hideLoading();
      wx.showToast({ title: "上传成功", icon: "success" });
      this.setData({ photos: updatedPhotos || [] });
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "上传失败", icon: "none" });
    }
  },

  // === 一键保存所有技师资料 ===
  async handleSaveAll() {
    const {
      serviceName,
      phone,
      intro,
      experienceYears,
      avatarUrl,
      age,
      ageTag,
      height,
      weight,
      baseAddress,
      latitude,
      longitude,
      selectedTags,
    } = this.data;

    if (!serviceName.trim()) {
      wx.showToast({ title: "请填写服务艺名", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      wx.showLoading({ title: "保存中..." });
      await updateFullProfile({
        serviceName: serviceName.trim(),
        phone: phone.trim(),
        intro: intro.trim(),
        experienceYears: Number(experienceYears) || 1,
        avatarUrl: avatarUrl.trim() || undefined,
        age: Number(age) || undefined,
        ageTag: ageTag || undefined,
        height: Number(height) || undefined,
        weight: Number(weight) || undefined,
        baseAddress: baseAddress || undefined,
        latitude: latitude ? Number(latitude) : undefined,
        longitude: longitude ? Number(longitude) : undefined,
        certificationsJson: JSON.stringify(selectedTags),
      });
      wx.hideLoading();
      wx.showToast({ title: "资料保存成功", icon: "success" });
      this.setData({ submitting: false });
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    }
  },
});
