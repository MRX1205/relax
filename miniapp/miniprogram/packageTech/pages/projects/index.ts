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
    showPlatformModal: false,
    showEditModal: false,

    // Join Platform Form
    selectedPlatformId: "",
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
    const id = e.currentTarget.dataset.id;
    const current = this.data.projects.find(p => String(p.id) === String(id));
    if (!current) return;

    const actionText = current.status === "ENABLED" ? "下架" : "上架";
    try {
      wx.showLoading({ title: `${actionText}中...` });
      const updated = await toggleProjectStatus(Number(current.id));
      wx.hideLoading();
      wx.showToast({ title: `已${actionText}`, icon: "success" });

      const updatedList = this.data.projects.map(p => (String(p.id) === String(id) ? updated : p));
      this.setData({ projects: updatedList });
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || `${actionText}失败`, icon: "none" });
    }
  },

  // === 删除 / 移除项目 ===
  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const project = this.data.projects.find(p => String(p.id) === String(id));
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
      await deleteProject(Number(project.id));
      wx.hideLoading();
      wx.showToast({ title: "已移除", icon: "success" });
      this.setData({
        projects: this.data.projects.filter(p => String(p.id) !== String(id)),
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

  // === 从平台项目库加入弹窗 ===
  openPlatformModal() {
    this.setData({
      showPlatformModal: true,
      selectedPlatformId: "",
      selectedPlatformProject: null,
      joinPrice: "",
    });
  },

  closePlatformModal() {
    this.setData({ showPlatformModal: false, selectedPlatformId: "", selectedPlatformProject: null });
  },

  selectPlatformItem(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id);
    const item = this.data.platformProjects.find(p => String(p.id) === id) || null;
    this.setData({
      selectedPlatformId: id,
      selectedPlatformProject: item,
      joinPrice: item ? String(item.basePrice) : "",
    });
  },

  handleJoinPriceInput(e: WechatMiniprogram.Input) {
    this.setData({ joinPrice: e.detail.value });
  },

  async handleConfirmJoin() {
    const { selectedPlatformProject, selectedPlatformId, joinPrice } = this.data;
    if (!selectedPlatformProject && !selectedPlatformId) {
      wx.showToast({ title: "请先选择平台项目", icon: "none" });
      return;
    }
    const price = parseFloat(joinPrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效接单价格", icon: "none" });
      return;
    }

    const projectId = Number(selectedPlatformProject?.id || selectedPlatformId);
    this.setData({ submitting: true });
    try {
      wx.showLoading({ title: "加入中..." });
      await joinPlatformProject(projectId, price);
      wx.hideLoading();
      wx.showToast({ title: "加入成功", icon: "success" });
      this.setData({ showPlatformModal: false, submitting: false, selectedPlatformId: "", selectedPlatformProject: null });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "加入失败", icon: "none" });
    }
  },

  // === 编辑项目 ===
  openEditModal(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const project = this.data.projects.find(p => String(p.id) === String(id));
    if (!project) return;

    if (project.creatorType === "TECHNICIAN") {
      wx.navigateTo({ url: `/packageTech/pages/project-edit/index?mode=edit&id=${project.id}` });
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

  async handleSaveEdit() {
    if (!this.data.editingProject) return;
    const price = parseFloat(this.data.editPrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效价格", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      wx.showLoading({ title: "保存中..." });
      await updateProject(Number(this.data.editingProject.id), {
        overridePrice: price,
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
});
