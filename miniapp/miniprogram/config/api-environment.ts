export type EnvironmentVersion = "develop" | "trial" | "release";

const API_BASE_URLS: Record<EnvironmentVersion, string> = {
  develop: "http://127.0.0.1:8080",
  trial: "https://staging-api.example.invalid",
  release: "https://api.example.invalid",
};

export function getApiBaseUrl(version: EnvironmentVersion): string {
  return API_BASE_URLS[version];
}
