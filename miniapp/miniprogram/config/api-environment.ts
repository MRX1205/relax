export type EnvironmentVersion = "develop" | "trial" | "release";

export const DEFAULT_LAN_IP = "192.168.1.7";
export const DEFAULT_PORT = "8080";

const API_BASE_URLS: Record<EnvironmentVersion, string> = {
  develop: "http://127.0.0.1:8080",
  trial: "https://realxback.lyhlz.cn",
  release: "https://realxback.lyhlz.cn",
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
        // 关键防御：如果在真机上运行，而配置包含 127.0.0.1 或 localhost，真机不可能访问手机自身回路，自动转为电脑局域网IP
        if (isRealDevice() && (custom.includes("127.0.0.1") || custom.includes("localhost"))) {
          custom = custom.replace("127.0.0.1", DEFAULT_LAN_IP).replace("localhost", DEFAULT_LAN_IP);
        }
        return custom;
      }
    } catch {
      // ignore in environments where storage is unavailable
    }
  }

  // 2. 开发模式下的自适应策略：
  if (version === "develop") {
    // 若在真机调试/预览下运行，必须连接电脑局域网IP（如 192.168.1.7）
    if (isRealDevice()) {
      return `http://${DEFAULT_LAN_IP}:${DEFAULT_PORT}`;
    }
    // 若在电脑模拟器 (devtools) 中运行，使用 127.0.0.1
    return `http://127.0.0.1:${DEFAULT_PORT}`;
  }

  return API_BASE_URLS[version];
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
