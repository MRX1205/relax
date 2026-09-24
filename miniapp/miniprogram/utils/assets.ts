import { environment } from "../config/environment";

/**
 * Asset resolution helper:
 * - If backend uploaded an image (coverFileId / coverUrl / avatarUrl / photos), display the uploaded image.
 * - Otherwise, display modern Apple-style default graphics.
 */

export function resolveAssetUrl(url?: string | null): string {
  if (!url) return "";
  const str = String(url).trim();
  if (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("/assets/")) {
    return str;
  }
  if (str.startsWith("/")) {
    return `${environment.apiBaseUrl}${str}`;
  }
  return `${environment.apiBaseUrl}/${str}`;
}

export function getProjectCover(name = "", categoryName = "", coverFileId?: string | number | null, coverUrl?: string | null): string {
  if (coverUrl && String(coverUrl).trim().length > 0) {
    return resolveAssetUrl(coverUrl);
  }
  if (coverFileId && String(coverFileId).trim().length > 0) {
    const fileIdStr = String(coverFileId);
    if (fileIdStr.startsWith("http") || fileIdStr.startsWith("/assets/")) {
      return fileIdStr;
    }
    return `${environment.apiBaseUrl}/api/v1/public/files/${fileIdStr}`;
  }

  const combined = (name + " " + categoryName).toLowerCase();

  if (combined.includes("推拿") || combined.includes("中医") || combined.includes("经络")) {
    return "/assets/images/project-massage.jpg";
  }
  if (combined.includes("肩颈") || combined.includes("颈椎")) {
    return "/assets/images/project-neck.jpg";
  }
  if (combined.includes("spa") || combined.includes("精油") || combined.includes("香薰")) {
    return "/assets/images/project-spa.jpg";
  }
  if (combined.includes("足") || combined.includes("脚")) {
    return "/assets/images/project-foot.jpg";
  }
  if (combined.includes("运动") || combined.includes("康复") || combined.includes("拉伸")) {
    return "/assets/images/project-sports.jpg";
  }

  return "/assets/defaults/default-cover.png";
}

export function getTechAvatar(serviceName = "", avatarUrl?: string | null): string {
  if (avatarUrl && String(avatarUrl).trim().length > 0) {
    return resolveAssetUrl(avatarUrl);
  }
  return "/assets/images/tech-wang.jpg";
}

export function getTechPhotos(photos?: string[] | null): string[] {
  if (photos && photos.length > 0) {
    return photos.map(p => resolveAssetUrl(p)).filter(Boolean);
  }
  return [
    "/assets/images/project-massage.jpg",
    "/assets/images/project-spa.jpg",
    "/assets/images/project-foot.jpg",
    "/assets/images/banner-luxury.jpg",
  ];
}

