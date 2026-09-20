import { environment } from "../config/environment";

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
        const envelope = response.data;
        if (response.statusCode >= 200 && response.statusCode < 300 && envelope.code === "OK") {
          resolve(envelope.data);
          return;
        }

        if (response.statusCode === 401) {
          clearAccessToken();
        }
        reject(new ApiError(
          envelope.code || "REQUEST_FAILED",
          envelope.message || `请求失败（${response.statusCode}）`,
          response.statusCode,
        ));
      },
      fail(error) {
        reject(new Error(error.errMsg || "网络连接失败"));
      },
    });
  });
}
