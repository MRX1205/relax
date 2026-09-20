import { request } from "../../services/http";

export interface PaymentConfig {
  "wxpay.appId": string;
  "wxpay.mchId": string;
  "wxpay.serialNo": string;
  "wxpay.notifyUrl": string;
  "wxpay.enabled": string;
}

export interface PaymentResult {
  type: "WECHAT" | "MOCK";
  payParams?: Record<string, string>;
  message?: string;
}

export function getPaymentConfig(): Promise<PaymentConfig> {
  return request<PaymentConfig>({ url: "/api/v1/admin/payment-config" });
}

export function updatePaymentConfig(config: {
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
