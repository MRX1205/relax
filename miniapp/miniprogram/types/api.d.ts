interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

interface HealthStatus {
  service: string;
  status: string;
  timestamp: string;
}

type RoleCode = "USER" | "TECHNICIAN" | "ADMIN" | "SUPER_ADMIN";
type AgreementType = "USER_AGREEMENT" | "PRIVACY_POLICY" | "TRANSACTION_RULES";
type FilePurpose = "AVATAR" | "TECHNICIAN_PHOTO" | "TECHNICIAN_CERTIFICATE" | "AFTER_SALE_EVIDENCE" | "SETTLEMENT_PROOF";

interface Account {
  id: string;
  nickname: string;
  avatarUrl: string | null;
  phone: string | null;
  status: "ACTIVE" | "DISABLED";
  lastRole: RoleCode;
  roles: RoleCode[];
  permissions: string[];
  permissionGroups: string[];
}

interface LoginResult {
  accessToken: string;
  expiresAt: string;
  account: Account;
}

interface Agreement {
  id: string;
  type: AgreementType;
  version: string;
  title: string;
  content: string;
  effectiveAt: string;
}

interface AgreementConsent {
  agreementId: string;
  type: AgreementType;
  version: string;
  context: "LOGIN" | "ORDER";
  agreedAt: string;
}

interface ServiceArea {
  id: string;
  regionId: string;
  regionCode: string;
  name: string;
  status: "ENABLED" | "DISABLED";
}

interface UserAddress {
  id: string;
  contactName: string;
  contactPhone: string;
  regionCode: string;
  regionName: string;
  detail: string;
  longitude: number;
  latitude: number;
  label: string | null;
  isDefault: boolean;
}

interface AddressInput {
  contactName: string;
  contactPhone: string;
  regionCode: string;
  detail: string;
  longitude: number;
  latitude: number;
  label: string;
  isDefault: boolean;
}

interface PermissionGroup {
  id: string;
  code: string;
  name: string;
  permissions: string[];
}

interface AccessUser {
  id: string;
  nickname: string;
  phone: string | null;
  status: "ACTIVE" | "DISABLED";
  roles: RoleCode[];
  permissionGroups: string[];
}

interface UploadGrant {
  method: "PUT";
  url: string;
  headers: Record<string, string>;
  expiresAt: string;
}

interface UploadPolicy {
  fileId: string;
  purpose: FilePurpose;
  mimeType: string;
  size: string;
  upload: UploadGrant;
}

interface FileAssetView {
  id: string;
  purpose: FilePurpose;
  fileName: string;
  mimeType: string;
  size: string;
  status: "PENDING" | "UPLOADED" | "READY" | "REJECTED";
}

interface RelaxAppOption {
  globalData: {
    account: Account | null;
  };
}

// === Phase 4 types ===
interface HomeData {
  categories: { id: string; name: string; sort: number }[];
  featuredProjects: { id: string; name: string; durationMinutes: number; basePrice: number; categoryName: string }[];
  nearbyTechnicians: { id: string; serviceName: string; avatarUrl: string | null; experienceYears: number; onlineStatus: string; startPrice: number | null }[];
}

interface ProjectBrief {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  durationMinutes: number;
  basePrice: number;
  description: string;
  notice: string;
  coverFileId: string | null;
  sort: number;
}

interface ProjectDetail {
  project: ProjectBrief;
  technicianCount: number;
  technicians: { id: string; serviceName: string; avatarUrl: string | null; experienceYears: number; price: number }[];
}

interface TechnicianItem {
  id: string;
  serviceName: string;
  avatarUrl: string | null;
  intro: string;
  experienceYears: number;
  onlineStatus: string;
  startPrice: number | null;
}

interface TechnicianDetail {
  technician: { id: string; serviceName: string; avatarUrl: string | null; intro: string; experienceYears: number; onlineStatus: string };
  projects: { id: string; projectId: string; projectName: string; durationMinutes: number; price: number }[];
  availability: { scheduleDate: string; startTime: string; endTime: string }[];
}

interface OrderPreview {
  projectId: string;
  projectName: string;
  durationMinutes: number;
  technicianId: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  addressSummary: string;
  projectAmount: number;
  travelFee: number;
  discountAmount: number;
  payableAmount: number;
}

// === Phase 5 types ===
interface OrderView {
  id: string;
  orderNo: string;
  userId: string;
  technicianId: string;
  projectId: string;
  status: string;
  serviceDate: string;
  startTime: string;
  endTime: string;
  version: number;
  note: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt?: string;
}

interface OrderDetailView {
  order: OrderView;
  projectSnapshot: { projectName: string; durationMinutes: number; basePrice: number; overridePrice: number | null; actualPrice: number } | null;
  addressSnapshot: { contactName: string; contactPhone: string; regionName: string; detail: string } | null;
  amount: { projectAmount: number; travelFee: number; discountAmount: number; payableAmount: number; paidAmount: number; refundedAmount: number } | null;
  statusLogs: { fromStatus: string | null; toStatus: string; operatorType: string; operatorId: string | null; reason: string | null; createdAt: string }[];
}

interface PaymentView {
  id: string;
  paymentNo: string;
  orderId: string;
  channel: string;
  amount: number;
  status: string;
  expireAt: string;
  paidAt: string | null;
  createdAt: string;
}

// === Phase 7 types ===
interface RefundView {
  id: string; refundNo: string; orderId: string; paymentId: string;
  amount: number; reason: string; status: string; operatorId: string | null;
  wechatRefundId: string | null; createdAt: string;
}

interface ReviewView {
  id: string; orderId: string; userId: string; technicianId: string;
  score: number; content: string; status: string; createdAt: string;
}

interface AfterSaleCase {
  id: string; caseNo: string; orderId: string; userId: string;
  type: string; content: string; status: string; assigneeId: string | null; createdAt: string;
}

// === Phase 8 types ===
interface IncomeView {
  id: string; orderId: string; technicianId: string;
  grossAmount: number; platformFee: number; payableAmount: number;
  status: string; createdAt: string;
}

interface SettlementView {
  id: string; settlementNo: string; technicianId: string;
  totalAmount: number; status: string; operatorId: string | null;
  paidAt: string | null; proofFileId: string | null;
  referenceNo: string | null; createdAt: string;
}

interface Banner {
  id: string; title: string; imageFileId: string | null;
  linkType: string | null; linkValue: string | null;
  sort: number; status: string;
}

interface StatsView {
  totalOrders: number; totalRevenue: number; totalUsers: number; totalTechnicians: number;
}
