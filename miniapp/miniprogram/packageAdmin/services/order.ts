import { request } from "../../services/http";

export function createOrder(data: {
  projectId: string;
  technicianId: string;
  addressId: string;
  serviceDate: string;
  startTime: string;
  note?: string;
  couponId?: string;
}): Promise<OrderDetailView> {
  return request<OrderDetailView>({ url: "/api/v1/orders", method: "POST", data });
}

export function getMyOrders(page = 0): Promise<OrderView[]> {
  return request<OrderView[]>({ url: `/api/v1/orders?page=${page}` });
}

export function getOrderDetail(orderNo: string): Promise<OrderDetailView> {
  return request<OrderDetailView>({ url: `/api/v1/orders/${orderNo}` });
}

export function cancelOrder(orderNo: string, reason?: string): Promise<void> {
  return request<void>({ url: `/api/v1/orders/${orderNo}/cancel`, method: "POST", data: { reason } });
}

export function createPayment(orderNo: string): Promise<PaymentView> {
  return request<PaymentView>({ url: `/api/v1/orders/${orderNo}/payments`, method: "POST" });
}

export function simulatePayment(paymentNo: string, scenario = "SUCCESS"): Promise<{ paymentNo: string; transactionId: string; result: string; message: string }> {
  return request({ url: `/api/v1/payments/${paymentNo}/simulate`, method: "POST", data: { scenario } });
}

// Admin
export function getAdminOrders(page = 0): Promise<OrderView[]> {
  return request<OrderView[]>({ url: `/api/v1/admin/orders?page=${page}` });
}

export function getAdminOrderDetail(orderNo: string): Promise<OrderDetailView> {
  return request<OrderDetailView>({ url: `/api/v1/admin/orders/${orderNo}` });
}

export function adminCancelOrder(orderNo: string, reason?: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/orders/${orderNo}/cancel`, method: "POST", data: { reason } });
}

export function updateOrderNote(orderNo: string, note: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/orders/${orderNo}/note`, method: "PUT", data: { note } });
}

// Technician
export function getTechOrders(page = 0): Promise<OrderView[]> {
  return request<OrderView[]>({ url: `/api/v1/technician/orders?page=${page}` });
}

export function getTechOrderDetail(orderNo: string): Promise<OrderDetailView> {
  return request<OrderDetailView>({ url: `/api/v1/technician/orders/${orderNo}` });
}

export function acceptOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/accept`, method: "POST" });
}

export function rejectOrder(orderNo: string, reason?: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/reject`, method: "POST", data: { reason } });
}

export function departOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/depart`, method: "POST" });
}

export function arriveOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/arrive`, method: "POST" });
}

export function startService(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/start`, method: "POST" });
}

export function completeService(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/technician/orders/${orderNo}/complete`, method: "POST" });
}

export function adminReassign(orderNo: string, newTechnicianId: string, reason?: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/orders/${orderNo}/reassign`, method: "POST", data: { newTechnicianId, reason } });
}

// Notifications
export interface Notification {
  id: string; userId: string; type: string; title: string; content: string;
  relatedOrderNo: string | null; readAt: string | null; createdAt: string;
}

export function getNotifications(page = 0): Promise<Notification[]> {
  return request<Notification[]>({ url: `/api/v1/notifications?page=${page}` });
}

export function markAllNotificationsRead(): Promise<number> {
  return request<number>({ url: "/api/v1/notifications/read-all", method: "POST" });
}

// Refund
export function requestRefund(orderNo: string, amount: number, reason: string): Promise<RefundView> {
  return request<RefundView>({ url: `/api/v1/orders/${orderNo}/refunds`, method: "POST", data: { amount, reason } });
}

export function getOrderRefunds(orderNo: string): Promise<RefundView[]> {
  return request<RefundView[]>({ url: `/api/v1/orders/${orderNo}/refunds` });
}

// Review
export function createReview(orderNo: string, score: number, content: string): Promise<ReviewView> {
  return request<ReviewView>({ url: `/api/v1/orders/${orderNo}/reviews`, method: "POST", data: { score, content } });
}

// After-sale
export function createAfterSale(orderNo: string, type: string, content: string): Promise<AfterSaleCase> {
  return request<AfterSaleCase>({ url: `/api/v1/orders/${orderNo}/after-sales`, method: "POST", data: { type, content } });
}

// Admin refunds
export function getAdminRefunds(page = 0): Promise<RefundView[]> {
  return request<RefundView[]>({ url: `/api/v1/admin/refunds?page=${page}` });
}

export function approveRefund(refundNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/refunds/${refundNo}/approve`, method: "POST" });
}

export function rejectRefund(refundNo: string, reason?: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/refunds/${refundNo}/reject`, method: "POST", data: { reason } });
}

// Technician income
export function getTechIncomes(page = 0): Promise<IncomeView[]> {
  return request<IncomeView[]>({ url: `/api/v1/technician/incomes?page=${page}` });
}

export function getTechSettlements(page = 0): Promise<SettlementView[]> {
  return request<SettlementView[]>({ url: `/api/v1/technician/settlements?page=${page}` });
}

// Admin settlements
export function getAdminSettlements(page = 0): Promise<SettlementView[]> {
  return request<SettlementView[]>({ url: `/api/v1/admin/settlements?page=${page}` });
}

export function markSettlementPaid(id: string, referenceNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/settlements/${id}/pay`, method: "POST", data: { referenceNo } });
}

export function voidSettlement(id: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/settlements/${id}/void`, method: "POST" });
}

// Banners
export function getBanners(): Promise<Banner[]> {
  return request<Banner[]>({ url: "/api/v1/banners" });
}

export function getAdminBanners(): Promise<Banner[]> {
  return request<Banner[]>({ url: "/api/v1/admin/banners" });
}

export function createBanner(data: {
  title: string;
  sort: number;
  imageFileId?: number | string | null;
  linkType?: string | null;
  linkValue?: string | null;
}): Promise<Banner> {
  return request<Banner>({ url: "/api/v1/admin/banners", method: "POST", data });
}

export function updateBanner(id: string, data: {
  title: string;
  sort: number;
  imageFileId?: number | string | null;
  linkType?: string | null;
  linkValue?: string | null;
}): Promise<Banner> {
  return request<Banner>({ url: `/api/v1/admin/banners/${id}`, method: "PUT", data });
}

export function updateBannerStatus(id: string, status: string): Promise<void> {
  return request<void>({ url: `/api/v1/admin/banners/${id}/status`, method: "PUT", data: { status } });
}

// Stats
export function getStats(): Promise<StatsView> {
  return request<StatsView>({ url: "/api/v1/admin/stats" });
}
