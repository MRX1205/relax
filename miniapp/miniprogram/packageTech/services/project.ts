import { request } from "./http";

export interface TechProjectItem {
  id: number;
  technicianId: number;
  projectId: number;
  overridePrice: number;
  status: "ENABLED" | "DISABLED";
  projectName: string;
  basePrice: number;
  durationMinutes: number;
  categoryName?: string;
  creatorType?: "PLATFORM" | "TECHNICIAN";
  creatorId?: number;
  description?: string;
  notice?: string;
}

export interface PlatformProjectItem {
  id: number;
  categoryId: number;
  categoryName?: string;
  name: string;
  durationMinutes: number;
  basePrice: number;
  description?: string;
  notice?: string;
  status: string;
  creatorType?: string;
}

export interface CreateCustomProjectDto {
  categoryId: number;
  name: string;
  durationMinutes: number;
  basePrice: number;
  description?: string;
  notice?: string;
  coverFileId?: number;
  onShelf?: boolean;
}

export interface UpdateProjectDto {
  categoryId?: number;
  name?: string;
  durationMinutes?: number;
  overridePrice?: number;
  description?: string;
  notice?: string;
  coverFileId?: number;
  status?: "ENABLED" | "DISABLED";
}

export function getMyProjects(): Promise<TechProjectItem[]> {
  return request<TechProjectItem[]>({
    url: "/api/v1/technician/projects",
    method: "GET",
  });
}

export function getPlatformAvailableProjects(): Promise<PlatformProjectItem[]> {
  return request<PlatformProjectItem[]>({
    url: "/api/v1/technician/available-platform-projects",
    method: "GET",
  });
}

export function joinPlatformProject(projectId: number, overridePrice?: number): Promise<TechProjectItem> {
  return request<TechProjectItem>({
    url: "/api/v1/technician/projects/join",
    method: "POST",
    data: { projectId, overridePrice },
  });
}

export function createCustomProject(data: CreateCustomProjectDto): Promise<TechProjectItem> {
  return request<TechProjectItem>({
    url: "/api/v1/technician/projects",
    method: "POST",
    data: data as any,
  });
}

export function updateProject(id: number, data: UpdateProjectDto): Promise<TechProjectItem> {
  return request<TechProjectItem>({
    url: `/api/v1/technician/projects/${id}`,
    method: "PUT",
    data: data as any,
  });
}

export function toggleProjectStatus(id: number): Promise<TechProjectItem> {
  return request<TechProjectItem>({
    url: `/api/v1/technician/projects/${id}/toggle-status`,
    method: "PUT",
  });
}

export function deleteProject(id: number): Promise<void> {
  return request<void>({
    url: `/api/v1/technician/projects/${id}`,
    method: "DELETE",
  });
}
