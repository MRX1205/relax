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

export function toFriendlyChineseMessage(rawMsg: string, statusCode: number): string {
  if (!rawMsg) {
    if (statusCode === 401) return "请先登录或登录已过期";
    if (statusCode === 403) return "当前账号无权执行该操作";
    if (statusCode === 404) return "未找到对应的数据或服务";
    if (statusCode >= 500) return "服务器繁忙，请稍后重试";
    return `请求失败（${statusCode}）`;
  }

  const s = String(rawMsg).trim();
  if (/bad credentials/i.test(s) || /password.*incorrect/i.test(s)) {
    return "手机号或密码不正确，请重新输入";
  }
  if (/access denied/i.test(s) || /forbidden/i.test(s)) {
    return "当前账号权限不足，无权执行此操作";
  }
  if (/authentication.*required/i.test(s) || /unauthorized/i.test(s)) {
    return "登录已过期，请重新登录";
  }
  if (/account.*not.*found/i.test(s) || /user.*not.*found/i.test(s)) {
    return "该手机号未注册或未开通服务端权限";
  }
  if (/role.*not.*granted/i.test(s)) {
    return "该账号尚未开通对应角色权限，请联系管理员";
  }
  if (/phone.*invalid/i.test(s)) {
    return "手机号格式不正确，请输入11位有效手机号码";
  }
  if (/password.*invalid/i.test(s) || /password.*required/i.test(s)) {
    return "密码格式不符合要求，请输入不少于6位密码";
  }
  if (/internal server error/i.test(s)) {
    return "系统繁忙，请稍后重试";
  }
  if (/request:fail/i.test(s) || /timeout/i.test(s) || /fail/i.test(s) && /connect/i.test(s)) {
    return "网络连接失败，请检查网络设置或开启微信开发调试";
  }
  if (/validation failed/i.test(s) || /constraintviolation/i.test(s)) {
    return "输入的内容不符合规范，请检查后重新提交";
  }
  if (/duplicate/i.test(s)) {
    return "数据已存在，请勿重复添加";
  }

  // If contains Chinese, return directly
  if (/[\u4e00-\u9fa5]/.test(s)) {
    return s;
  }
  return `操作提示: ${s}`;
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
          if (!curRoute.includes("pages/login/index") && !curRoute.includes("pages/role-login/index")) {
            wx.showToast({ title: "登录已过期，请重新登录", icon: "none" });
            setTimeout(() => {
              wx.reLaunch({ url: "/pages/login/index" });
            }, 800);
          }
        }

        const friendlyMsg = toFriendlyChineseMessage(envelope.message, response.statusCode);
        reject(new ApiError(
          envelope.code || "REQUEST_FAILED",
          friendlyMsg,
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

export function downloadAndOpenDocument(
  path: string,
  fileName?: string,
  fileType: "doc" | "docx" | "xls" | "xlsx" | "ppt" | "pptx" | "pdf" = "xlsx"
): Promise<void> {
  return new Promise((resolve, reject) => {
    wx.showLoading({ title: "正在导出..." });
    const token = getAccessToken();
    const url = `${environment.apiBaseUrl}${path}`;
    const safeAsciiName = `order_export_${Date.now()}.${fileType}`;
    const targetFilePath = `${wx.env.USER_DATA_PATH}/${safeAsciiName}`;

    wx.request({
      url,
      method: "GET",
      responseType: "arraybuffer",
      header: {
        Authorization: token ? `Bearer ${token}` : "",
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          const fs = wx.getFileSystemManager();
          fs.writeFile({
            filePath: targetFilePath,
            data: res.data as ArrayBuffer,
            encoding: "binary",
            success: () => {
              wx.hideLoading();
              wx.openDocument({
                filePath: targetFilePath,
                fileType,
                showMenu: true,
                success: () => {
                  wx.showToast({ title: "已打开报表", icon: "success" });
                  resolve();
                },
                fail: (err) => {
                  wx.showToast({ title: "打开报表失败", icon: "none" });
                  reject(err);
                },
              });
            },
            fail: (err) => {
              wx.hideLoading();
              wx.showToast({ title: "保存报表失败", icon: "none" });
              reject(err);
            },
          });
        } else {
          wx.hideLoading();
          wx.showToast({ title: `导出失败(${res.statusCode})`, icon: "none" });
          reject(new Error(`Export failed with status ${res.statusCode}`));
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: "下载失败，请检查网络", icon: "none" });
        reject(err);
      },
    });
  });
}

