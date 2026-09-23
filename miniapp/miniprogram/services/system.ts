import { request } from "./http";

export interface PublicSystemSettings {
  appName: string;
  vipEnabled: boolean;
  servicePhone?: string;
  customerQrUrl?: string;
  inviteRewardAmount?: string;
}

export async function getPublicSystemSettings(): Promise<PublicSystemSettings> {
  try {
    return await request<PublicSystemSettings>({ url: "/api/v1/system/settings" });
  } catch {
    return {
      appName: "东莞到家",
      vipEnabled: false,
      servicePhone: "400-800-6688",
      customerQrUrl: "",
      inviteRewardAmount: "30.00",
    };
  }
}

export async function updateSystemSettings(data: {
  appName?: string;
  vipEnabled?: boolean;
  servicePhone?: string;
  customerQrUrl?: string;
  inviteRewardAmount?: string;
}): Promise<PublicSystemSettings> {
  return request<PublicSystemSettings>({
    url: "/api/v1/admin/system/settings",
    method: "PUT",
    data,
  });
}
