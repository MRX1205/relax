import { getCachedAccount, loadCurrentAccount, switchRole } from "../../services/auth";
import { getAccessToken } from "../../services/http";
import { hasRole } from "../../utils/auth-guard";

interface RoleItem {
  code: RoleCode;
  name: string;
  description: string;
  icon: string;
  badge?: string;
  authorized?: boolean;
}

const ROLE_ITEMS: RoleItem[] = [
  {
    code: "USER",
    name: "顾客端",
    description: "浏览精选项目、预约上门技师、查看订单",
    icon: "🏠",
    badge: "顾客",
  },
  {
    code: "TECHNICIAN",
    name: "技师端",
    description: "接单履约、自建项目、收入明细与排班管理",
    icon: "💆",
    badge: "服务商",
  },
  {
    code: "ADMIN",
    name: "管理员端",
    description: "技师入驻审批、订单调度改派、全平台结算看板",
    icon: "🛡️",
    badge: "管理台",
  },
];

Page({
  data: {
    roles: ROLE_ITEMS,
    currentRole: "USER" as RoleCode,
  },

  async onShow() {
    if (!getAccessToken()) {
      return;
    }
    try {
      const account = await loadCurrentAccount();
      const currentRole = account?.lastRole || "USER";
      const roles = this.data.roles.map(r => ({
        ...r,
        authorized: r.code === "USER" || hasRole(r.code),
      }));
      this.setData({ currentRole, roles });
    } catch {
      const account = getCachedAccount();
      const currentRole = account?.lastRole || "USER";
      const roles = this.data.roles.map(r => ({
        ...r,
        authorized: r.code === "USER" || hasRole(r.code),
      }));
      this.setData({ currentRole, roles });
    }
  },

  async handleSelectRole(e: WechatMiniprogram.TouchEvent) {
    const role = e.currentTarget.dataset.role as RoleCode;

    if (role === "USER") {
      // 切换回用户端并跳转至首页
      try {
        await switchRole("USER");
      } catch {
        // ignore
      }
      wx.switchTab({ url: "/pages/home/index" });
      return;
    }

    // 技师端或管理员端：跳转至对应的登录页面（支持手机号密码与微信快捷登录）
    wx.navigateTo({
      url: `/pages/role-login/index?role=${role}`,
    });
  },
});
