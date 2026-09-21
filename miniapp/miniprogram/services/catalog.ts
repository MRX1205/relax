import { request } from "./http";

export function getHomeData(): Promise<HomeData> {
  return request<HomeData>({ url: "/api/v1/home" });
}

export function getProjects(categoryId: string | number = 0, page = 0): Promise<ProjectBrief[]> {
  const params = new URLSearchParams();
  if (categoryId && categoryId !== "0") params.set("categoryId", String(categoryId));
  params.set("page", String(page));
  return request<ProjectBrief[]>({ url: `/api/v1/projects?${params}` });
}

export function getProjectDetail(id: string): Promise<ProjectDetail> {
  return request<ProjectDetail>({ url: `/api/v1/projects/${id}` });
}

export function getTechnicians(page = 0): Promise<TechnicianItem[]> {
  return request<TechnicianItem[]>({ url: `/api/v1/technicians?page=${page}` });
}

export function getTechnicianDetail(id: string): Promise<TechnicianDetail> {
  return request<TechnicianDetail>({ url: `/api/v1/technicians/${id}` });
}

export function previewOrder(data: {
  projectId: string;
  technicianId: string;
  addressId?: string;
  serviceDate: string;
  startTime: string;
}): Promise<OrderPreview> {
  return request<OrderPreview>({ url: "/api/v1/orders/preview", method: "POST", data });
}
