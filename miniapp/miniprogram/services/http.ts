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
      header: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
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
            title: "网络连接失败",
            content: `无法连接服务器:\n${environment.apiBaseUrl}\n\n排查提示：\n1. 手机真机调试：请确保手机与电脑在同一WiFi，并在电脑真机调试面板中勾选【不校验合法域名】\n2. 若电脑IP变动，可点击下方按钮直接修改`,
            showCancel: true,
            cancelText: "关闭",
            confirmText: "修改IP",
            success(res) {
              if (res.confirm) {
                wx.showModal({
                  title: "修改后端IP地址",
                  editable: true,
                  placeholderText: `如: http://${DEFAULT_LAN_IP}:8080`,
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
