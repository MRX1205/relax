import { environment } from "../config/environment";
import { setCustomApiBaseUrl, DEFAULT_LAN_IP } from "../config/api-environment";

const ACCESS_TOKEN_KEY = "relax.accessToken";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  url: string;
  method?: HttpMethod;
  data?: WechatMiniprogram.IAnyObject | string | ArrayBuffer;
}

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly statusCode: number,
  ) {
    super(message);
  }
}

export function getAccessToken(): string {
  return wx.getStorageSync<string>(ACCESS_TOKEN_KEY) || "";
}

export function setAccessToken(token: string): void {
  wx.setStorageSync(ACCESS_TOKEN_KEY, token);
}

export function clearAccessToken(): void {
  wx.removeStorageSync(ACCESS_TOKEN_KEY);
}

export function request<T>({ url, method = "GET", data }: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const accessToken = getAccessToken();

    wx.request<ApiEnvelope<T>>({
      url: `${environment.apiBaseUrl}${url}`,
      method,
      data,
      timeout: 10000,
      header: {
        ...(method === "POST" || method === "PUT" || data !== undefined
          ? { "content-type": "application/json" }
          : {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      success(response) {
        const rawData = response.data;
        const envelope: ApiEnvelope<T> =
          typeof rawData === "object" && rawData !== null
            ? (rawData as ApiEnvelope<T>)
            : { code: "ERROR", message: String(rawData || "响应数据格式错误"), data: null as unknown as T, requestId: "" };

        if (response.statusCode >= 200 && response.statusCode < 300 && envelope.code === "OK") {
          resolve(envelope.data);
          return;
        }

        if (response.statusCode === 401) {
          clearAccessToken();
          const pages = getCurrentPages();
          const curRoute = pages[pages.length - 1]?.route || "";
          if (!curRoute.includes("pages/login/index")) {
            wx.showToast({ title: "登录已过期，请重新登录", icon: "none" });
            setTimeout(() => {
              wx.reLaunch({ url: "/pages/login/index" });
            }, 800);
          }
        }
        reject(new ApiError(
          envelope.code || "REQUEST_FAILED",
          envelope.message || `请求失败（${response.statusCode}）`,
          response.statusCode,
        ));
      },
      fail(error) {
        const targetUrl = `${environment.apiBaseUrl}${url}`;
        console.error(`[HTTP] 请求失败: ${targetUrl}`, error);
        if (environment.version !== "release") {
          wx.showModal({
            title: "网络连接提示",
            content: `无法连接云端服务器:\n${targetUrl}\n\n排查建议：\n1. 若初次在手机扫码，请点击小程序右上角「···」-> 进入【开发版/体验版设置】-> 开启【开发调试】(打开vConsole) 即可直接绕过微信域名白名单限制正常使用！\n2. 微信公众平台后台管理员需在【开发管理】->【开发设置】->【服务器域名】将 https://realxback.lyhlz.cn 添加到 request 合法域名。\n3. 如需临时更换调试接口，可点击下方【修改地址】。`,
            showCancel: true,
            cancelText: "关闭",
            confirmText: "修改地址",
            success(res) {
              if (res.confirm) {
                wx.showModal({
                  title: "修改后端API地址",
                  editable: true,
                  placeholderText: `如: https://realxback.lyhlz.cn`,
                  content: environment.apiBaseUrl,
                  success(inputRes) {
                    if (inputRes.confirm && inputRes.content) {
                      const newUrl = inputRes.content.trim().replace(/\/+$/, "");
                      if (newUrl.startsWith("http")) {
                        setCustomApiBaseUrl(newUrl);
                        wx.showToast({ title: "已更新，正在重载", icon: "success" });
                        setTimeout(() => {
                          const pages = getCurrentPages();
                          const curPage = pages[pages.length - 1];
                          if (curPage && typeof curPage.onLoad === "function") {
                            curPage.onLoad(curPage.options || {});
                          }
                          if (curPage && typeof curPage.onShow === "function") {
                            curPage.onShow();
                          }
                        }, 800);
                      }
                    }
                  }
                });
              }
            }
          });
        }
        reject(new Error(error.errMsg || "网络连接失败，请检查服务器连接"));
      },
    });
  });
}
