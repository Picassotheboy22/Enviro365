// TypeScript mirrors of the backend DTOs (package com.enviro.assessment.junior.smsibi.dto).
// Keeping them in one file makes the API contract easy to see. Money values are Java BigDecimals on the server
// and arrive as plain JSON numbers.

export type ProductType = 'RETIREMENT' | 'SAVINGS'

export type Role = 'INVESTOR' | 'ADMIN'

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
  withdrawalCount: number
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
  /** 90% of the balance, or 0 when the product is restricted. Calculated by the server. */
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
  balanceBefore: number
  balanceAfter: number
  createdAt: string // ISO local date-time, e.g. "2026-09-10T14:03:00"
}

/** Optional filters for history and CSV export. Undefined fields are left out of the query string. */
export interface WithdrawalFilter {
  investorId?: number
  productId?: number
  from?: string // yyyy-MM-dd, inclusive
  to?: string // yyyy-MM-dd, inclusive
}

export interface ProductTypeTotal {
  type: ProductType
  productCount: number
  balance: number
}

export interface MonthlyWithdrawals {
  /** Calendar month as "yyyy-MM", e.g. "2026-09". */
  month: string
  noticeCount: number
  amount: number
}

/** GET /api/dashboard (staff only): statistics calculated on the server. */
export interface DashboardResponse {
  clientCount: number
  productCount: number
  assetsUnderManagement: number
  retirementEligibleClients: number
  noticeCount: number
  totalWithdrawn: number
  averageWithdrawal: number
  noticesLast30Days: number
  withdrawnLast30Days: number
  assetsByProductType: ProductTypeTotal[]
  /** The last six months, oldest first, including months without notices. */
  withdrawalsByMonth: MonthlyWithdrawals[]
}

/** RFC 9457 error body returned by the backend's GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  /** Business-rule code, e.g. "EXCEEDS_WITHDRAWAL_LIMIT" (422 responses only). */
  code?: string
  /** Field name -> message (400 validation responses only). */
  errors?: Record<string, string>
}
