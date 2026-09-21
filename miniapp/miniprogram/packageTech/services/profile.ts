import { request } from "./http";

export interface TechPhotoItem {
  id: number;
  technicianId: number;
  fileId: number;
  fileUrl: string;
  photoType: string;
  sort: number;
  createdAt: string;
}

export interface FullProfileData {
  id: number;
  userId: number;
  serviceName: string;
  realName: string;
  phone: string;
  intro: string;
  experienceYears: number;
  onlineStatus: string;
  avatarUrl?: string;
  age?: number;
  ageTag?: string;
  height?: number;
  weight?: number;
  latitude?: number;
  longitude?: number;
  baseAddress?: string;
  certificationsJson?: string;
}

export interface TechFullProfileResponse {
  profile: FullProfileData;
  photos: TechPhotoItem[];
}

export interface UpdateFullProfileDto {
  serviceName?: string;
  phone?: string;
  intro?: string;
  experienceYears?: number;
  avatarUrl?: string;
  age?: number;
  ageTag?: string;
  height?: number;
  weight?: number;
  latitude?: number;
  longitude?: number;
  baseAddress?: string;
  certificationsJson?: string;
}

export function getFullProfile(): Promise<TechFullProfileResponse> {
  return request<TechFullProfileResponse>({
    url: "/api/v1/technician/profile-full",
    method: "GET",
  });
}

export function updateFullProfile(data: UpdateFullProfileDto): Promise<TechFullProfileResponse> {
  return request<TechFullProfileResponse>({
    url: "/api/v1/technician/profile-full",
    method: "PUT",
    data: data as any,
  });
}

export function listPhotos(): Promise<TechPhotoItem[]> {
  return request<TechPhotoItem[]>({
    url: "/api/v1/technician/photos",
    method: "GET",
  });
}

export function addPhoto(data: { fileUrl: string; fileId?: number; photoType?: string; sort?: number }): Promise<TechPhotoItem[]> {
  return request<TechPhotoItem[]>({
    url: "/api/v1/technician/photos",
    method: "POST",
    data: data as any,
  });
}

export function deletePhoto(id: number): Promise<void> {
  return request<void>({
    url: `/api/v1/technician/photos/${id}`,
    method: "DELETE",
  });
}

export function updateOnlineStatus(onlineStatus: "ONLINE" | "OFFLINE" | "RESTING"): Promise<void> {
  return request<void>({
    url: "/api/v1/technician/online-status",
    method: "PUT",
    data: { onlineStatus },
  });
}
