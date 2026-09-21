import { getProjectDetail } from "../../../services/catalog";
import { getProjectCover, getTechAvatar } from "../../../utils/assets";

Page({
  data: {
    loading: true,
    detail: null as any,
    projectId: "",
  },

  async onLoad(query: Record<string, string>) {
    this.setData({ projectId: query.id || "" });
    try {
      const detail = await getProjectDetail(query.id!);
      wx.setNavigationBarTitle({ title: detail.project.name });

      const enrichedProject = {
        ...detail.project,
        displayCover: getProjectCover(
          detail.project.name,
          detail.project.categoryName,
          detail.project.coverFileId,
          (detail.project as any).coverUrl
        ),
      };

      const enrichedTechnicians = (detail.technicians || []).map(t => ({
        ...t,
        displayAvatar: getTechAvatar(t.serviceName, t.avatarUrl),
      }));

      this.setData({
        detail: {
          ...detail,
          project: enrichedProject,
          technicians: enrichedTechnicians,
        },
        loading: false,
      });
    } catch {
      this.setData({ loading: false });
    }
  },

  goToTechnician(e: WechatMiniprogram.TouchEvent) {
    const techId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/packageUser/pages/technician-detail/index?id=${techId}` });
  },

  goToBooking(e: WechatMiniprogram.TouchEvent) {
    const techId = e.currentTarget.dataset.id;
    const techName = e.currentTarget.dataset.name;
    const projectName = this.data.detail?.project.name || "";
    const duration = this.data.detail?.project.durationMinutes || 60;
    wx.navigateTo({
      url: `/packageUser/pages/booking/index?projectId=${this.data.projectId}&technicianId=${techId}&projectName=${encodeURIComponent(projectName)}&technicianName=${encodeURIComponent(techName)}&duration=${duration}`,
    });
  },
});
