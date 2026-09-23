import { getAddresses, createAddress, updateAddress } from "../../services/region";
import { getServiceAreas } from "../../services/region";

Page({
  data: {
    isEdit: false,
    addressId: "",
    contactName: "",
    contactPhone: "",
    regionCode: "",
    regionName: "",
    detail: "",
    longitude: 0,
    latitude: 0,
    label: "",
    isDefault: false,
    saving: false,
    serviceAreas: [] as ServiceArea[],
    areaNames: [] as string[],
    selectedAreaIndex: 0,
  },

  async onLoad(query: Record<string, string>) {
    try {
      const areas = await getServiceAreas();
      const areaNames = (areas || []).map(a => a.name);
      this.setData({ serviceAreas: areas, areaNames });
      if (areas.length > 0 && !this.data.regionCode) {
        // 默认优先选中东莞核心街道（如南城或首个区域）
        const defaultIdx = areas.findIndex(a => a.name.includes("南城") || a.name.includes("东城"));
        const idx = defaultIdx >= 0 ? defaultIdx : 0;
        const defaultArea = areas[idx];
        if (defaultArea) {
          this.setData({
            selectedAreaIndex: idx,
            regionCode: defaultArea.regionCode,
            regionName: defaultArea.name,
          });
        }
      }
    } catch {
      // ignore
    }

    if (query.id) {
      wx.setNavigationBarTitle({ title: "编辑服务地址" });
      this.setData({ isEdit: true, addressId: query.id });
      try {
        const addresses = await getAddresses();
        const addr = addresses.find(a => String(a.id) === query.id);
        if (addr) {
          this.setData({
            contactName: addr.contactName,
            contactPhone: addr.contactPhone,
            regionCode: addr.regionCode,
            regionName: addr.regionName,
            detail: addr.detail,
            longitude: addr.longitude,
            latitude: addr.latitude,
            label: addr.label || "",
            isDefault: addr.isDefault,
          });
        }
      } catch {
        wx.showToast({ title: "加载地址失败", icon: "none" });
      }
    } else {
      wx.setNavigationBarTitle({ title: "新增服务地址" });
    }
  },

  handleFieldInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field as string;
    if (field) {
      this.setData({ [field]: e.detail.value });
    }
  },

  handleRegionPickerChange(e: WechatMiniprogram.PickerChange) {
    const idx = Number(e.detail.value);
    const area = this.data.serviceAreas[idx];
    if (area) {
      this.setData({
        selectedAreaIndex: idx,
        regionCode: area.regionCode,
        regionName: area.name,
      });
    }
  },

  handlePickLocation() {
    wx.chooseLocation({
      success: res => {
        this.setData({
          longitude: Number(res.longitude.toFixed(6)),
          latitude: Number(res.latitude.toFixed(6)),
          detail: this.data.detail || res.address || res.name || "",
        });
      },
      fail: () => {
        // 用户未授权或取消，不阻断
      },
    });
  },

  handleDefaultChange(e: WechatMiniprogram.SwitchChange) {
    this.setData({ isDefault: e.detail.value });
  },

  async handleSave() {
    const { contactName, contactPhone, regionCode, detail, label, isDefault } = this.data;
    if (!contactName.trim()) {
      return wx.showToast({ title: "请填写联系人姓名", icon: "none" });
    }
    if (!contactPhone.trim() || !/^1\d{10}$/.test(contactPhone.trim())) {
      return wx.showToast({ title: "请输入正确的11位手机号", icon: "none" });
    }
    if (!regionCode) {
      return wx.showToast({ title: "请选择所属区域", icon: "none" });
    }
    if (!detail.trim()) {
      return wx.showToast({ title: "请填写详细门牌地址", icon: "none" });
    }

    // 若未在地图上选点，默认以东莞市中心标准坐标保底，避免用户无法下单
    const finalLng = this.data.longitude || 113.7517;
    const finalLat = this.data.latitude || 23.0206;

    this.setData({ saving: true });
    const payload: AddressInput = {
      contactName: contactName.trim(),
      contactPhone: contactPhone.trim(),
      regionCode,
      detail: detail.trim(),
      longitude: finalLng,
      latitude: finalLat,
      label: label ? label.trim() : "",
      isDefault,
    };

    try {
      if (this.data.isEdit) {
        await updateAddress(this.data.addressId, payload);
      } else {
        const created = await createAddress(payload);
        if (created?.id) {
          wx.setStorageSync("last_used_address_id", String(created.id));
        }
      }
      wx.showToast({ title: "地址已保存", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    } catch (err: any) {
      wx.showToast({ title: err?.message || "保存地址失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
