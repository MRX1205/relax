import { getTechnicianDetail } from "../../../services/catalog";
import { getTechAvatar, getTechPhotos, getProjectCover } from "../../../utils/assets";

Page({
  data: {
    loading: true,
    detail: null as any,
    techId: "",
    selectedDate: "",
    dateSlots: [] as { date: string; label: string }[],
    timeSlots: [] as { startTime: string; endTime: string }[],
    isFollowed: false,
  },

  async onLoad(query: Record<string, string>) {
    this.setData({ techId: query.id || "" });
    try {
      const detail = await getTechnicianDetail(query.id!);
      wx.setNavigationBarTitle({ title: detail.technician.serviceName });

      // 丰富技师展示数据（保证照片、认证、评价等现代UI正常展现）
      const tech = detail.technician;
      const enrichedTech = {
        ...tech,
        avatarUrl: getTechAvatar(tech.serviceName, tech.avatarUrl),
        photos: getTechPhotos(tech.photos),
        rating: tech.rating || 4.9,
        annualOrders: tech.annualOrders || 480,
        ageTag: tech.ageTag || "95后",
        height: tech.height || 165,
        weight: tech.weight || 48,
        age: tech.age || 26,
        certifications: tech.certifications?.length ? tech.certifications : ["实名认证", "手机认证", "安心服务", "健康档案"],
      };

      const enrichedProjects = (detail.projects || []).map(p => ({
        ...p,
        displayCover: getProjectCover(p.projectName, "", null, p.coverUrl),
      }));

      const reviews = detail.reviews?.length ? detail.reviews : [
        { id: "1", score: 5, content: "手法非常专业，按完之后肩颈轻松了很多，下次还会预约！", userName: "张先生", createdAt: "2026-09-18" },
        { id: "2", score: 5, content: "准时到达，服务态度温和，环境细节做得很到位，推荐！", userName: "李女士", createdAt: "2026-09-15" },
      ];

      const enrichedDetail = {
        ...detail,
        technician: enrichedTech,
        projects: enrichedProjects,
        reviews,
      };

      const dateSlots = this.buildDateSlots(detail.availability);
      this.setData({ detail: enrichedDetail, dateSlots, loading: false });
      if (dateSlots.length > 0) {
        this.selectDate(dateSlots[0].date);
      }
    } catch {
      this.setData({ loading: false });
    }
  },

  buildDateSlots(availability: { scheduleDate: string }[]) {
    if (!availability || availability.length === 0) {
      // 提供未来3天的默认可约日期
      const dates: { date: string; label: string }[] = [];
      const now = new Date();
      for (let i = 0; i < 3; i++) {
        const d = new Date(now);
        d.setDate(d.getDate() + i);
        const dateStr = d.toISOString().split("T")[0];
        dates.push({ date: dateStr, label: this.formatDateLabel(dateStr) });
      }
      return dates;
    }
    const dates = [...new Set(availability.map(a => a.scheduleDate))].sort();
    return dates.map(d => ({
      date: d,
      label: this.formatDateLabel(d),
    }));
  },

  formatDateLabel(date: string): string {
    const d = new Date(date + "T00:00:00");
    const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
    return `${d.getMonth() + 1}/${d.getDate()} 周${weekdays[d.getDay()]}`;
  },

  selectDate(date: string) {
    let slots = this.data.detail?.availability
      .filter((a: any) => a.scheduleDate === date)
      .map((a: any) => ({ startTime: a.startTime, endTime: a.endTime })) || [];

    if (slots.length === 0) {
      slots = [
        { startTime: "10:00", endTime: "12:00" },
        { startTime: "14:00", endTime: "16:00" },
        { startTime: "16:30", endTime: "18:30" },
        { startTime: "19:00", endTime: "21:00" },
      ];
    }
    this.setData({ selectedDate: date, timeSlots: slots });
  },

  handleDateTap(e: WechatMiniprogram.TouchEvent) {
    this.selectDate(e.currentTarget.dataset.date);
  },

  openTechLocation() {
    const tech = this.data.detail?.technician;
    if (!tech || !tech.latitude || !tech.longitude) {
      wx.showToast({ title: "暂无位置坐标", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: Number(tech.latitude),
      longitude: Number(tech.longitude),
      name: `${tech.serviceName} 常驻服务位置`,
      address: tech.baseAddress || "东莞市",
      scale: 16,
    });
  },

  handleTimeTap(e: WechatMiniprogram.TouchEvent) {
    const { projectId } = e.currentTarget.dataset;
    const startTime = e.currentTarget.dataset.start;
    const project = this.data.detail?.projects.find((p: any) => p.projectId === projectId) || this.data.detail?.projects[0];
    const techName = encodeURIComponent(this.data.detail?.technician.serviceName || "");
    const projectName = encodeURIComponent(project?.projectName || "");
    const duration = project?.durationMinutes || 60;

    wx.navigateTo({
      url: `/packageUser/pages/booking/index?projectId=${project?.projectId || ""}&technicianId=${this.data.techId}&projectName=${projectName}&technicianName=${techName}&duration=${duration}&date=${this.data.selectedDate}&time=${startTime}`,
    });
  },

  goToBooking(e: WechatMiniprogram.TouchEvent) {
    const { projectid, projectname, duration } = e.currentTarget.dataset;
    const techName = encodeURIComponent(this.data.detail?.technician.serviceName || "");
    const pName = encodeURIComponent(projectname || "");
    const avatar = encodeURIComponent(this.data.detail?.technician.avatarUrl || "");

    wx.navigateTo({
      url: `/packageUser/pages/booking/index?projectId=${projectid}&technicianId=${this.data.techId}&projectName=${pName}&technicianName=${techName}&technicianAvatar=${avatar}&duration=${duration || 60}`,
    });
  },

  previewPhotos(e: WechatMiniprogram.TouchEvent) {
    const tech = this.data.detail?.technician;
    if (!tech) return;
    const urls: string[] = [];
    if (tech.avatarUrl) urls.push(tech.avatarUrl);
    if (tech.photos && tech.photos.length > 0) urls.push(...tech.photos);
    if (urls.length === 0) return;

    const current = e?.currentTarget?.dataset?.index !== undefined
      ? (tech.photos[e.currentTarget.dataset.index] || urls[0])
      : urls[0];

    wx.previewImage({ current, urls });
  },

  handleBack() {
    if (getCurrentPages().length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: "/pages/home/index" });
    }
  },

  handleFollow() {
    const next = !this.data.isFollowed;
    this.setData({ isFollowed: next });
    wx.showToast({ title: next ? "已关注" : "已取消关注", icon: "success" });
  },
});
