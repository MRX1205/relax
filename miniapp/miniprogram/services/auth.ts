import { environment } from "../config/environment";
import { clearAccessToken, getAccessToken, request, setAccessToken } from "./http";

const ACCOUNT_KEY = "relax.account";

export function getCachedAccount(): Account | null {
  return wx.getStorageSync<Account>(ACCOUNT_KEY) || null;
}

export function cacheAccount(account: Account): Account {
  wx.setStorageSync(ACCOUNT_KEY, account);
  const app = getApp<RelaxAppOption>();
  app.globalData.account = account;
  return account;
}

export async function loginWithWechat(): Promise<LoginResult> {
  // 开发模式使用固定code，体验版和正式版使用wx.login获取真实code
  const isDev = environment.version === "develop";
  const code = isDev
    ? "miniapp-local-super-admin"
    : await new Promise<string>((resolve, reject) => {
        wx.login({
          success: res => resolve(res.code),
          fail: err => reject(new Error(err.errMsg || "wx.login failed")),
        });
      });
  const result = await request<LoginResult>({
    url: "/api/v1/auth/wechat-login",
    method: "POST",
    data: { code },
  });
  setAccessToken(result.accessToken);
  cacheAccount(result.account);
  return result;
}

export async function loadCurrentAccount(): Promise<Account | null> {
  if (!getAccessToken()) {
    return null;
  }
  const account = await request<Account>({ url: "/api/v1/me" });
  return cacheAccount(account);
}

export async function bindPhone(code: string): Promise<Account> {
  const account = await request<Account>({
    url: "/api/v1/auth/bind-phone",
    method: "POST",
    data: { code },
  });
  return cacheAccount(account);
}

export async function updateProfile(nickname: string, avatarUrl: string): Promise<Account> {
  const account = await request<Account>({
    url: "/api/v1/me/profile",
    method: "PUT",
    data: { nickname, avatarUrl },
  });
  return cacheAccount(account);
}

export async function switchRole(role: RoleCode): Promise<Account> {
  const account = await request<Account>({
    url: "/api/v1/me/last-role",
    method: "PUT",
    data: { role },
  });
  return cacheAccount(account);
}

export async function logout(): Promise<void> {
  if (getAccessToken()) {
    await request<void>({ url: "/api/v1/auth/logout", method: "POST" });
  }
  clearSession();
}

export function clearSession(): void {
  clearAccessToken();
  wx.removeStorageSync(ACCOUNT_KEY);
  getApp<RelaxAppOption>().globalData.account = null;
}
