import { request } from "../../../services/http";

interface Pricing {
  id: string;
  technicianId: string;
  technicianName: string;
  projectId: string;
  projectName: string;
  overridePrice: number;
  effectivePrice: number;
  status: string;
}

interface Project {
  id: string;
  name: string;
  basePrice: number;
  durationMinutes: number;
  status: string;
}

interface TechnicianOption {
  id: string;
  serviceName: string;
  realName: string;
  phone: string;
}

interface MergedProjectPricing {
  projectId: string;
  projectName: string;
  durationMinutes: number;
  basePrice: number;
  hasCustomPrice: boolean;
  overridePrice: number | null;
  effectivePrice: number;
  diffText: string;
  diffType: "higher" | "lower" | "equal";
  pricingId: string | null;
}

Page({
  data: {
    technicianId: "",
    technicianName: "",
    technicians: [] as TechnicianOption[],
    selectedTechIndex: 0,
    loading: true,
    mergedList: [] as MergedProjectPricing[],
    showEditModal: false,
    editingProject: null as MergedProjectPricing | null,
    inputPrice: "",
    priceDiffPreview: "",
    saving: false,
  },

  async onLoad(query: Record<string, string>) {
    const techId = query.technicianId || "";
    const techName = query.name ? decodeURIComponent(query.name) : "";
    this.setData({ technicianId: techId, technicianName: techName });
    await this.initData();
  },

  async initData() {
    this.setData({ loading: true });
    try {
      // 1. 加载技师列表
      const rawTechs = await request<any[]>({ url: "/api/v1/admin/technicians" }).catch(() => []);
      const technicians: TechnicianOption[] = (rawTechs || []).map((t: any) => ({
        id: String(t.id),
        serviceName: t.serviceName || "专业技师",
        realName: t.realName || "",
        phone: t.phone || "",
      }));

      let currentTechId = this.data.technicianId;
      let currentTechName = this.data.technicianName;
      let techIndex = 0;

      if (technicians.length > 0) {
        if (!currentTechId) {
          currentTechId = technicians[0].id;
          currentTechName = technicians[0].serviceName;
        } else {
          const foundIdx = technicians.findIndex((t) => t.id === currentTechId);
          if (foundIdx >= 0) {
            techIndex = foundIdx;
            currentTechName = technicians[foundIdx].serviceName;
          }
        }
      }

      this.setData({
        technicians,
        selectedTechIndex: techIndex,
        technicianId: currentTechId,
        technicianName: currentTechName,
      });

      if (currentTechName) {
        wx.setNavigationBarTitle({ title: `${currentTechName} - 专属定价` });
      }

      await this.loadPricingForTech(currentTechId);
    } catch {
      this.setData({ loading: false });
    }
  },

  async loadPricingForTech(techId: string) {
    if (!techId) {
      this.setData({ loading: false, mergedList: [] });
      return;
    }
    this.setData({ loading: true });
    try {
      const [pricingList, projectsList] = await Promise.all([
        request<Pricing[]>({ url: `/api/v1/admin/technicians/${techId}/pricing` }).catch(() => []),
        request<Project[]>({ url: "/api/v1/admin/projects" }).catch(() => []),
      ]);

      const pricingMap = new Map<string, Pricing>();
      (pricingList || []).forEach((p) => {
        pricingMap.set(String(p.projectId), p);
      });

      const mergedList: MergedProjectPricing[] = (projectsList || []).map((proj) => {
        const custom = pricingMap.get(String(proj.id));
        const hasCustom = !!custom && custom.overridePrice != null;
        const overridePrice = hasCustom ? Number(custom.overridePrice) : null;
        const basePrice = Number(proj.basePrice);
        const effectivePrice = hasCustom ? overridePrice! : basePrice;

        let diffText = "使用统一指导价";
        let diffType: "higher" | "lower" | "equal" = "equal";

        if (hasCustom) {
          const diff = effectivePrice - basePrice;
          if (diff > 0) {
            diffText = `资深溢价 +¥${diff.toFixed(2)}`;
            diffType = "higher";
          } else if (diff < 0) {
            diffText = `特惠让利 -¥${Math.abs(diff).toFixed(2)}`;
            diffType = "lower";
          } else {
            diffText = "与基础指导价持平";
            diffType = "equal";
          }
        }

        return {
          projectId: String(proj.id),
          projectName: proj.name,
          durationMinutes: proj.durationMinutes || 60,
          basePrice,
          hasCustomPrice: hasCustom,
          overridePrice,
          effectivePrice,
          diffText,
          diffType,
          pricingId: custom ? String(custom.id) : null,
        };
      });

      this.setData({ mergedList, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleTechPickerChange(e: WechatMiniprogram.PickerChange) {
    const idx = parseInt(e.detail.value as string);
    const tech = this.data.technicians[idx];
    if (tech) {
      this.setData({
        selectedTechIndex: idx,
        technicianId: tech.id,
        technicianName: tech.serviceName,
      });
      wx.setNavigationBarTitle({ title: `${tech.serviceName} - 专属定价` });
      this.loadPricingForTech(tech.id);
    }
  },

  openEdit(e: WechatMiniprogram.TouchEvent) {
    const item = e.currentTarget.dataset.item as MergedProjectPricing;
    const defaultVal = item.hasCustomPrice ? String(item.overridePrice) : String(item.basePrice);
    this.setData({
      showEditModal: true,
      editingProject: item,
      inputPrice: defaultVal,
    });
    this.calculateDiffPreview(defaultVal, item.basePrice);
  },

  closeEdit() {
    this.setData({ showEditModal: false, editingProject: null });
  },

  handlePriceInput(e: WechatMiniprogram.Input) {
    const val = e.detail.value;
    this.setData({ inputPrice: val });
    if (this.data.editingProject) {
      this.calculateDiffPreview(val, this.data.editingProject.basePrice);
    }
  },

  calculateDiffPreview(inputVal: string, basePrice: number) {
    const num = parseFloat(inputVal);
    if (isNaN(num) || num <= 0) {
      this.setData({ priceDiffPreview: "请输入有效数字金额" });
      return;
    }
    const diff = num - basePrice;
    if (diff > 0) {
      this.setData({ priceDiffPreview: `比平台指导价高 ¥${diff.toFixed(2)} (技师资深溢价)` });
    } else if (diff < 0) {
      this.setData({ priceDiffPreview: `比平台指导价优惠 ¥${Math.abs(diff).toFixed(2)} (促销特价)` });
    } else {
      this.setData({ priceDiffPreview: "与平台基础指导价一致" });
    }
  },

  async handleSavePrice() {
    const { technicianId, editingProject, inputPrice } = this.data;
    if (!editingProject) return;
    const priceNum = parseFloat(inputPrice);
    if (isNaN(priceNum) || priceNum <= 0) {
      wx.showToast({ title: "请输入有效价格", icon: "none" });
      return;
    }

    this.setData({ saving: true });
    try {
      await request({
        url: `/api/v1/admin/technicians/${technicianId}/pricing/${editingProject.projectId}`,
        method: "PUT",
        data: { overridePrice: priceNum },
      });
      wx.showToast({ title: "专属定价设置成功", icon: "success" });
      this.closeEdit();
      this.loadPricingForTech(technicianId);
    } catch {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleResetPrice(e: WechatMiniprogram.TouchEvent) {
    const item = e.currentTarget.dataset.item as MergedProjectPricing;
    const confirmed = await new Promise<boolean>((resolve) => {
      wx.showModal({
        title: "恢复默认指导价",
        content: `确定取消技师在【${item.projectName}】上的专属定价，恢复使用平台基础价 ¥${item.basePrice} 吗？`,
        confirmText: "恢复默认",
        confirmColor: "#FF382E",
        success: (res) => resolve(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      wx.showLoading({ title: "正在恢复..." });
      // If server supports deleting override or setting equal
      if (item.pricingId) {
        await request({
          url: `/api/v1/admin/technicians/pricing/${item.pricingId}`,
          method: "DELETE",
        }).catch(() => {
          // fallback update to base price
          return request({
            url: `/api/v1/admin/technicians/${this.data.technicianId}/pricing/${item.projectId}`,
            method: "PUT",
            data: { overridePrice: item.basePrice },
          });
        });
      }
      wx.hideLoading();
      wx.showToast({ title: "已恢复平台基础价", icon: "success" });
      this.loadPricingForTech(this.data.technicianId);
    } catch {
      wx.hideLoading();
      wx.showToast({ title: "操作失败", icon: "none" });
    }
  },

  noop() {},
});
