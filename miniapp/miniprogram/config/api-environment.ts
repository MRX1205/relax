export type EnvironmentVersion = "develop" | "trial" | "release";

const API_BASE_URLS: Record<EnvironmentVersion, string> = {
  develop: "http://192.168.1.7:8080",
  trial: "https://realxback.lyhlz.cn",
  release: "https://realxback.lyhlz.cn",
};

export function getApiBaseUrl(version: EnvironmentVersion): string {
  return API_BASE_URLS[version];
}
