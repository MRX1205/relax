import { request } from "../../../services/http";

interface Category {
  id: string;
  name: string;
  sort: number;
  status: string;
}

Page({
  data: {
    loading: true,
    categories: [] as Category[],
    showForm: false,
    editingId: "",
    formName: "",
    formSort: "0",
    saving: false,
  },

  onLoad() {
    this.loadCategories();
  },

  async loadCategories() {
    try {
      const categories = await request<Category[]>({ url: "/api/v1/categories" });
      this.setData({ categories, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleAdd() {
    this.setData({ showForm: true, editingId: "", formName: "", formSort: "0" });
  },

  handleEdit(e: WechatMiniprogram.TouchEvent) {
    const cat = e.currentTarget.dataset.item as Category;
    this.setData({ showForm: true, editingId: cat.id, formName: cat.name, formSort: String(cat.sort) });
  },

  handleCancel() {
    this.setData({ showForm: false });
  },

  handleNameInput(e: WechatMiniprogram.Input) {
    this.setData({ formName: e.detail.value });
  },

  handleSortInput(e: WechatMiniprogram.Input) {
    this.setData({ formSort: e.detail.value });
  },

  async handleSave() {
    const { editingId, formName, formSort } = this.data;
    if (!formName.trim()) return wx.showToast({ title: "请输入分类名称", icon: "none" });
    this.setData({ saving: true });
    try {
      if (editingId) {
        await request({
          url: `/api/v1/admin/categories/${editingId}`,
          method: "PUT",
          data: { name: formName.trim(), sort: parseInt(formSort) || 0 },
        });
      } else {
        await request({
          url: "/api/v1/admin/categories",
          method: "POST",
          data: { name: formName.trim(), sort: parseInt(formSort) || 0 },
        });
      }
      this.setData({ showForm: false });
      this.loadCategories();
    } catch (err) {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleToggleStatus(e: WechatMiniprogram.TouchEvent) {
    const cat = e.currentTarget.dataset.item as Category;
    const newStatus = cat.status === "ENABLED" ? "DISABLED" : "ENABLED";
    try {
      await request({
        url: `/api/v1/admin/categories/${cat.id}/status`,
        method: "PUT",
        data: { status: newStatus },
      });
      this.loadCategories();
    } catch {
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },
});
