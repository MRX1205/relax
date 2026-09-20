import { request } from "../../../services/http";

interface Schedule {
  id: string; technicianId: number; scheduleDate: string;
  startTime: string; endTime: string; type: string; status: string;
}

Page({
  data: {
    loading: true,
    schedules: [] as Schedule[],
    showForm: false,
    editingId: "",
    formDate: "",
    formStart: "09:00",
    formEnd: "18:00",
    saving: false,
    today: "",
  },

  onLoad() {
    const today = new Date().toISOString().split("T")[0];
    this.setData({ today, formDate: today });
    this.loadSchedules();
  },

  async loadSchedules() {
    try {
      const schedules = await request<Schedule[]>({ url: "/api/v1/technician/schedules" });
      this.setData({ schedules, loading: false });
    } catch { this.setData({ loading: false }); }
  },

  handleAdd() {
    this.setData({
      showForm: true, editingId: "",
      formDate: this.data.today, formStart: "09:00", formEnd: "18:00",
    });
  },

  handleEdit(e: WechatMiniprogram.TouchEvent) {
    const s = e.currentTarget.dataset.item as Schedule;
    this.setData({
      showForm: true, editingId: s.id,
      formDate: s.scheduleDate, formStart: s.startTime, formEnd: s.endTime,
    });
  },

  handleCancel() { this.setData({ showForm: false }); },

  handleDateChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ formDate: e.detail.value as string });
  },

  handleStartChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ formStart: e.detail.value as string });
  },

  handleEndChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ formEnd: e.detail.value as string });
  },

  async handleSave() {
    const { editingId, formDate, formStart, formEnd } = this.data;
    if (!formDate) return wx.showToast({ title: "请选择日期", icon: "none" });
    if (formStart >= formEnd) return wx.showToast({ title: "结束时间须晚于开始时间", icon: "none" });

    this.setData({ saving: true });
    try {
      if (editingId) {
        await request({
          url: `/api/v1/technician/schedules/${editingId}`,
          method: "PUT",
          data: { scheduleDate: formDate, startTime: formStart, endTime: formEnd, type: "AVAILABLE" },
        });
      } else {
        await request({
          url: "/api/v1/technician/schedules",
          method: "POST",
          data: { scheduleDate: formDate, startTime: formStart, endTime: formEnd, type: "AVAILABLE" },
        });
      }
      this.setData({ showForm: false });
      this.loadSchedules();
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "保存失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({ title: "删除排班", content: "确认删除该排班？", success: res => r(res.confirm) });
    });
    if (!confirmed) return;
    try {
      await request({ url: `/api/v1/technician/schedules/${id}`, method: "DELETE" });
      this.loadSchedules();
    } catch { wx.showToast({ title: "删除失败", icon: "none" }); }
  },
});
