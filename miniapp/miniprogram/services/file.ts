import { environment } from "../config/environment";
import { getAccessToken, request } from "./http";

export async function uploadPrivateFile(filePath: string, purpose: FilePurpose): Promise<FileAssetView> {
  const mimeType = mimeTypeFor(filePath);
  const content = await readFile(filePath);
  const policy = await request<UploadPolicy>({
    url: "/api/v1/files/upload-policies",
    method: "POST",
    data: {
      purpose,
      fileName: filePath.split("/").pop() || "upload",
      mimeType,
      size: content.byteLength,
    },
  });
  await uploadContent(policy.upload, content);
  return request<FileAssetView>({
    url: `/api/v1/files/${policy.fileId}/complete`,
    method: "POST",
  });
}

function readFile(filePath: string): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath,
      success(result) {
        resolve(result.data as ArrayBuffer);
      },
      fail(error) {
        reject(new Error(error.errMsg || "读取文件失败"));
      },
    });
  });
}

function uploadContent(grant: UploadGrant, content: ArrayBuffer): Promise<void> {
  const url = grant.url.startsWith("http") ? grant.url : `${environment.apiBaseUrl}${grant.url}`;
  const headers = { ...grant.headers };
  if (!url.startsWith("http") || url.startsWith(environment.apiBaseUrl)) {
    headers.Authorization = `Bearer ${getAccessToken()}`;
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method: grant.method,
      header: headers,
      data: content,
      timeout: 30000,
      success(response) {
        response.statusCode >= 200 && response.statusCode < 300
          ? resolve()
          : reject(new Error("文件上传失败"));
      },
      fail(error) {
        reject(new Error(error.errMsg || "文件上传失败"));
      },
    });
  });
}

function mimeTypeFor(filePath: string): "image/jpeg" | "image/png" | "application/pdf" {
  const path = filePath.toLowerCase();
  if (path.endsWith(".png")) return "image/png";
  if (path.endsWith(".pdf")) return "application/pdf";
  return "image/jpeg";
}
