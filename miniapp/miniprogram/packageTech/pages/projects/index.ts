import {
  getMyProjects,
  getPlatformAvailableProjects,
  joinPlatformProject,
  createCustomProject,
  updateProject,
  toggleProjectStatus,
  deleteProject,
  TechProjectItem,
  PlatformProjectItem,
} from "../../services/project";

Page({
  data: {
    loading: true,
    projects: [] as TechProjectItem[],
    platformProjects: [] as PlatformProjectItem[],

    // Modals
    showCustomModal: false,
    showPlatformModal: false,
    showEditModal: false,

    // Custom Project Form
    customForm: {
      name: "",
      categoryId: 101,
      durationMinutes: "60",
      basePrice: "198",
      description: "",
      notice: "请保持室内通风，服务前请勿过饱饮食",
      onShelf: true,
    },

    // Join Platform Form
    selectedPlatformProject: null as PlatformProjectItem | null,
    joinPrice: "",

    // Edit Project Form
    editingProject: null as TechProjectItem | null,
    editPrice: "",
    editName: "",
    editDuration: "",
    editDescription: "",

    submitting: false,
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadData();
  },

  async onPullDownRefresh() {
    await this.loadData();
    wx.stopPullDownRefresh();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [myProjects, platformList] = await Promise.all([
        getMyProjects(),
        getPlatformAvailableProjects().catch(() => [] as PlatformProjectItem[]),
      ]);
      this.setData({
        projects: myProjects || [],
        platformProjects: platformList || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
      wx.showToast({ title: "加载失败", icon: "none" });
    }
  },

  // === 上架 / 下架 切换 ===
  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const current = this.data.projects.find(p => p.id === id);
    if (!current) return;

    const actionText = current.status === "ENABLED" ? "下架" : "上架";
    try {
      wx.showLoading({ title: `${actionText}中...` });
      const updated = await toggleProjectStatus(id);
      wx.hideLoading();
      wx.showToast({ title: `已${actionText}`, icon: "success" });

      const updatedList = this.data.projects.map(p => (p.id === id ? updated : p));
      this.setData({ projects: updatedList });
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || `${actionText}失败`, icon: "none" });
    }
  },

  // === 删除 / 移除项目 ===
  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const project = this.data.projects.find(p => p.id === id);
    if (!project) return;

    const isCustom = project.creatorType === "TECHNICIAN";
    const confirmText = isCustom ? "确认彻底删除该自定义项目？" : "确认从您的接单列表中移除该项目？";

    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "提示",
        content: confirmText,
        confirmColor: "#E54D42",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      wx.showLoading({ title: "移除中..." });
      await deleteProject(id);
      wx.hideLoading();
      wx.showToast({ title: "已移除", icon: "success" });
      this.setData({
        projects: this.data.projects.filter(p => p.id !== id),
      });
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || "移除失败", icon: "none" });
    }
  },

  // === 自主创建项目 ===
  openCustomModal() {
    wx.navigateTo({ url: "/packageTech/pages/project-edit/index?mode=create" });
  },

  closeCustomModal() {
    this.setData({ showCustomModal: false });
  },

  handleCustomInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field as string;
    this.setData({
      [`customForm.${field}`]: e.detail.value,
    });
  },

  handleCustomShelfChange(e: any) {
    this.setData({
      "customForm.onShelf": e.detail.value,
    });
  },

  async handleSaveCustom() {
    const { name, durationMinutes, basePrice, description, notice, onShelf } = this.data.customForm;
    if (!name.trim()) {
      wx.showToast({ title: "请输入项目名称", icon: "none" });
      return;
    }
    const duration = parseInt(durationMinutes, 10);
    if (isNaN(duration) || duration <= 0) {
      wx.showToast({ title: "请输入有效时长", icon: "none" });
      return;
    }
    const price = parseFloat(basePrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效价格", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      wx.showLoading({ title: "创建中..." });
      await createCustomProject({
        categoryId: 101,
        name: name.trim(),
        durationMinutes: duration,
        basePrice: price,
        description: description.trim(),
        notice: notice.trim(),
        onShelf,
      });
      wx.hideLoading();
      wx.showToast({ title: "创建成功", icon: "success" });
      this.setData({ showCustomModal: false, submitting: false });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "创建失败", icon: "none" });
    }
  },

  // === 从平台项目库加入弹窗 ===
  openPlatformModal() {
    this.setData({
      showPlatformModal: true,
      selectedPlatformProject: null,
      joinPrice: "",
    });
  },

  closePlatformModal() {
    this.setData({ showPlatformModal: false });
  },

  selectPlatformItem(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const item = this.data.platformProjects.find(p => p.id === id) || null;
    this.setData({
      selectedPlatformProject: item,
      joinPrice: item ? String(item.basePrice) : "",
    });
  },

  handleJoinPriceInput(e: WechatMiniprogram.Input) {
    this.setData({ joinPrice: e.detail.value });
  },

  async handleConfirmJoin() {
    if (!this.data.selectedPlatformProject) {
      wx.showToast({ title: "请先选择平台项目", icon: "none" });
      return;
    }
    const price = parseFloat(this.data.joinPrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效价格", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      wx.showLoading({ title: "加入中..." });
      await joinPlatformProject(this.data.selectedPlatformProject.id, price);
      wx.hideLoading();
      wx.showToast({ title: "加入成功", icon: "success" });
      this.setData({ showPlatformModal: false, submitting: false });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "加入失败", icon: "none" });
    }
  },

  // === 编辑项目 ===
  openEditModal(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as number;
    const project = this.data.projects.find(p => p.id === id);
    if (!project) return;

    if (project.creatorType === "TECHNICIAN") {
      wx.navigateTo({ url: `/packageTech/pages/project-edit/index?mode=edit&id=${id}` });
      return;
    }

    this.setData({
      showEditModal: true,
      editingProject: project,
      editPrice: String(project.overridePrice || project.basePrice),
      editName: project.projectName,
      editDuration: String(project.durationMinutes),
      editDescription: project.description || "",
    });
  },

  closeEditModal() {
    this.setData({ showEditModal: false, editingProject: null });
  },

  handleEditPriceInput(e: WechatMiniprogram.Input) {
    this.setData({ editPrice: e.detail.value });
  },

  handleEditNameInput(e: WechatMiniprogram.Input) {
    this.setData({ editName: e.detail.value });
  },

  handleEditDurationInput(e: WechatMiniprogram.Input) {
    this.setData({ editDuration: e.detail.value });
  },

  handleEditDescInput(e: WechatMiniprogram.Input) {
    this.setData({ editDescription: e.detail.value });
  },

  async handleSaveEdit() {
    if (!this.data.editingProject) return;
    const price = parseFloat(this.data.editPrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效价格", icon: "none" });
      return;
    }

    const isCustom = this.data.editingProject.creatorType === "TECHNICIAN";
    this.setData({ submitting: true });

    try {
      wx.showLoading({ title: "保存中..." });
      await updateProject(this.data.editingProject.id, {
        overridePrice: price,
        name: isCustom ? this.data.editName.trim() : undefined,
        durationMinutes: isCustom ? parseInt(this.data.editDuration, 10) || undefined : undefined,
        description: isCustom ? this.data.editDescription.trim() : undefined,
      });
      wx.hideLoading();
      wx.showToast({ title: "保存成功", icon: "success" });
      this.setData({ showEditModal: false, editingProject: null, submitting: false });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    }
  },

  noop() {},
});
