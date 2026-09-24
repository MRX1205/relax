import { request } from "../../../services/http";
import { uploadDirectFile } from "../../../services/file";
import { getProjectCover } from "../../../utils/assets";

interface Category {
  id: string;
  name: string;
  sort: number;
  status: string;
}

interface Project {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  durationMinutes: number;
  basePrice: number;
  description: string;
  notice: string;
  coverFileId: string | null;
  coverUrl?: string;
  displayCover?: string;
  status: string;
  sort: number;
  creatorType?: string;
  creatorId?: number;
}

Page({
  data: {
    loading: true,
    projects: [] as Project[],
    filteredProjects: [] as Project[],
    categories: [] as Category[],
    selectedCategoryId: "ALL",
    searchKeyword: "",

    // 模态弹窗表单
    showModal: false,
    isEditing: false,
    editingId: "",
    selectedCategoryName: "点击选择分类",
    form: {
      categoryId: "",
      name: "",
      durationMinutes: "60",
      basePrice: "",
      description: "",
      notice: "",
      sort: "0",
    },
    coverPath: "",
    saving: false,
  },

  async onLoad() {
    await this.loadData();
  },

  async onPullDownRefresh() {
    await this.loadData();
    wx.stopPullDownRefresh();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [projectsRes, categoriesRes] = await Promise.all([
        request<Project[]>({ url: "/api/v1/admin/projects" }),
        request<Category[]>({ url: "/api/v1/categories" }),
      ]);

      const projects = (projectsRes || []).map(p => ({
        ...p,
        displayCover: getProjectCover(p.name, p.categoryName, p.coverFileId, p.coverUrl),
      }));

      this.setData({
        projects,
        categories: categoriesRes || [],
        loading: false,
      });
      this.filterProjects();
    } catch {
      this.setData({ loading: false });
      wx.showToast({ title: "加载项目数据失败", icon: "none" });
    }
  },

  handleSearchInput(e: WechatMiniprogram.Input) {
    this.setData({ searchKeyword: e.detail.value });
    this.filterProjects();
  },

  clearSearch() {
    this.setData({ searchKeyword: "" });
    this.filterProjects();
  },

  handleCategorySelect(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    this.setData({ selectedCategoryId: id });
    this.filterProjects();
  },

  filterProjects() {
    const { projects, selectedCategoryId, searchKeyword } = this.data;
    const kw = (searchKeyword || "").trim().toLowerCase();

    let list = projects;
    if (selectedCategoryId !== "ALL") {
      list = list.filter(p => String(p.categoryId) === String(selectedCategoryId));
    }
    if (kw) {
      list = list.filter(p =>
        (p.name && p.name.toLowerCase().includes(kw)) ||
        (p.categoryName && p.categoryName.toLowerCase().includes(kw))
      );
    }

    this.setData({ filteredProjects: list });
  },

  // ════════ 打开新增弹窗 ════════
  openAddModal() {
    this.setData({
      showModal: true,
      isEditing: false,
      editingId: "",
      coverPath: "",
      selectedCategoryName: "点击选择分类",
      form: {
        categoryId: "",
        name: "",
        durationMinutes: "60",
        basePrice: "",
        description: "",
        notice: "",
        sort: "0",
      },
    });
  },

  // ════════ 打开编辑弹窗 ════════
  openEditModal(e: WechatMiniprogram.TouchEvent) {
    const p = e.currentTarget.dataset.item as Project;
    if (!p) return;

    this.setData({
      showModal: true,
      isEditing: true,
      editingId: p.id,
      coverPath: p.displayCover || "",
      selectedCategoryName: p.categoryName || "已选分类",
      form: {
        categoryId: String(p.categoryId),
        name: p.name,
        durationMinutes: String(p.durationMinutes || 60),
        basePrice: String(p.basePrice || ""),
        description: p.description || "",
        notice: p.notice || "",
        sort: String(p.sort || 0),
      },
    });
  },

  closeModal() {
    this.setData({ showModal: false });
  },

  handleFormInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field;
    if (field) {
      this.setData({ [`form.${field}`]: e.detail.value });
    }
  },

  handleCategoryPicker(e: WechatMiniprogram.PickerChange) {
    const idx = parseInt(e.detail.value as string);
    const cat = this.data.categories[idx];
    if (cat) {
      this.setData({
        "form.categoryId": String(cat.id),
        selectedCategoryName: cat.name,
      });
    }
  },

  async chooseCover() {
    try {
      const res = await wx.chooseMedia({
        count: 1,
        mediaType: ["image"],
        sizeType: ["compressed"],
      });
      if (res && res.tempFiles && res.tempFiles.length > 0) {
        this.setData({ coverPath: res.tempFiles[0].tempFilePath });
      }
    } catch {}
  },

  async handleSave() {
    const { isEditing, editingId, form, coverPath } = this.data;
    if (!form.categoryId) {
      return wx.showToast({ title: "请选择所属分类", icon: "none" });
    }
    if (!form.name.trim()) {
      return wx.showToast({ title: "请输入项目名称", icon: "none" });
    }
    if (!form.basePrice || parseFloat(form.basePrice) <= 0) {
      return wx.showToast({ title: "请输入有效的基础价格", icon: "none" });
    }

    this.setData({ saving: true });
    try {
      let coverFileId: string | null = null;
      if (coverPath && (coverPath.startsWith("http://tmp") || coverPath.startsWith("wxfile://"))) {
        wx.showLoading({ title: "上传封面中..." });
        const file = await uploadDirectFile(coverPath, "PROJECT_COVER");
        coverFileId = String(file.id);
        wx.hideLoading();
      }

      const existingProject = isEditing ? this.data.projects.find(p => p.id === editingId) : null;
      const finalCoverId = coverFileId || (existingProject ? existingProject.coverFileId : null);

      const payload = {
        categoryId: parseInt(form.categoryId),
        name: form.name.trim(),
        durationMinutes: parseInt(form.durationMinutes) || 60,
        basePrice: parseFloat(form.basePrice),
        description: form.description.trim(),
        notice: form.notice.trim(),
        sort: parseInt(form.sort) || 0,
        coverFileId: finalCoverId ? parseInt(String(finalCoverId)) : null,
      };

      if (isEditing) {
        await request({
          url: `/api/v1/admin/projects/${editingId}`,
          method: "PUT",
          data: payload,
        });
        wx.showToast({ title: "修改成功", icon: "success" });
      } else {
        await request({
          url: "/api/v1/admin/projects",
          method: "POST",
          data: payload,
        });
        wx.showToast({ title: "添加成功", icon: "success" });
      }

      this.setData({ showModal: false });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const p = e.currentTarget.dataset.item as Project;
    if (!p) return;
    const newStatus = p.status === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF";
    const statusTxt = newStatus === "ON_SHELF" ? "上架" : "下架";

    try {
      wx.showLoading({ title: "更新状态..." });
      await request({
        url: `/api/v1/admin/projects/${p.id}/status`,
        method: "PUT",
        data: { status: newStatus },
      });
      wx.hideLoading();
      wx.showToast({ title: `已${statusTxt}`, icon: "success" });

      const updated = this.data.projects.map(item =>
        item.id === p.id ? { ...item, status: newStatus } : item
      );
      this.setData({ projects: updated });
      this.filterProjects();
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const p = e.currentTarget.dataset.item as Project;
    if (!p) return;

    const confirmed = await new Promise<boolean>(resolve => {
      wx.showModal({
        title: "确认删除",
        content: `确定要删除服务项目「${p.name}」吗？已发生的历史订单不受影响。`,
        confirmColor: "#FF382E",
        success: res => resolve(res.confirm),
      });
    });

    if (!confirmed) return;

    try {
      wx.showLoading({ title: "正在删除..." });
      await request({
        url: `/api/v1/admin/projects/${p.id}`,
        method: "DELETE",
      });
      wx.hideLoading();
      wx.showToast({ title: "已删除", icon: "success" });

      const updated = this.data.projects.filter(item => item.id !== p.id);
      this.setData({ projects: updated });
      this.filterProjects();
    } catch (err: any) {
      wx.hideLoading();
      wx.showToast({ title: err?.message || "删除失败", icon: "none" });
    }
  },

  noop() {},
});
