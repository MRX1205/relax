import { getServiceAreas, getTechnicianServiceAreas, updateTechnicianServiceAreas } from "../../services/region";

const ZONE_MAP: Record<string, string[]> = {
  "中心城区": ["东城街道", "南城街道", "万江街道", "莞城街道", "石碣镇", "高埗镇"],
  "松山湖片区": ["大朗镇", "大岭山镇", "寮步镇", "茶山镇", "石排镇"],
  "滨海湾片区": ["长安镇", "虎门镇", "厚街镇", "沙田镇"],
  "东南临深": ["塘厦镇", "凤岗镇", "清溪镇", "樟木头镇"],
  "水乡新城": ["麻涌镇", "中堂镇", "望牛墩镇", "洪梅镇", "道滘镇"],
  "东部产业": ["常平镇", "黄江镇", "谢岗镇", "桥头镇", "企石镇", "横沥镇", "东坑镇"],
};

function getZoneForName(name: string): string {
  for (const [zone, list] of Object.entries(ZONE_MAP)) {
    if (list.some(n => name.includes(n) || n.includes(name))) {
      return zone;
    }
  }
  return "其他片区";
}

interface AreaItem {
  id: string;
  name: string;
  zone: string;
  status: string;
  selected: boolean;
}

Page({
  data: {
    loading: true,
    saving: false,
    allAreas: [] as AreaItem[],
    displayedAreas: [] as AreaItem[],
    activeTab: "全部",
    tabs: ["全部", "中心城区", "松山湖片区", "滨海湾片区", "东南临深", "水乡新城", "东部产业"],
    selectedCount: 0,
    totalCount: 0,
  },

  async onLoad() {
    await this.fetchData();
  },

  async fetchData() {
    this.setData({ loading: true });
    try {
      const [allList, myAreas] = await Promise.all([
        getServiceAreas().catch(() => []),
        getTechnicianServiceAreas().catch(() => []),
      ]);

      const selectedSet = new Set<string>(myAreas && myAreas.length > 0 ? myAreas : allList.map((a: any) => String(a.id)));

      const allAreas: AreaItem[] = (allList || []).map((a: any) => {
        const idStr = String(a.id);
        return {
          id: idStr,
          name: a.name || `区域 ${idStr}`,
          zone: getZoneForName(a.name || ""),
          status: a.status || "ENABLED",
          selected: selectedSet.has(idStr),
        };
      });

      const selectedCount = allAreas.filter(a => a.selected).length;

      this.setData({
        allAreas,
        selectedCount,
        totalCount: allAreas.length,
        loading: false,
      });
      this.updateDisplayedAreas();
    } catch (err: any) {
      this.setData({ loading: false });
      wx.showToast({ title: err?.message || "加载片区失败", icon: "none" });
    }
  },

  updateDisplayedAreas() {
    const { allAreas, activeTab } = this.data;
    if (activeTab === "全部") {
      this.setData({ displayedAreas: allAreas });
    } else {
      this.setData({
        displayedAreas: allAreas.filter(a => a.zone === activeTab),
      });
    }
  },

  handleTabClick(e: WechatMiniprogram.TouchEvent) {
    const tab = e.currentTarget.dataset.tab as string;
    this.setData({ activeTab: tab }, () => {
      this.updateDisplayedAreas();
    });
  },

  handleToggleArea(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id);
    const { allAreas } = this.data;
    const target = allAreas.find(a => a.id === id);
    if (!target) return;

    if (target.selected) {
      const currentSelectedCount = allAreas.filter(a => a.selected).length;
      if (currentSelectedCount <= 1) {
        wx.showToast({ title: "至少保留一个接单区域", icon: "none" });
        return;
      }
    }

    target.selected = !target.selected;
    const selectedCount = allAreas.filter(a => a.selected).length;

    this.setData({ allAreas, selectedCount });
    this.updateDisplayedAreas();
  },

  handleSelectAllCurrent() {
    const { allAreas, activeTab } = this.data;
    allAreas.forEach(a => {
      if (activeTab === "全部" || a.zone === activeTab) {
        a.selected = true;
      }
    });
    const selectedCount = allAreas.filter(a => a.selected).length;
    this.setData({ allAreas, selectedCount });
    this.updateDisplayedAreas();
    wx.showToast({ title: "已全部开启", icon: "none" });
  },

  handleDeselectCurrent() {
    const { allAreas, activeTab } = this.data;
    const targets = activeTab === "全部" ? allAreas : allAreas.filter(a => a.zone === activeTab);
    const othersSelected = allAreas.filter(a => activeTab !== "全部" && a.zone !== activeTab && a.selected).length;

    if (activeTab === "全部" || othersSelected === 0) {
      wx.showToast({ title: "至少保留一个接单区域", icon: "none" });
      return;
    }

    targets.forEach(a => { a.selected = false; });
    const selectedCount = allAreas.filter(a => a.selected).length;
    this.setData({ allAreas, selectedCount });
    this.updateDisplayedAreas();
    wx.showToast({ title: "已关闭当前片区", icon: "none" });
  },

  async handleSave() {
    const { allAreas, saving } = this.data;
    if (saving) return;

    const selectedIds = allAreas.filter(a => a.selected).map(a => a.id);
    if (selectedIds.length === 0) {
      wx.showToast({ title: "请至少选择一个接单区域", icon: "none" });
      return;
    }

    this.setData({ saving: true });
    try {
      await updateTechnicianServiceAreas(selectedIds);
      wx.showToast({ title: "接单片区已更新", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } catch (err: any) {
      wx.showToast({ title: err?.message || "保存失败，请重试", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
