import { request } from "../../services/http";

export interface PaymentConfig {
  "wxpay.app-id"?: string;
  "wxpay.mch-id"?: string;
  "wxpay.serial-no"?: string;
  "wxpay.notify-url"?: string;
  "wxpay.enabled"?: string;
  "payment.mode"?: string;
  [key: string]: any;
}

export interface PaymentModeInfo {
  mode: "MOCK" | "WXPAY" | "OFFLINE";
  label: string;
  description: string;
}

export interface PaymentResult {
  type: "WECHAT" | "MOCK" | "OFFLINE";
  payParams?: Record<string, string>;
  message?: string;
}

export function getPublicPaymentMode(): Promise<PaymentModeInfo> {
  return request<PaymentModeInfo>({ url: "/api/v1/payment/mode" });
}

export function getPaymentConfig(): Promise<PaymentConfig> {
  return request<PaymentConfig>({ url: "/api/v1/admin/payment-config" });
}

export function updatePaymentConfig(config: {
  paymentMode?: "MOCK" | "WXPAY" | "OFFLINE";
  appId?: string;
  mchId?: string;
  apiKey?: string;
  serialNo?: string;
  privateKey?: string;
  notifyUrl?: string;
  enabled?: boolean;
}): Promise<void> {
  return request<void>({ url: "/api/v1/admin/payment-config", method: "PUT", data: config });
}

export function createPayment(orderNo: string): Promise<PaymentResult> {
  return request<PaymentResult>({ url: `/api/v1/orders/${orderNo}/payment`, method: "POST" });
}

export function mockPay(paymentNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/payments/${paymentNo}/mock-pay`, method: "POST" });
}
