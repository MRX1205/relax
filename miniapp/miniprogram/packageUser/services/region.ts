import { request } from "../../services/http";

export function getServiceAreas(): Promise<ServiceArea[]> {
  return request<ServiceArea[]>({ url: "/api/v1/regions/dongguan/service-areas" });
}

export function getAddresses(): Promise<UserAddress[]> {
  return request<UserAddress[]>({ url: "/api/v1/addresses" });
}

export function createAddress(data: AddressInput): Promise<UserAddress> {
  return request<UserAddress>({ url: "/api/v1/addresses", method: "POST", data });
}

export function updateAddress(id: string, data: AddressInput): Promise<UserAddress> {
  return request<UserAddress>({ url: `/api/v1/addresses/${id}`, method: "PUT", data });
}

export function deleteAddress(id: string): Promise<void> {
  return request<void>({ url: `/api/v1/addresses/${id}`, method: "DELETE" });
}

export function updateServiceArea(id: string, status: "ENABLED" | "DISABLED"): Promise<ServiceArea> {
  return request<ServiceArea>({
    url: `/api/v1/admin/service-areas/${id}`,
    method: "PUT",
    data: { status },
  });
}
