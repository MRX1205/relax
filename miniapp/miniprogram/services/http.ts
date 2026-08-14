import { environment } from "../config/environment";

const ACCESS_TOKEN_KEY = "relax.accessToken";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

interface RequestOptions {
  url: string;
  method?: HttpMethod;
  data?: WechatMiniprogram.IAnyObject | string | ArrayBuffer;
}

function getAccessToken(): string {
  return wx.getStorageSync<string>(ACCESS_TOKEN_KEY) || "";
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

        reject(new Error(envelope.message || `请求失败（${response.statusCode}）`));
      },
      fail(error) {
        reject(new Error(error.errMsg || "网络连接失败"));
      },
    });
  });
}

