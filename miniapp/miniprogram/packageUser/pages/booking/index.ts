import { previewOrder } from "../../../services/catalog";
import { createOrder } from "../../services/order";
import { createPayment, simulatePayment } from "../../services/order";
import { getAddresses } from "../../services/region";
import { request } from "../../../services/http";

interface TimeSlot {
  time: string;
  display: string;
  available: boolean;
  statusText?: string;
  selected: boolean;
}

Page({
  data: {
    projectId: "",
    technicianId: "",
    projectName: "",
    technicianName: "",
    technicianAvatarUrl: "",
    durationMinutes: 60,
    serviceDate: "",
    startTime: "",
    endTime: "",
    addressId: "",
    addressSummary: "点击选择服务地址",
    addresses: [] as UserAddress[],
    note: "",
    loading: false,
    preview: null as OrderPreview | null,
    error: "",
    today: "",
    submitting: false,
    timeSlots: [] as TimeSlot[],
    loadingSlots: false,
    dates: [] as Array<{ dateStr: string; dayNum: string; monthDay: string; weekday: string; isToday: boolean }>,
    selectedDateIndex: 0,
    travelMode: "transit" as "transit" | "driving",
    couponId: null as string | null,
    couponDesc: "暂不使用优惠券",
    availableCoupons: [] as any[],
    privacyPhone: true,
    quantity: 1,
  },

  togglePrivacyPhone() {
    this.setData({ privacyPhone: !this.data.privacyPhone });
  },

  handleQuantityChange(e: WechatMiniprogram.TouchEvent) {
    const delta = Number(e.currentTarget.dataset.delta);
    const newQty = Math.max(1, Math.min(5, this.data.quantity + delta));
    this.setData({ quantity: newQty, preview: null });
  },

  onLoad(query: Record<string, string>) {
    const dates = this.generateDateItems(7);
    const today = dates[0].dateStr;
    this.setData({
      projectId: query.projectId || "",
      technicianId: query.technicianId || "",
      projectName: decodeURIComponent(query.projectName || ""),
      technicianName: decodeURIComponent(query.technicianName || ""),
      technicianAvatarUrl: decodeURIComponent(query.technicianAvatar || ""),
      durationMinutes: Number(query.duration) || 60,
      serviceDate: today,
      today,
      dates,
      selectedDateIndex: 0,
    });
    this.loadAddresses();
    this.loadTimeSlots(today);
    this.loadCoupons();
  },

  generateDateItems(count: number): Array<{ dateStr: string; dayNum: string; monthDay: string; weekday: string; isToday: boolean }> {
    const dates = [];
    const weekDays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
    const now = new Date();
    for (let i = 0; i < count; i++) {
      const d = new Date(now);
      d.setDate(now.getDate() + i);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      const dateStr = `${y}-${m}-${day}`;
      const isToday = i === 0;
      const weekday = isToday ? "今天" : weekDays[d.getDay()];
      const monthDay = `${d.getMonth() + 1}/${d.getDate()}`;
      dates.push({ dateStr, dayNum: day, monthDay, weekday, isToday });
    }
    return dates;
  },

  handleDateSelect(e: WechatMiniprogram.TouchEvent) {
    const index = Number(e.currentTarget.dataset.index);
    const selected = this.data.dates[index];
    if (!selected) return;
    this.setData({
      selectedDateIndex: index,
      serviceDate: selected.dateStr,
      startTime: "",
      endTime: "",
      preview: null,
    });
    this.loadTimeSlots(selected.dateStr);
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

  async loadCoupons() {
    try {
      const coupons = await request<any[]>({ url: "/api/v1/coupons/mine" });
      const available = (coupons || []).filter((c: any) => c.status === "ACTIVE" && !c.usedAt);
      this.setData({ availableCoupons: available });
    } catch {}
  },

  async loadTimeSlots(date: string) {
    this.setData({ loadingSlots: true, timeSlots: [] });
    try {
      const schedules = await request<any[]>({
        url: `/api/v1/technicians/${this.data.technicianId}/schedules?date=${date}`,
      }).catch(() => []);

      const slots: TimeSlot[] = [];
      const startHour = 8;
      const endHour = 22;

      for (let hour = startHour; hour < endHour; hour++) {
        const time = `${hour.toString().padStart(2, "0")}:00`;

        let available = false;
        if (schedules && schedules.length > 0) {
          for (const schedule of schedules) {
            if (time >= schedule.startTime && time < schedule.endTime && schedule.type !== "OFF") {
              available = true;
              break;
            }
          }
        } else {
          available = true; // 技师无特殊排班时默认可预约
        }

        let statusText = "可约";
        // 过去的时间不可约
        if (date === this.data.today) {
          const nowHour = new Date().getHours();
          if (hour <= nowHour) {
            available = false;
            statusText = "已过";
          }
        }
        if (!available && statusText !== "已过") {
          statusText = "约满";
        }

        slots.push({ time, display: time, available, statusText, selected: false });
      }

      this.setData({ timeSlots: slots, loadingSlots: false });

      // 如果今天全部已过，友好轻提示
      const hasAvailable = slots.some(s => s.available);
      if (!hasAvailable && date === this.data.today) {
        wx.showToast({ title: "今日时段已过，建议预约明日", icon: "none", duration: 2500 });
      }
    } catch {
      const slots: TimeSlot[] = [];
      for (let hour = 8; hour < 22; hour++) {
        const time = `${hour.toString().padStart(2, "0")}:00`;
        slots.push({ time, display: time, available: true, statusText: "可约", selected: false });
      }
      this.setData({ timeSlots: slots, loadingSlots: false });
    }
  },

  handleTimeSelect(e: WechatMiniprogram.TouchEvent) {
    const time = e.currentTarget.dataset.time as string;
    const available = e.currentTarget.dataset.available === true || e.currentTarget.dataset.available === "true";
    if (!available) {
      wx.showToast({ title: "该时间段不可预约", icon: "none" });
      return;
    }

    const slots = this.data.timeSlots.map(s => ({
      ...s,
      selected: s.time === time,
    }));

    // ✅ Fix S4: correct end time calculation using minutes
    const [startHour, startMin] = time.split(":").map(Number);
    const totalMins = startHour * 60 + startMin + this.data.durationMinutes;
    const endHour = Math.floor(totalMins / 60);
    const endMin = totalMins % 60;
    const endTime = `${endHour.toString().padStart(2, "0")}:${endMin.toString().padStart(2, "0")}`;

    this.setData({ timeSlots: slots, startTime: time, endTime, preview: null });
  },

  handleNoteInput(e: WechatMiniprogram.Input) {
    this.setData({ note: e.detail.value });
  },

  handleSelectAddress() {
    if (this.data.addresses.length === 0) {
      wx.navigateTo({ url: "/packageUser/pages/address-edit/index" });
      return;
    }
    const items = this.data.addresses.map(a => `${a.contactName}  ${a.regionName} ${a.detail}`);
    wx.showActionSheet({
      itemList: items,
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

  handleTravelModeChange(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "transit" | "driving";
    this.setData({ travelMode: mode, preview: null });
  },

  handleSelectCoupon() {
    const coupons = this.data.availableCoupons;
    if (coupons.length === 0) {
      wx.showToast({ title: "暂无可用优惠券", icon: "none" });
      return;
    }
    const items = ["不使用优惠券", ...coupons.map((c: any) => `${c.name} 减¥${c.discount}`)];
    wx.showActionSheet({
      itemList: items,
      success: res => {
        if (res.tapIndex === 0) {
          this.setData({ couponId: null, couponDesc: "暂不使用优惠券", preview: null });
        } else {
          const coupon = coupons[res.tapIndex - 1];
          this.setData({
            couponId: coupon.id,
            couponDesc: `${coupon.name} 减¥${coupon.discount}`,
            preview: null,
          });
        }
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
    const { projectId, technicianId, addressId, serviceDate, startTime, note, preview, couponId } = this.data;
    if (!preview) return wx.showToast({ title: "请先计算价格", icon: "none" });
    if (!addressId) return wx.showToast({ title: "请选择服务地址", icon: "none" });

    this.setData({ submitting: true });
    try {
      const orderDetail = await createOrder({
        projectId, technicianId, addressId, serviceDate, startTime,
        note: note.trim() || undefined,
        couponId: couponId || undefined,
      });

      const payment = await createPayment(orderDetail.order.orderNo);

      // ✅ Fix C3: branch on payment channel - Mock vs Real WeChat Pay
      if (payment.channel === "MOCK") {
        await simulatePayment(payment.paymentNo, "SUCCESS");
      } else {
        // Real WeChat Pay
        const params = payment.payParams!;
        await new Promise<void>((resolve, reject) => {
          wx.requestPayment({
            timeStamp: params.timeStamp,
            nonceStr: params.nonceStr,
            package: params.package,
            signType: "RSA",
            paySign: params.paySign,
            success: () => resolve(),
            fail: (err) => reject(new Error(err.errMsg || "支付取消或失败")),
          });
        });
      }

      wx.showToast({ title: "下单成功！", icon: "success" });
      setTimeout(() => {
        wx.redirectTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderDetail.order.orderNo}` });
      }, 1500);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "下单失败";
      // 用户主动取消支付不弹错误
      if (!msg.includes("cancel")) {
        wx.showToast({ title: msg, icon: "none" });
      }
    } finally {
      this.setData({ submitting: false });
    }
  },

  goToAddAddress() {
    wx.navigateTo({ url: "/packageUser/pages/address-edit/index" });
  },
});
