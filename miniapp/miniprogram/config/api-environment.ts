export type EnvironmentVersion = "develop" | "trial" | "release";

export const DEFAULT_API_BASE_URL = "https://realxback.lyhlz.cn";
export const DEFAULT_LAN_IP = "192.168.1.7";
export const DEFAULT_PORT = "8080";

const API_BASE_URLS: Record<EnvironmentVersion, string> = {
  develop: DEFAULT_API_BASE_URL,
  trial: DEFAULT_API_BASE_URL,
  release: DEFAULT_API_BASE_URL,
};

export function isRealDevice(): boolean {
  if (typeof wx !== "undefined" && typeof wx.getSystemInfoSync === "function") {
    try {
      const info = wx.getSystemInfoSync();
      // 在真机上 platform 为 "ios" / "android"，而模拟器中为 "devtools"
      return info.platform === "ios" || info.platform === "android" || info.platform !== "devtools";
    } catch {
      return false;
    }
  }
  return false;
}

export function getApiBaseUrl(version: EnvironmentVersion): string {
  // 1. 本地存储优先（支持开发者手动覆盖）
  if (typeof wx !== "undefined" && typeof wx.getStorageSync === "function") {
    try {
      let custom = wx.getStorageSync<string>("relax.apiBaseUrl");
      if (custom && custom.startsWith("http")) {
        custom = custom.trim().replace(/\/+$/, "");
        // 关键防御：如果本地缓存了旧版私有局域网IP (192.168.* / 127.0.0.1 / localhost)，自动清理并重置为云端生产域名
        if (custom.includes("192.168.") || custom.includes("127.0.0.1") || custom.includes("localhost")) {
          wx.removeStorageSync("relax.apiBaseUrl");
          return DEFAULT_API_BASE_URL;
        }
        return custom;
      }
    } catch {
      // ignore in environments where storage is unavailable
    }
  }

  return API_BASE_URLS[version] || DEFAULT_API_BASE_URL;
}

export function setCustomApiBaseUrl(url: string): void {
  if (typeof wx !== "undefined" && typeof wx.setStorageSync === "function") {
    wx.setStorageSync("relax.apiBaseUrl", url.trim().replace(/\/+$/, ""));
  }
}

export function clearCustomApiBaseUrl(): void {
  if (typeof wx !== "undefined" && typeof wx.removeStorageSync === "function") {
    wx.removeStorageSync("relax.apiBaseUrl");
  }
}
