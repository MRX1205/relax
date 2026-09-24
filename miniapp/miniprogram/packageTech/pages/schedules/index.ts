import { request } from "../../../services/http";

export interface ScheduleItem {
  id: string;
  technicianId: number;
  scheduleDate: string;
  startTime: string;
  endTime: string;
  type: string;
  status: string;
}

interface CalendarDay {
  dateStr: string;
  dayName: string;
  shortDate: string;
  dayNum: string;
  isToday: boolean;
  hasSchedule: boolean;
  count: number;
}

const WEEK_NAMES = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];

Page({
  data: {
    loading: true,
    saving: false,
    schedules: [] as ScheduleItem[],
    calendarDays: [] as CalendarDay[],
    selectedDate: "",
    selectedDayInfo: null as CalendarDay | null,
    currentDaySchedules: [] as ScheduleItem[],
    currentDayTotalHours: 0,
    showModal: false,
    editingId: "",
    formStart: "09:00",
    formEnd: "22:00",
    viewMode: "day" as "day" | "list", // "day" or "list"
  },

  onLoad() {
    this.initCalendar();
    this.loadSchedules();
  },

  initCalendar() {
    const days: CalendarDay[] = [];
    const now = new Date();

    for (let i = 0; i < 7; i++) {
      const d = new Date(now.getTime() + i * 24 * 3600 * 1000);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, "0");
      const date = String(d.getDate()).padStart(2, "0");
      const dateStr = `${year}-${month}-${date}`;
      const dayName = i === 0 ? "今天" : (i === 1 ? "明天" : WEEK_NAMES[d.getDay()]);
      const shortDate = `${month}/${date}`;
      const dayNum = String(d.getDate());

      days.push({
        dateStr,
        dayName,
        shortDate,
        dayNum,
        isToday: i === 0,
        hasSchedule: false,
        count: 0,
      });
    }

    const selectedDate = days[0].dateStr;
    this.setData({
      calendarDays: days,
      selectedDate,
      selectedDayInfo: days[0],
    });
  },

  async loadSchedules() {
    try {
      const res = await request<ScheduleItem[]>({ url: "/api/v1/technician/schedules" });
      const schedules = Array.isArray(res) ? res : [];
      this.setData({ schedules, loading: false });
      this.updateCalendarBadges();
      this.updateDaySchedules();
    } catch {
      this.setData({ loading: false });
    }
  },

  updateCalendarBadges() {
    const { calendarDays, schedules } = this.data;
    const updated = calendarDays.map(day => {
      const daySchedules = schedules.filter(s => s.scheduleDate === day.dateStr);
      return {
        ...day,
        hasSchedule: daySchedules.length > 0,
        count: daySchedules.length,
      };
    });
    const selectedDayInfo = updated.find(d => d.dateStr === this.data.selectedDate) || updated[0];
    this.setData({ calendarDays: updated, selectedDayInfo });
  },

  updateDaySchedules() {
    const { schedules, selectedDate } = this.data;
    const daySchedules = schedules.filter(s => s.scheduleDate === selectedDate);

    // Calculate total hours
    let totalMinutes = 0;
    daySchedules.forEach(s => {
      const [sh, sm] = (s.startTime || "00:00").split(":").map(Number);
      const [eh, em] = (s.endTime || "00:00").split(":").map(Number);
      const diff = (eh * 60 + em) - (sh * 60 + sm);
      if (diff > 0) totalMinutes += diff;
    });

    const totalHours = Math.round((totalMinutes / 60) * 10) / 10;
    this.setData({
      currentDaySchedules: daySchedules,
      currentDayTotalHours: totalHours,
    });
  },

  handleSelectDate(e: WechatMiniprogram.TouchEvent) {
    const dateStr = e.currentTarget.dataset.date as string;
    const selectedDayInfo = this.data.calendarDays.find(d => d.dateStr === dateStr) || null;
    this.setData({ selectedDate: dateStr, selectedDayInfo }, () => {
      this.updateDaySchedules();
    });
  },

  switchViewMode(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "day" | "list";
    this.setData({ viewMode: mode });
  },

  // Apply Quick Shift Presets
  async applyPreset(e: WechatMiniprogram.TouchEvent) {
    const preset = e.currentTarget.dataset.preset as "full" | "morning" | "evening" | "rest";
    const { selectedDate, currentDaySchedules, saving } = this.data;
    if (saving) return;

    if (preset === "rest") {
      if (currentDaySchedules.length === 0) {
        wx.showToast({ title: "今日已是休息状态", icon: "none" });
        return;
      }
      const confirm = await new Promise<boolean>(r => {
        wx.showModal({
          title: "设置休息",
          content: `确认将 ${selectedDate} 设为休息并清除当天排班？`,
          success: res => r(res.confirm),
        });
      });
      if (!confirm) return;

      this.setData({ saving: true });
      try {
        for (const s of currentDaySchedules) {
          await request({ url: `/api/v1/technician/schedules/${s.id}`, method: "DELETE" });
        }
        wx.showToast({ title: "已设为今日休息", icon: "success" });
        await this.loadSchedules();
      } catch (err: any) {
        wx.showToast({ title: err?.message || "操作失败", icon: "none" });
      } finally {
        this.setData({ saving: false });
      }
      return;
    }

    let startTime = "09:00";
    let endTime = "22:00";
    if (preset === "morning") {
      startTime = "09:00";
      endTime = "15:00";
    } else if (preset === "evening") {
      startTime = "15:00";
      endTime = "22:00";
    }

    this.setData({ saving: true });
    try {
      // Clear existing for this date first if any
      for (const s of currentDaySchedules) {
        await request({ url: `/api/v1/technician/schedules/${s.id}`, method: "DELETE" });
      }
      // Create new preset slot
      await request({
        url: "/api/v1/technician/schedules",
        method: "POST",
        data: {
          scheduleDate: selectedDate,
          startTime,
          endTime,
          type: "AVAILABLE",
        },
      });
      wx.showToast({ title: "排班设置成功", icon: "success" });
      await this.loadSchedules();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "保存排班失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  openCustomModal() {
    this.setData({
      showModal: true,
      editingId: "",
      formStart: "09:00",
      formEnd: "22:00",
    });
  },

  closeCustomModal() {
    this.setData({ showModal: false, editingId: "" });
  },

  handleStartChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ formStart: e.detail.value as string });
  },

  handleEndChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ formEnd: e.detail.value as string });
  },

  async handleSaveCustom() {
    const { selectedDate, formStart, formEnd, editingId, saving } = this.data;
    if (saving) return;

    if (!formStart || !formEnd) {
      wx.showToast({ title: "请选择时间范围", icon: "none" });
      return;
    }
    if (formStart >= formEnd) {
      wx.showToast({ title: "结束时间须晚于开始时间", icon: "none" });
      return;
    }

    this.setData({ saving: true });
    try {
      if (editingId) {
        await request({
          url: `/api/v1/technician/schedules/${editingId}`,
          method: "PUT",
          data: {
            scheduleDate: selectedDate,
            startTime: formStart,
            endTime: formEnd,
            type: "AVAILABLE",
          },
        });
      } else {
        await request({
          url: "/api/v1/technician/schedules",
          method: "POST",
          data: {
            scheduleDate: selectedDate,
            startTime: formStart,
            endTime: formEnd,
            type: "AVAILABLE",
          },
        });
      }
      this.setData({ showModal: false });
      wx.showToast({ title: "排班已保存", icon: "success" });
      await this.loadSchedules();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "保存排班失败", icon: "none" });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleDeleteSlot(e: WechatMiniprogram.TouchEvent) {
    const id = e.currentTarget.dataset.id as string;
    const confirmed = await new Promise<boolean>(r => {
      wx.showModal({
        title: "删除时段",
        content: "确认删除此时段排班？",
        confirmColor: "#FF3B30",
        success: res => r(res.confirm),
      });
    });
    if (!confirmed) return;

    try {
      await request({ url: `/api/v1/technician/schedules/${id}`, method: "DELETE" });
      wx.showToast({ title: "已删除", icon: "none" });
      await this.loadSchedules();
    } catch (err: any) {
      wx.showToast({ title: err?.message || "删除失败", icon: "none" });
    }
  },
});
