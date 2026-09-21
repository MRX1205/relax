import { getServiceAreas } from "../../services/region";

const PREF_KEY = "relax.tech.selectedAreas";

Page({
  data: {
    loading: true,
    areas: [] as ServiceArea[],
    selectedIds: [] as string[],
    saving: false,
  },

  async onLoad() {
    const cached = wx.getStorageSync<string[]>(PREF_KEY) || [];
    try {
      const areas = await getServiceAreas();
      const defaultSelected = cached.length > 0 ? cached : areas.map(a => a.id);
      this.setData({ areas, selectedIds: defaultSelected, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleToggleArea(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as string;
    let selected = [...this.data.selectedIds];
    if (selected.includes(id)) {
      if (selected.length <= 1) {
        wx.showToast({ title: "至少保留一个接单区域", icon: "none" });
        return;
      }
      selected = selected.filter(x => x !== id);
    } else {
      selected.push(id);
    }
    this.setData({ selectedIds: selected });
  },

  handleSelectAll() {
    this.setData({ selectedIds: this.data.areas.map(a => a.id) });
  },

  handleSave() {
    wx.setStorageSync(PREF_KEY, this.data.selectedIds);
    wx.showToast({ title: "接单片区已更新", icon: "success" });
    setTimeout(() => wx.navigateBack(), 1000);
  },
});
