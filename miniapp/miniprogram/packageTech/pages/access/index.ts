import { request } from "../../../services/http";
import { getServiceAreas } from "../../services/region";
import { uploadPrivateFile } from "../../../services/file";

interface ApplicationStatus {
  status: string;
  rejectReason: string | null;
  serviceName?: string;
  phone?: string;
  createdAt?: string;
}

Page({
  data: {
    loading: true,
    application: null as ApplicationStatus | null,
    serviceName: "",
    realName: "",
    phone: "",
    intro: "",
    experienceYears: "",
    selectedAreas: [] as string[],
    serviceAreas: [] as ServiceArea[],
    photoPath: "",
    certificatePath: "",
    submitting: false,
    agreed: true,
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [app, areas] = await Promise.all([
        request<ApplicationStatus | null>({ url: "/api/v1/technician/application" }).catch(() => null),
        getServiceAreas().catch(() => []),
      ]);
      this.setData({
        application: app,
        serviceAreas: areas || [],
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleInput(e: WechatMiniprogram.Input) {
    const field = e.currentTarget.dataset.field;
    if (!field) return;
    this.setData({ [field]: e.detail.value });
  },

  toggleAgreement() {
    this.setData({ agreed: !this.data.agreed });
  },

  toggleArea(e: WechatMiniprogram.TouchEvent) {
    const code = e.currentTarget.dataset.code as string;
    const areas = [...this.data.selectedAreas];
    const idx = areas.indexOf(code);
    if (idx >= 0) {
      areas.splice(idx, 1);
    } else {
      areas.push(code);
    }
    this.setData({ selectedAreas: areas });
  },

  selectAllAreas() {
    const allCodes = this.data.serviceAreas.map(a => a.regionCode);
    this.setData({ selectedAreas: allCodes });
  },

  clearAreas() {
    this.setData({ selectedAreas: [] });
  },

  async choosePhoto() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      if (res.tempFiles && res.tempFiles.length > 0) {
        this.setData({ photoPath: res.tempFiles[0].tempFilePath });
      }
    } catch {}
  },

  removePhoto() {
    this.setData({ photoPath: "" });
  },

  async chooseCertificate() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      if (res.tempFiles && res.tempFiles.length > 0) {
        this.setData({ certificatePath: res.tempFiles[0].tempFilePath });
      }
    } catch {}
  },

  removeCertificate() {
    this.setData({ certificatePath: "" });
  },

  reApply() {
    this.setData({ application: null });
  },

  async handleSubmit() {
    if (!this.data.agreed) {
      wx.showToast({ title: "请阅读并同意技师入驻服务协议", icon: "none" });
      return;
    }

    const { serviceName, realName, phone, intro, experienceYears, selectedAreas, photoPath, certificatePath } = this.data;

    const sName = (serviceName || "").trim();
    if (!sName) {
      wx.showToast({ title: "请输入服务艺名", icon: "none" });
      return;
    }
    const rName = (realName || "").trim();
    if (!rName) {
      wx.showToast({ title: "请输入真实姓名", icon: "none" });
      return;
    }
    const p = (phone || "").trim();
    if (!/^1\d{10}$/.test(p)) {
      wx.showToast({ title: "请输入11位手机号", icon: "none" });
      return;
    }
    if (selectedAreas.length === 0) {
      wx.showToast({ title: "请至少勾选一个意向服务区域", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: "正在提交资料…" });

    try {
      let photoFileId: string | number | undefined;
      if (photoPath) {
        try {
          const photoFile = await uploadPrivateFile(photoPath, "TECHNICIAN_PHOTO");
          photoFileId = photoFile?.id;
        } catch {
          // 如果模拟或上传失败，降级不阻断
        }
      }

      let certificateFileId: string | number | undefined;
      if (certificatePath) {
        try {
          const certFile = await uploadPrivateFile(certificatePath, "TECHNICIAN_CERTIFICATE");
          certificateFileId = certFile?.id;
        } catch {
          // 降级
        }
      }

      await request({
        url: "/api/v1/technician/application",
        method: "POST",
        data: {
          serviceName: sName,
          realName: rName,
          phone: p,
          intro: (intro || "").trim(),
          experienceYears: parseInt(experienceYears) || 1,
          serviceAreaCodes: selectedAreas,
          photoFileId,
          certificateFileId,
        },
      });

      wx.hideLoading();
      wx.showToast({ title: "申请已提交，等待审核", icon: "success" });
      await this.loadData();
    } catch (err: any) {
      wx.hideLoading();
      this.setData({ submitting: false });
      const msg = err?.message || err?.errMsg || "提交失败，请重试";
      wx.showModal({ title: "提交提示", content: msg, showCancel: false });
    }
  },
});
