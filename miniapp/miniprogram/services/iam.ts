import { request } from "./http";

export function searchAccessUsers(keyword = ""): Promise<AccessUser[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  return request<AccessUser[]>({ url: `/api/v1/admin/access/users${query}` });
}

export function getPermissionGroups(): Promise<PermissionGroup[]> {
  return request<PermissionGroup[]>({ url: "/api/v1/admin/access/groups" });
}

export function updateAdminAccess(userId: string, enabled: boolean, groupCodes: string[]): Promise<AccessUser> {
  return request<AccessUser>({
    url: `/api/v1/admin/access/users/${userId}`,
    method: "PUT",
    data: { enabled, groupCodes },
  });
}

export function updateAccountStatus(userId: string, status: "ACTIVE" | "DISABLED"): Promise<AccessUser> {
  return request<AccessUser>({
    url: `/api/v1/admin/access/users/${userId}/status`,
    method: "PUT",
    data: { status },
  });
}
