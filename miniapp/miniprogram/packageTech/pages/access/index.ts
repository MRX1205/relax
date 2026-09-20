import { request } from "../../../services/http";
import { getServiceAreas } from "../../services/region";
import { uploadPrivateFile } from "../../../services/file";

interface ApplicationStatus {
  status: string;
  rejectReason: string | null;
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
  },

  async onLoad() {
    try {
      const [app, areas] = await Promise.all([
        request<ApplicationStatus | null>({ url: "/api/v1/technician/application" }).catch(() => null),
        getServiceAreas(),
      ]);
      this.setData({ application: app, serviceAreas: areas, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },

  handleInput(field: string) {
    return (e: WechatMiniprogram.Input) => {
      this.setData({ [field]: e.detail.value });
    };
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

  async choosePhoto() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      this.setData({ photoPath: res.tempFiles[0].tempFilePath });
    } catch {}
  },

  async chooseCertificate() {
    try {
      const res = await wx.chooseMedia({ count: 1, mediaType: ["image"], sizeType: ["compressed"] });
      this.setData({ certificatePath: res.tempFiles[0].tempFilePath });
    } catch {}
  },

  async handleSubmit() {
    const { serviceName, realName, phone, intro, experienceYears, selectedAreas, photoPath, certificatePath } = this.data;
    if (!serviceName.trim()) return wx.showToast({ title: "请输入服务名", icon: "none" });
    if (!realName.trim()) return wx.showToast({ title: "请输入真实姓名", icon: "none" });
    if (!/^1\d{10}$/.test(phone)) return wx.showToast({ title: "请输入正确手机号", icon: "none" });
    if (selectedAreas.length === 0) return wx.showToast({ title: "请选择服务区域", icon: "none" });
    if (!photoPath) return wx.showToast({ title: "请上传工作照", icon: "none" });

    this.setData({ submitting: true });
    try {
      const photoFile = await uploadPrivateFile(photoPath, "TECHNICIAN_PHOTO");
      let certificateFileId: string | undefined;
      if (certificatePath) {
        const certFile = await uploadPrivateFile(certificatePath, "TECHNICIAN_CERTIFICATE");
        certificateFileId = certFile.id;
      }
      await request({
        url: "/api/v1/technician/application",
        method: "POST",
        data: {
          serviceName: serviceName.trim(),
          realName: realName.trim(),
          phone: phone.trim(),
          intro: intro.trim(),
          experienceYears: parseInt(experienceYears) || 0,
          serviceAreaCodes: selectedAreas,
          photoFileId: photoFile.id,
          certificateFileId,
        },
      });
      wx.showToast({ title: "提交成功", icon: "success" });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "提交失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
