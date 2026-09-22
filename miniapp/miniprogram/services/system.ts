import { request } from "./http";

export interface PublicSystemSettings {
  appName: string;
  vipEnabled: boolean;
}

export async function getPublicSystemSettings(): Promise<PublicSystemSettings> {
  try {
    return await request<PublicSystemSettings>({ url: "/api/v1/system/settings" });
  } catch {
    return { appName: "东莞到家", vipEnabled: false };
  }
}

export async function updateSystemSettings(data: { appName?: string; vipEnabled?: boolean }): Promise<PublicSystemSettings> {
  return request<PublicSystemSettings>({
    url: "/api/v1/admin/system/settings",
    method: "PUT",
    data,
  });
}
