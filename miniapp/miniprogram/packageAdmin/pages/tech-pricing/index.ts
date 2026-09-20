import { request } from "../../../services/http";

interface Pricing {
  id: string; technicianId: string; technicianName: string;
  projectId: string; projectName: string; overridePrice: number;
  effectivePrice: number; status: string;
}
interface Project { id: string; name: string; basePrice: number; status: string; }

Page({
  data: {
    technicianId: "",
    technicianName: "",
    loading: true,
    pricing: [] as Pricing[],
    projects: [] as Project[],
    showAdd: false,
    selectedProjectId: "",
    overridePrice: "",
    saving: false,
  },

  onLoad(query: Record<string, string>) {
    this.setData({ technicianId: query.technicianId || "", technicianName: decodeURIComponent(query.name || "") });
    wx.setNavigationBarTitle({ title: `${this.data.technicianName} - 定价` });
    this.loadData();
  },

  async loadData() {
    try {
      const [pricing, projects] = await Promise.all([
        request<Pricing[]>({ url: `/api/v1/admin/technicians/${this.data.technicianId}/pricing` }),
        request<Project[]>({ url: "/api/v1/admin/projects" }),
      ]);
      this.setData({ pricing, projects, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  handleShowAdd() {
    this.setData({ showAdd: true, selectedProjectId: "", overridePrice: "" });
  },

  handleProjectChange(e: WechatMiniprogram.PickerChange) {
    const proj = this.data.projects[parseInt(e.detail.value as string)];
    if (proj) {
      this.setData({ selectedProjectId: proj.id, overridePrice: String(proj.basePrice) });
    }
  },

  handlePriceInput(e: WechatMiniprogram.Input) {
    this.setData({ overridePrice: e.detail.value });
  },

  handleCancel() {
    this.setData({ showAdd: false });
  },

  async handleSave() {
    const { technicianId, selectedProjectId, overridePrice } = this.data;
    if (!selectedProjectId) return wx.showToast({ title: "请选择项目", icon: "none" });
    if (!overridePrice || parseFloat(overridePrice) <= 0) return wx.showToast({ title: "请输入有效价格", icon: "none" });
    this.setData({ saving: true });
    try {
      await request({
        url: `/api/v1/admin/technicians/${technicianId}/pricing/${selectedProjectId}`,
        method: "PUT",
        data: { overridePrice: parseFloat(overridePrice) },
      });
      this.setData({ showAdd: false });
      this.loadData();
    } catch (err) {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "删除定价", content: "确认删除该定价规则？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await request({
        url: `/api/v1/admin/technicians/${this.data.technicianId}/pricing/${id}`,
        method: "DELETE",
      });
      this.loadData();
    } catch { wx.showToast({ title: "删除失败", icon: "none" }); }
  },
});
