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
  },

  async onLoad(query: Record<string, string>) {
    const areas = await getServiceAreas();
    this.setData({ serviceAreas: areas });

    if (query.id) {
      wx.setNavigationBarTitle({ title: "编辑地址" });
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
        wx.showToast({ title: "加载失败", icon: "none" });
      }
    } else {
      wx.setNavigationBarTitle({ title: "新增地址" });
    }
  },

  handleInput(field: string) {
    return (e: WechatMiniprogram.Input) => {
      this.setData({ [field]: e.detail.value });
    };
  },

  handleRegionTap() {
    wx.showActionSheet({
      itemList: this.data.serviceAreas.map(a => a.name),
      success: res => {
        const area = this.data.serviceAreas[res.tapIndex];
        this.setData({ regionCode: area.regionCode, regionName: area.name });
      },
    });
  },

  handlePickLocation() {
    wx.chooseLocation({
      success: res => {
        this.setData({
          longitude: res.longitude,
          latitude: res.latitude,
          detail: this.data.detail || res.address || res.name || "",
        });
      },
    });
  },

  handleDefaultChange(e: WechatMiniprogram.SwitchChange) {
    this.setData({ isDefault: e.detail.value });
  },

  async handleSave() {
    const { contactName, contactPhone, regionCode, detail, longitude, latitude } = this.data;
    if (!contactName.trim()) return wx.showToast({ title: "请输入联系人", icon: "none" });
    if (!/^1\d{10}$/.test(contactPhone)) return wx.showToast({ title: "请输入正确手机号", icon: "none" });
    if (!regionCode) return wx.showToast({ title: "请选择区域", icon: "none" });
    if (!detail.trim()) return wx.showToast({ title: "请输入详细地址", icon: "none" });
    if (!longitude || !latitude) return wx.showToast({ title: "请选择地图位置", icon: "none" });

    this.setData({ saving: true });
    const payload: AddressInput = {
      contactName: contactName.trim(),
      contactPhone: contactPhone.trim(),
      regionCode,
      detail: detail.trim(),
      longitude,
      latitude,
      label: this.data.label.trim(),
      isDefault: this.data.isDefault,
    };
    try {
      if (this.data.isEdit) {
        await updateAddress(this.data.addressId, payload);
      } else {
        await createAddress(payload);
      }
      wx.navigateBack();
    } catch (err) {
      wx.showToast({ title: "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },
});
