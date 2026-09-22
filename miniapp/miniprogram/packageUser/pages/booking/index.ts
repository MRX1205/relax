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
    estimatedBasePrice: 0,
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

    // 支付模式：MOCK (模拟支付) | WXPAY (微信支付) | OFFLINE (现场支付/免线上付)
    paymentMode: "MOCK" as "MOCK" | "WXPAY" | "OFFLINE",
    paymentModeLabel: "模拟支付",
    paymentModeDesc: "",
  },

  togglePrivacyPhone() {
    this.setData({ privacyPhone: !this.data.privacyPhone });
  },

  handleQuantityChange(e: WechatMiniprogram.TouchEvent) {
    const delta = Number(e.currentTarget.dataset.delta);
    const newQty = Math.max(1, Math.min(5, this.data.quantity + delta));
    this.setData({ quantity: newQty }, () => {
      this.autoCalculatePreview();
    });
  },

  async onLoad(query: Record<string, string>) {
    const dates = this.generateDateItems(7);
    const today = dates[0].dateStr;
    const basePrice = Number(query.price) || 0;

    this.setData({
      projectId: query.projectId || "",
      technicianId: query.technicianId || "",
      projectName: decodeURIComponent(query.projectName || ""),
      technicianName: decodeURIComponent(query.technicianName || ""),
      technicianAvatarUrl: decodeURIComponent(query.technicianAvatar || ""),
      durationMinutes: Number(query.duration) || 60,
      estimatedBasePrice: basePrice,
      serviceDate: today,
      today,
      dates,
      selectedDateIndex: 0,
    });

    await this.loadPaymentMode();
    await this.loadAddresses();
    this.loadTimeSlots(today);
    this.loadCoupons();
  },

  async onShow() {
    // 从地址编辑页或其它页面返回时刷新地址与支付配置
    await this.loadAddresses();
    await this.loadPaymentMode();
  },

  async loadPaymentMode() {
    try {
      const modeInfo = await request<{ mode: "MOCK" | "WXPAY" | "OFFLINE"; label: string; description: string }>({
        url: "/api/v1/payment/mode",
      });
      if (modeInfo?.mode) {
        this.setData({
          paymentMode: modeInfo.mode,
          paymentModeLabel: modeInfo.label,
          paymentModeDesc: modeInfo.description,
        });
      }
    } catch {
      // 默认 MOCK
    }
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

      const lastUsedId = wx.getStorageSync("last_used_address_id");
      let targetAddr = addresses.find(a => String(a.id) === String(lastUsedId));
      if (!targetAddr) {
        targetAddr = addresses.find(a => a.isDefault) || addresses[0];
      }

      if (targetAddr) {
        this.setData({
          addressId: String(targetAddr.id),
          addressSummary: `${targetAddr.contactName} ${targetAddr.contactPhone}\n${targetAddr.regionName} ${targetAddr.detail}`,
        }, () => {
          this.autoCalculatePreview();
        });
      }
    } catch {
      // ignore
    }
  },

  async loadCoupons() {
    try {
      const coupons = await request<any[]>({ url: "/api/v1/coupons/mine" });
      const available = (coupons || []).filter((c: any) => c.status === "ACTIVE" && !c.usedAt);
      this.setData({ availableCoupons: available });
    } catch {
      // ignore
    }
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
          available = true;
        }

        let statusText = "可约";
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

      // 如果有可预约时段且当前未选时段，默认选中第一个可用时段
      const firstAvailable = slots.find(s => s.available);
      if (firstAvailable && !this.data.startTime) {
        this.selectSlot(firstAvailable.time);
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

  selectSlot(time: string) {
    const slots = this.data.timeSlots.map(s => ({
      ...s,
      selected: s.time === time,
    }));

    const [startHour, startMin] = time.split(":").map(Number);
    const totalMins = startHour * 60 + startMin + this.data.durationMinutes;
    const endHour = Math.floor(totalMins / 60);
    const endMin = totalMins % 60;
    const endTime = `${endHour.toString().padStart(2, "0")}:${endMin.toString().padStart(2, "0")}`;

    this.setData({ timeSlots: slots, startTime: time, endTime }, () => {
      this.autoCalculatePreview();
    });
  },

  handleTimeSelect(e: WechatMiniprogram.TouchEvent) {
    const time = e.currentTarget.dataset.time as string;
    const available = e.currentTarget.dataset.available === true || e.currentTarget.dataset.available === "true";
    if (!available) {
      wx.showToast({ title: "该时间段不可预约", icon: "none" });
      return;
    }
    this.selectSlot(time);
  },

  handleNoteInput(e: WechatMiniprogram.Input) {
    this.setData({ note: e.detail.value });
  },

  handleSelectAddress() {
    if (this.data.addresses.length === 0) {
      wx.navigateTo({ url: "/packageUser/pages/address-edit/index" });
      return;
    }
    wx.navigateTo({ url: "/packageUser/pages/address-list/index" });
  },

  handleTravelModeChange(e: WechatMiniprogram.TouchEvent) {
    const mode = e.currentTarget.dataset.mode as "transit" | "driving";
    this.setData({ travelMode: mode }, () => {
      this.autoCalculatePreview();
    });
  },

  handleSelectCoupon() {
    const coupons = this.data.availableCoupons;
    if (coupons.length === 0) {
      wx.showToast({ title: "暂无可用优惠券", icon: "none" });
      return;
    }
    const items = ["不使用优惠券", ...coupons.map((c: any) => `${c.name} 减¥${c.discount}`)];
    wx.showActionSheet({
      itemList: items.slice(0, 6),
      success: res => {
        if (res.tapIndex === 0) {
          this.setData({ couponId: null, couponDesc: "暂不使用优惠券" }, () => {
            this.autoCalculatePreview();
          });
        } else {
          const coupon = coupons[res.tapIndex - 1];
          this.setData({
            couponId: coupon.id,
            couponDesc: `${coupon.name} 减¥${coupon.discount}`,
          }, () => {
            this.autoCalculatePreview();
          });
        }
      },
    });
  },

  // 自动在后台静默核算价格
  async autoCalculatePreview() {
    const { projectId, technicianId, addressId, serviceDate, startTime } = this.data;
    if (!projectId || !technicianId || !addressId || !serviceDate || !startTime) {
      return;
    }

    this.setData({ loading: true, error: "" });
    try {
      const preview = await previewOrder({
        projectId,
        technicianId,
        addressId,
        serviceDate,
        startTime,
      });
      this.setData({
        preview,
        estimatedBasePrice: preview.payableAmount,
        loading: false,
      });
    } catch (err: any) {
      this.setData({
        loading: false,
        error: err?.message || "",
      });
    }
  },

  async handlePreview() {
    await this.autoCalculatePreview();
  },

  async handleSubmit() {
    const { projectId, technicianId, addressId, serviceDate, startTime, note, couponId, paymentMode } = this.data;
    if (!addressId) {
      wx.showToast({ title: "请选择服务地址", icon: "none" });
      return;
    }
    if (!serviceDate || !startTime) {
      wx.showToast({ title: "请选择服务时间", icon: "none" });
      return;
    }

    this.setData({ submitting: true });
    try {
      const orderDetail = await createOrder({
        projectId,
        technicianId,
        addressId,
        serviceDate,
        startTime,
        note: note.trim() || undefined,
        couponId: couponId || undefined,
      });

      const orderNo = orderDetail.order.orderNo;
      const payment = await createPayment(orderNo);

      // 分支处理三种支付模式：
      // 1. OFFLINE (现场支付 / 仅预约无需支付)
      if (payment.channel === "OFFLINE" || paymentMode === "OFFLINE") {
        wx.showToast({ title: "预约成功！现场结算", icon: "success" });
        setTimeout(() => {
          wx.redirectTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderNo}` });
        }, 1200);
        return;
      }

      // 2. MOCK (模拟支付)
      if (payment.channel === "MOCK") {
        await simulatePayment(payment.paymentNo, "SUCCESS");
        wx.showToast({ title: "预约支付成功！", icon: "success" });
        setTimeout(() => {
          wx.redirectTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderNo}` });
        }, 1200);
        return;
      }

      // 3. WXPAY (真实微信支付)
      const params = payment.payParams;
      if (!params) {
        throw new Error("缺少微信支付参数");
      }

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

      wx.showToast({ title: "微信支付成功！", icon: "success" });
      setTimeout(() => {
        wx.redirectTo({ url: `/packageUser/pages/order-detail/index?orderNo=${orderNo}` });
      }, 1200);
    } catch (err: any) {
      const msg = err?.message || err?.errMsg || "下单失败";
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
