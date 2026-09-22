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

export async function getWxLoginCode(): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    wx.login({
      success: res => {
        if (res.code) {
          resolve(res.code);
        } else {
          resolve("miniapp-mock-login-code");
        }
      },
      fail: () => resolve("miniapp-mock-login-code"),
    });
  });
}

export async function loginWithWechat(customCode?: string): Promise<LoginResult> {
  // 开发模式若未指定code则使用开发默认
  let code = customCode;
  if (!code) {
    code = await getWxLoginCode();
  }
  const result = await request<LoginResult>({
    url: "/api/v1/auth/wechat-login",
    method: "POST",
    data: { code },
  });
  setAccessToken(result.accessToken);
  cacheAccount(result.account);
  return result;
}

export async function loginWithPhone(phone: string): Promise<LoginResult> {
  const result = await request<LoginResult>({
    url: "/api/v1/auth/phone-login",
    method: "POST",
    data: { phone: phone.trim() },
  });
  setAccessToken(result.accessToken);
  cacheAccount(result.account);
  return result;
}

export async function loginWithPassword(phone: string, password: string, targetRole?: RoleCode | "STAFF"): Promise<LoginResult> {
  const result = await request<LoginResult>({
    url: "/api/v1/auth/password-login",
    method: "POST",
    data: {
      phone: phone.trim(),
      password: password.trim(),
      targetRole: targetRole || "STAFF",
    },
  });
  setAccessToken(result.accessToken);
  cacheAccount(result.account);
  return result;
}

export async function loginRoleWithWechat(targetRole?: RoleCode | "STAFF"): Promise<LoginResult> {
  const code = await getWxLoginCode();
  const result = await request<LoginResult>({
    url: "/api/v1/auth/role-wechat-login",
    method: "POST",
    data: {
      code,
      targetRole: targetRole || "STAFF",
    },
  });
  setAccessToken(result.accessToken);
  cacheAccount(result.account);
  return result;
}

export async function bindCurrentWechat(): Promise<Account> {
  const code = await getWxLoginCode();
  const account = await request<Account>({
    url: "/api/v1/auth/bind-wechat",
    method: "POST",
    data: { code },
  });
  return cacheAccount(account);
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
