import { getCachedAccount, loadCurrentAccount } from "../services/auth";
import { getAccessToken } from "../services/http";

export type AppRole = "USER" | "TECHNICIAN" | "ADMIN" | "SUPER_ADMIN";

/**
 * 检查当前登录用户是否拥有指定角色
 */
export function hasRole(role: AppRole): boolean {
  const account = getCachedAccount();
  if (!account || !account.roles) return false;
  return account.roles.includes(role);
}

/**
 * 页面级权限守卫：进入敏感页面（如技师工作台、管理员总台）时校验
 * 如果无权限，拦截并阻断页面渲染，提示后引导回退
 */
export async function requireRole(
  requiredRole: AppRole,
  options: {
    fallbackUrl?: string;
    deniedMessage?: string;
    silent?: boolean;
  } = {}
): Promise<boolean> {
  const {
    fallbackUrl = "/pages/home/index",
    deniedMessage,
    silent = false,
  } = options;

  const hasToken = !!getAccessToken();
  if (!hasToken) {
    if (!silent) {
      wx.showToast({ title: "请先登录", icon: "none" });
    }
    setTimeout(() => {
      wx.navigateTo({ url: "/pages/login/index" });
    }, 500);
    return false;
  }

  // 尝试刷新最新的账号角色信息
  let account = getCachedAccount();
  try {
    const latest = await loadCurrentAccount();
    if (latest) account = latest;
  } catch {
    // 离线/网络异常时使用本地缓存
  }

  const roles = account?.roles || [];

  // SUPER_ADMIN 拥有对所有页面的豁免访问权
  if (roles.includes("SUPER_ADMIN")) {
    return true;
  }

  if (!roles.includes(requiredRole)) {
    if (!silent) {
      const roleName = requiredRole === "ADMIN" ? "平台管理员" : "认证技师";
      const content = deniedMessage || `当前账号未被授权为${roleName}，无法访问此专区页面。`;
      wx.showModal({
        title: "无权访问",
        content,
        showCancel: false,
        confirmText: "返回首页",
        confirmColor: "#E54D42",
        success: () => {
          wx.switchTab({ url: fallbackUrl });
        },
      });
    } else {
      wx.switchTab({ url: fallbackUrl });
    }
    return false;
  }

  return true;
}
