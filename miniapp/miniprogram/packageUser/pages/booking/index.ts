import { previewOrder } from "../../../services/catalog";
import { createOrder } from "../../services/order";
import { createPayment, simulatePayment } from "../../services/order";
import { getAddresses } from "../../services/region";
import { request } from "../../../services/http";

interface TimeSlot {
  time: string;
  display: string;
  available: boolean;
  selected: boolean;
}

Page({
  data: {
    projectId: "",
    technicianId: "",
    projectName: "",
    technicianName: "",
    durationMinutes: 60,
    serviceDate: "",
    startTime: "",
    endTime: "",
    addressId: "",
    addressSummary: "点击选择地址",
    addresses: [] as UserAddress[],
    note: "",
    loading: false,
    preview: null as OrderPreview | null,
    error: "",
    today: "",
    submitting: false,
    timeSlots: [] as TimeSlot[],
    loadingSlots: false,
    dates: [] as string[],
    selectedDateIndex: 0,
  },

  onLoad(query: Record<string, string>) {
    const today = new Date().toISOString().split("T")[0];
    const dates = this.generateDates(today, 7);
    this.setData({
      projectId: query.projectId || "",
      technicianId: query.technicianId || "",
      projectName: query.projectName || "",
      technicianName: query.technicianName || "",
      durationMinutes: Number(query.duration) || 60,
      serviceDate: dates[0],
      today,
      dates,
    });
    this.loadAddresses();
    this.loadTimeSlots(dates[0]);
  },

  generateDates(startDate: string, count: number): string[] {
    const dates: string[] = [];
    const start = new Date(startDate);
    for (let i = 0; i < count; i++) {
      const date = new Date(start);
      date.setDate(date.getDate() + i);
      dates.push(date.toISOString().split("T")[0]);
    }
    return dates;
  },

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    const weekDays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const weekDay = weekDays[date.getDay()];
    return `${month}/${day} ${weekDay}`;
  },

  isToday(dateStr: string): boolean {
    return dateStr === this.data.today;
  },

  async loadAddresses() {
    try {
      const addresses = await getAddresses();
      this.setData({ addresses });
      const defaultAddr = addresses.find(a => a.isDefault) || addresses[0];
      if (defaultAddr) {
        this.setData({
          addressId: defaultAddr.id,
          addressSummary: `${defaultAddr.contactName} ${defaultAddr.contactPhone}\n${defaultAddr.regionName} ${defaultAddr.detail}`,
        });
      }
    } catch {}
  },

  async loadTimeSlots(date: string) {
    this.setData({ loadingSlots: true, timeSlots: [] });
    try {
      // 获取技师排班
      const schedules = await request<any[]>({
        url: `/api/v1/technicians/${this.data.technicianId}/schedules?date=${date}`,
      }).catch(() => []);

      // 生成时间槽（8:00 - 21:00，每小时一个）
      const slots: TimeSlot[] = [];
      const startHour = 8;
      const endHour = 21;

      for (let hour = startHour; hour < endHour; hour++) {
        const time = `${hour.toString().padStart(2, "0")}:00`;
        const display = `${hour.toString().padStart(2, "0")}:00`;

        // 检查该时间段是否有排班
        let available = false;
        if (schedules && schedules.length > 0) {
          for (const schedule of schedules) {
            const scheduleStart = schedule.startTime;
            const scheduleEnd = schedule.endTime;
            if (time >= scheduleStart && time < scheduleEnd) {
              available = true;
              break;
            }
          }
        } else {
          // 如果没有排班数据，默认全部可用（开发模式）
          available = true;
        }

        slots.push({
          time,
          display,
          available,
          selected: false,
        });
      }

      this.setData({ timeSlots: slots, loadingSlots: false });
    } catch {
      // 如果获取排班失败，生成默认时间槽
      const slots: TimeSlot[] = [];
      for (let hour = 8; hour < 21; hour++) {
        const time = `${hour.toString().padStart(2, "0")}:00`;
        slots.push({
          time,
          display: `${hour.toString().padStart(2, "0")}:00`,
          available: true,
          selected: false,
        });
      }
      this.setData({ timeSlots: slots, loadingSlots: false });
    }
  },

  handleDateSelect(e: WechatMiniprogram.TouchEvent) {
    const index = Number(e.currentTarget.dataset.index);
    const date = this.data.dates[index];
    this.setData({
      selectedDateIndex: index,
      serviceDate: date,
      startTime: "",
      endTime: "",
      preview: null,
    });
    this.loadTimeSlots(date);
  },

  handleTimeSelect(e: WechatMiniprogram.TouchEvent) {
    const time = e.currentTarget.dataset.time;
    const available = e.currentTarget.dataset.available;
    if (!available) {
      wx.showToast({ title: "该时间段不可用", icon: "none" });
      return;
    }

    const slots = this.data.timeSlots.map(s => ({
      ...s,
      selected: s.time === time,
    }));

    // 计算结束时间
    const [hour] = time.split(":").map(Number);
    const endHour = hour + Math.ceil(this.data.durationMinutes / 60);
    const endTime = `${endHour.toString().padStart(2, "0")}:00`;

    this.setData({
      timeSlots: slots,
      startTime: time,
      endTime,
      preview: null,
    });
  },

  handleNoteInput(e: WechatMiniprogram.Input) {
    this.setData({ note: e.detail.value });
  },

  handleSelectAddress() {
    if (this.data.addresses.length === 0) {
      wx.navigateTo({ url: "/packageUser/pages/address-edit/index" });
      return;
    }
    const names = this.data.addresses.map(a => `${a.contactName} ${a.regionName} ${a.detail}`);
    wx.showActionSheet({
      itemList: names,
      success: res => {
        const addr = this.data.addresses[res.tapIndex];
        this.setData({
          addressId: addr.id,
          addressSummary: `${addr.contactName} ${addr.contactPhone}\n${addr.regionName} ${addr.detail}`,
          preview: null,
        });
      },
    });
  },

  async handlePreview() {
    const { projectId, technicianId, addressId, serviceDate, startTime } = this.data;
    if (!projectId || !technicianId) return wx.showToast({ title: "缺少项目或技师信息", icon: "none" });
    if (!serviceDate || !startTime) return wx.showToast({ title: "请选择服务时间", icon: "none" });
    if (!addressId) return wx.showToast({ title: "请选择服务地址", icon: "none" });

    this.setData({ loading: true, error: "", preview: null });
    try {
      const preview = await previewOrder({ projectId, technicianId, addressId, serviceDate, startTime });
      this.setData({ preview, loading: false });
    } catch (err) {
      this.setData({ error: err instanceof Error ? err.message : "预览失败", loading: false });
    }
  },

  async handleSubmit() {
    const { projectId, technicianId, addressId, serviceDate, startTime, note, preview } = this.data;
    if (!preview) return wx.showToast({ title: "请先计算价格", icon: "none" });
    this.setData({ submitting: true });
    try {
      const orderDetail = await createOrder({
        projectId, technicianId, addressId, serviceDate, startTime, note: note.trim() || undefined,
      });
      // 创建支付单并模拟支付
      const payment = await createPayment(orderDetail.order.orderNo);
      await simulatePayment(payment.paymentNo, "SUCCESS");
      wx.showToast({ title: "下单成功", icon: "success" });
      setTimeout(() => {
        wx.redirectTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderDetail.order.orderNo}` });
      }, 1500);
    } catch (err) {
      wx.showToast({ title: err instanceof Error ? err.message : "下单失败", icon: "none" });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
