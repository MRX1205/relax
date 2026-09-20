import { getCachedAccount, switchRole, loadCurrentAccount } from "../../services/auth";
import { getAccessToken } from "../../services/http";

interface RoleItem {
  code: RoleCode;
  name: string;
  description: string;
  icon: string;
}

const ROLE_MAP: RoleItem[] = [
  { code: "USER", name: "用户", description: "浏览项目、预约技师、查看订单", icon: "🏠" },
  { code: "TECHNICIAN", name: "技师", description: "接单、排班、收入查询", icon: "🔧" },
  { code: "ADMIN", name: "管理员", description: "项目管理、订单处理、结算", icon: "⚙️" },
  { code: "SUPER_ADMIN", name: "超级管理员", description: "全部权限、管理员管理", icon: "👑" },
];

Page({
  data: {
    roles: [] as RoleItem[],
    currentRole: "" as RoleCode,
    switching: false,
  },

  async onLoad() {
    if (!getAccessToken()) {
      wx.reLaunch({ url: "/pages/login/index" });
      return;
    }
    
    try {
      const account = await loadCurrentAccount();
      if (!account) {
        wx.reLaunch({ url: "/pages/login/index" });
        return;
      }
      const roles = ROLE_MAP.filter(r => account.roles.includes(r.code));
      this.setData({ roles, currentRole: account.lastRole });
    } catch {
      const account = getCachedAccount();
      if (!account) {
        wx.reLaunch({ url: "/pages/login/index" });
        return;
      }
      const roles = ROLE_MAP.filter(r => account.roles.includes(r.code));
      this.setData({ roles, currentRole: account.lastRole });
    }
  },

  async handleSwitch(e: WechatMiniprogram.TouchEvent) {
    const role = e.currentTarget.dataset.role as RoleCode;
    if (role === this.data.currentRole || this.data.switching) return;
    this.setData({ switching: true });
    try {
      await switchRole(role);
      wx.showToast({ title: "切换成功", icon: "success" });
      setTimeout(() => {
        wx.reLaunch({ url: "/pages/home/index" });
      }, 500);
    } catch (err) {
      wx.showToast({ title: "切换失败", icon: "none" });
    } finally {
      this.setData({ switching: false });
    }
  },
});
