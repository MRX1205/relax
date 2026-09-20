import { getTechnicianDetail } from "../../../services/catalog";

Page({
  data: {
    loading: true,
    detail: null as TechnicianDetail | null,
    techId: "",
    selectedDate: "",
    dateSlots: [] as { date: string; label: string }[],
    timeSlots: [] as { startTime: string; endTime: string }[],
  },

  async onLoad(query: Record<string, string>) {
    this.setData({ techId: query.id || "" });
    try {
      const detail = await getTechnicianDetail(query.id!);
      wx.setNavigationBarTitle({ title: detail.technician.serviceName });
      const dateSlots = this.buildDateSlots(detail.availability);
      this.setData({ detail, dateSlots, loading: false });
      if (dateSlots.length > 0) {
        this.selectDate(dateSlots[0].date);
      }
    } catch {
      this.setData({ loading: false });
    }
  },

  buildDateSlots(availability: { scheduleDate: string }[]) {
    const dates = [...new Set(availability.map(a => a.scheduleDate))].sort();
    return dates.map(d => ({
      date: d,
      label: this.formatDateLabel(d),
    }));
  },

  formatDateLabel(date: string): string {
    const d = new Date(date);
    const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
    return `${d.getMonth() + 1}/${d.getDate()} 周${weekdays[d.getDay()]}`;
  },

  selectDate(date: string) {
    const slots = this.data.detail?.availability
      .filter(a => a.scheduleDate === date)
      .map(a => ({ startTime: a.startTime, endTime: a.endTime })) || [];
    this.setData({ selectedDate: date, timeSlots: slots });
  },

  handleDateTap(e: WechatMiniprogram.TouchEvent) {
    this.selectDate(e.currentTarget.dataset.date);
  },

  handleTimeTap(e: WechatMiniprogram.TouchEvent) {
    const { projectId } = e.currentTarget.dataset;
    if (!projectId) {
      wx.showToast({ title: "请先选择项目", icon: "none" });
      return;
    }
    const startTime = e.currentTarget.dataset.start;
    wx.navigateTo({
      url: `/packageUser/pages/booking/index?projectId=${projectId}&technicianId=${this.data.techId}&date=${this.data.selectedDate}&time=${startTime}`,
    });
  },

  goToBooking(e: WechatMiniprogram.TouchEvent) {
    const projectId = e.currentTarget.dataset.projectid;
    wx.navigateTo({
      url: `/packageUser/pages/booking/index?projectId=${projectId}&technicianId=${this.data.techId}`,
    });
  },
});
