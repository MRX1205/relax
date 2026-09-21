import { request } from "../../../services/http";
import { uploadPrivateFile } from "../../../services/file";

interface Category { id: string; name: string; sort: number; status: string; }
interface Project {
  id: string; categoryId: string; categoryName: string; name: string;
  durationMinutes: number; basePrice: number; description: string;
  notice: string; coverFileId: string | null; status: string; sort: number;
  creatorType?: string; creatorId?: number;
}

Page({
  data: {
    loading: true,
    projects: [] as Project[],
    categories: [] as Category[],
    showForm: false,
    editingId: "",
    selectedCategoryName: "点击选择分类",
    form: {
      categoryId: "", name: "", durationMinutes: "60", basePrice: "",
      description: "", notice: "", sort: "0",
    },
    coverPath: "",
    saving: false,
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    try {
      const [projects, categories] = await Promise.all([
        request<Project[]>({ url: "/api/v1/admin/projects" }),
        request<Category[]>({ url: "/api/v1/categories" }),
      ]);
      this.setData({ projects, categories, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  handleAdd() {
    this.setData({
      showForm: true, editingId: "", coverPath: "",
      selectedCategoryName: "点击选择分类",
      form: { categoryId: "", name: "", durationMinutes: "60", basePrice: "", description: "", notice: "", sort: "0" },
    });
  },

  handleEdit(e: WechatMiniprogram.TouchEvent) {
    const p = e.currentTarget.dataset.item as Project;
    this.setData({
      showForm: true, editingId: p.id, coverPath: "",
      selectedCategoryName: p.categoryName || "已选分类",
      form: {
        categoryId: p.categoryId, name: p.name, durationMinutes: String(p.durationMinutes),
        basePrice: String(p.basePrice), description: p.description, notice: p.notice, sort: String(p.sort),
      },
    });
  },

  handleCancel() { this.setData({ showForm: false }); },

  handleFormInput(field: string) {
    return (e: WechatMiniprogram.Input) => {
      this.setData({ [`form.${field}`]: e.detail.value });
    };
  },

  handleCategoryChange(e: WechatMiniprogram.PickerChange) {
    const cat = this.data.categories[parseInt(e.detail.value as string)];
    if (cat) this.setData({ "form.categoryId": cat.id, selectedCategoryName: cat.name });
  },

  async chooseCover() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      this.setData({ coverPath: res.tempFiles[0].tempFilePath });
    } catch {}
  },

  async handleSave() {
    const { editingId, form, coverPath } = this.data;
    if (!form.categoryId) return wx.showToast({ title: "请选择分类", icon: "none" });
    if (!form.name.trim()) return wx.showToast({ title: "请输入项目名称", icon: "none" });
    if (!form.basePrice) return wx.showToast({ title: "请输入基础价格", icon: "none" });

    this.setData({ saving: true });
    try {
      let coverFileId: string | undefined;
      if (coverPath) {
        const file = await uploadPrivateFile(coverPath, "PROJECT_COVER");
        coverFileId = file.id;
      }
      const payload = {
        categoryId: form.categoryId, name: form.name.trim(),
        durationMinutes: parseInt(form.durationMinutes) || 60,
        basePrice: parseFloat(form.basePrice), description: form.description.trim(),
        notice: form.notice.trim(), sort: parseInt(form.sort) || 0,
        coverFileId: coverFileId || (editingId ? this.data.projects.find(p => p.id === editingId)?.coverFileId : null),
      };
      if (editingId) {
        await request({ url: `/api/v1/admin/projects/${editingId}`, method: "PUT", data: payload });
      } else {
        await request({ url: "/api/v1/admin/projects", method: "POST", data: payload });
      }
      this.setData({ showForm: false });
      this.loadData();
    } catch (err) {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const p = e.currentTarget.dataset.item as Project;
    const newStatus = p.status === "ON_SHELF" ? "OFF_SHELF" : "ON_SHELF";
    try {
      await request({ url: `/api/v1/admin/projects/${p.id}/status`, method: "PUT", data: { status: newStatus } });
      this.onLoad();
    } catch { wx.showToast({ title: "操作失败", icon: "none" }); }
  },
});
