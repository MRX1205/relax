import { environment } from "../config/environment";
import { getAccessToken, request } from "./http";

export interface DirectUploadResult {
  id: number;
  url: string;
  fileName: string;
}

export function uploadDirectFile(filePath: string, purpose: string = "IMAGE"): Promise<DirectUploadResult> {
  const token = getAccessToken();
  const url = `${environment.apiBaseUrl}/api/v1/files/upload`;
  const header: Record<string, string> = {};
  if (token) {
    header.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url,
      filePath,
      name: "file",
      formData: {
        purpose,
      },
      header,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const data = JSON.parse(res.data);
            if ((data.code === "OK" || data.code === "SUCCESS") && data.data && data.data.url) {
              const resultUrl = data.data.url.startsWith("http")
                ? data.data.url
                : `${environment.apiBaseUrl}${data.data.url}`;
              resolve({
                id: Number(data.data.id),
                url: resultUrl,
                fileName: data.data.fileName,
              });
              return;
            }
            reject(new Error(data.message || "上传失败"));
          } catch (e) {
            reject(new Error("解析上传响应失败"));
          }
        } else {
          reject(new Error(`上传失败(${res.statusCode})`));
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || "网络异常，上传失败"));
      },
    });
  });
}

export function uploadImageFile(filePath: string, purpose: string = "IMAGE"): Promise<string> {
  return uploadDirectFile(filePath, purpose).then(res => res.url);
}

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
