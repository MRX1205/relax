import { request } from "../../../services/http";
import {
  createCustomProject,
  updateProject,
  getMyProjects,
  TechProjectItem,
} from "../../services/project";

interface CategoryItem {
  id: number;
  name: string;
}

Page({
  data: {
    isEdit: false,
    projectId: 0,
    categories: [] as CategoryItem[],
    categoryIndex: -1,

    name: "",
    durationMinutes: "60",
    basePrice: "198",
    description: "",
    notice: "请保持室内通风，服务前请勿过饱饮食",
    onShelf: true,

    submitting: false,
  },

  async onLoad(options: { mode?: string; id?: string }) {
    await this.loadCategories();

    if (options.mode === "edit" && options.id) {
      const id = parseInt(options.id, 10);
      this.setData({ isEdit: true, projectId: id });
      wx.setNavigationBarTitle({ title: "修改项目信息" });
      await this.loadProjectDetail(id);
    }
  },

  async loadCategories() {
    try {
      const list = await request<CategoryItem[]>({ url: "/api/v1/categories" });
      if (list && list.length > 0) {
        this.setData({
          categories: list,
          categoryIndex: 0,
        });
      }
    } catch {
      // Default fallback categories
      this.setData({
        categories: [
          { id: 101, name: "中医推拿" },
          { id: 102, name: "精油SPA" },
          { id: 103, name: "足疗保健" },
          { id: 104, name: "运动康复" },
        ],
        categoryIndex: 0,
      });
    }
  },

  async loadProjectDetail(id: number) {
    try {
      wx.showLoading({ title: "加载中..." });
      const projects = await getMyProjects();
      const current = projects.find(p => p.id === id);
      wx.hideLoading();

      if (current) {
        let catIndex = this.data.categories.findIndex(c => c.name === current.categoryName);
        if (catIndex < 0 && this.data.categories.length > 0) catIndex = 0;

        this.setData({
          name: current.projectName,
          durationMinutes: String(current.durationMinutes),
          basePrice: String(current.overridePrice || current.basePrice),
          description: current.description || "",
          notice: current.notice || "请保持室内通风，服务前请勿过饱饮食",
          categoryIndex: catIndex,
        });
      }
    } catch {
      wx.hideLoading();
    }
  },

  handleInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field as string;
    if (field) {
      this.setData({ [field]: e.detail.value } as any);
    }
  },

  handleCategoryChange(e: any) {
    const index = parseInt(e.detail.value, 10);
    this.setData({ categoryIndex: index });
  },

  handleShelfChange(e: any) {
    this.setData({ onShelf: e.detail.value });
  },

  async handleSave() {
    const { name, durationMinutes, basePrice, description, notice, onShelf, categoryIndex, categories, isEdit, projectId } = this.data;

    if (!name.trim()) {
      wx.showToast({ title: "请输入项目名称", icon: "none" });
      return;
    }
    const duration = parseInt(durationMinutes, 10);
    if (isNaN(duration) || duration < 15) {
      wx.showToast({ title: "服务时长需不少于15分钟", icon: "none" });
      return;
    }
    const price = parseFloat(basePrice);
    if (isNaN(price) || price <= 0) {
      wx.showToast({ title: "请输入有效接单价格", icon: "none" });
      return;
    }
    const categoryId = (categoryIndex >= 0 && categories[categoryIndex]) ? Number(categories[categoryIndex].id) : 101;

    this.setData({ submitting: true });
    try {
      if (isEdit) {
        await updateProject(projectId, {
          name: name.trim(),
          categoryId,
          durationMinutes: duration,
          overridePrice: price,
          description: description.trim(),
          notice: notice.trim(),
        });
        wx.showToast({ title: "更新成功", icon: "success" });
      } else {
        await createCustomProject({
          categoryId,
          name: name.trim(),
          durationMinutes: duration,
          basePrice: price,
          description: description.trim(),
          notice: notice.trim(),
          onShelf,
        });
        wx.showToast({ title: "创建成功", icon: "success" });
      }

      setTimeout(() => {
        wx.navigateBack();
      }, 600);
    } catch (err: any) {
      this.setData({ submitting: false });
      wx.showToast({ title: err?.message || "保存失败", icon: "none" });
    }
  },
});
