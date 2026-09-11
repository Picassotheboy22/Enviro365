// TypeScript mirrors of the backend DTOs (package com.enviro.assessment.junior.smsibi.dto).
// Keeping them in one file makes the API contract easy to see. Money values are Java BigDecimals on the server
// and arrive as plain JSON numbers.

export type ProductType = 'RETIREMENT' | 'SAVINGS'

export type Role = 'INVESTOR' | 'ADMIN'

/**
 * Where a withdrawal notice is in its workflow: submitted (PENDING), approved by staff (APPROVED), then paid out (PAID).
 * Staff can reject a pending notice, and the investor can cancel it.
 */
export type NoticeStatus = 'PENDING' | 'APPROVED' | 'PAID' | 'REJECTED' | 'CANCELLED'

/** The workflow steps, each a POST to /api/withdrawals/{id}/{action}. */
export type NoticeAction = 'approve' | 'reject' | 'pay' | 'cancel'

/** GET /api/auth/me */
export interface CurrentUser {
  username: string
  displayName: string
  role: Role
  /** The user's own investor id; null for staff accounts. */
  investorId: number | null
}

/** One row of the staff Clients overview (GET /api/investors). */
export interface InvestorSummary {
  id: number
  fullName: string
  email: string
  age: number
  productCount: number
  totalBalance: number
  /** Notices submitted, whatever their status. */
  withdrawalCount: number
  /** Notices that are still pending or approved. */
  openNoticeCount: number
  /** The total paid out. */
  totalWithdrawn: number
  /** When the latest withdrawal notice was submitted; null if there are none. */
  lastWithdrawalAt: string | null
}

export interface InvestorDetails {
  id: number
  firstName: string
  lastName: string
  fullName: string
  email: string
  phone: string | null
  dateOfBirth: string // ISO date, e.g. "1956-05-10"
  age: number
}

export interface ProductResponse {
  id: number
  name: string
  type: ProductType
  balance: number
  /** The total of the product's open (pending or approved) notices. */
  heldAmount: number
  /** The balance minus the held amount: what new notices can draw on. */
  availableBalance: number
  /** 90% of the available balance, or 0 when the product is restricted. Calculated by the server. */
  maxWithdrawalAmount: number
  withdrawalAllowed: boolean
  restrictionReason: string | null
}

export interface PortfolioResponse {
  investor: InvestorDetails
  products: ProductResponse[]
  totalBalance: number
}

export interface WithdrawalRequest {
  productId: number
  amount: number
}

export interface WithdrawalResponse {
  id: number
  investorId: number
  investorName: string
  productId: number
  productName: string
  productType: ProductType
  amount: number
  status: NoticeStatus
  /** The product balance just before and after the payment. Null until the notice is paid. */
  balanceBefore: number | null
  balanceAfter: number | null
  /** When the investor submitted the notice. ISO local date-time, e.g. "2026-09-10T14:03:00". */
  createdAt: string
  /** The staff member who approved or rejected the notice. */
  reviewedBy: string | null
  reviewedAt: string | null
  /** Why staff rejected the notice. The investor sees this. */
  rejectionReason: string | null
  paidBy: string | null
  paidAt: string | null
  cancelledAt: string | null
}

/** Optional filters for history and CSV export. Undefined fields are left out of the query string. */
export interface WithdrawalFilter {
  investorId?: number
  productId?: number
  from?: string // yyyy-MM-dd, inclusive
  to?: string // yyyy-MM-dd, inclusive
  /** Only these statuses. Sent as a repeated parameter: status=PENDING&status=APPROVED. */
  status?: NoticeStatus[]
}

export interface ProductTypeTotal {
  type: ProductType
  productCount: number
  balance: number
}

export interface MonthlyWithdrawals {
  /** Calendar month as "yyyy-MM", e.g. "2026-09". */
  month: string
  /** Notices paid in the month. */
  noticeCount: number
  /** The amount paid out in the month. */
  amount: number
}

/** GET /api/dashboard (staff only): statistics calculated on the server. */
export interface DashboardResponse {
  clientCount: number
  productCount: number
  assetsUnderManagement: number
  retirementEligibleClients: number
  /** Notices submitted, whatever their status. */
  noticeCount: number
  /** Pending notices, waiting for staff to approve or reject them. */
  awaitingApproval: number
  /** Approved notices, waiting to be paid. */
  awaitingPayment: number
  /** The total of all open (pending and approved) notices. */
  amountOnHold: number
  paidCount: number
  /** The total paid out. */
  totalWithdrawn: number
  /** The average paid notice. */
  averageWithdrawal: number
  /** Notices submitted in the last 30 days. */
  noticesLast30Days: number
  /** The amount paid out in the last 30 days. */
  withdrawnLast30Days: number
  assetsByProductType: ProductTypeTotal[]
  /** Payments in each of the last six months, oldest first, including months without payments. */
  withdrawalsByMonth: MonthlyWithdrawals[]
}

/** RFC 9457 error body returned by the backend's GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  /** Machine-readable code, e.g. "EXCEEDS_WITHDRAWAL_LIMIT" (422) or "INVALID_STATUS_TRANSITION" (409). */
  code?: string
  /** The notice's status when a workflow step was refused (409 responses only). */
  currentStatus?: NoticeStatus
  /** Field name -> message (400 validation responses only). */
  errors?: Record<string, string>
}
