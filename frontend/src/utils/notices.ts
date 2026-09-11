import type { NoticeAction, NoticeStatus, Role, WithdrawalResponse } from '@/types'
import { formatDateTime } from '@/utils/format'
import { toCents } from '@/utils/validation'

export const noticeStatusLabel: Record<NoticeStatus, string> = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  PAID: 'Paid',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
}

/** Open notices still hold money on their product and are waiting for staff. */
export const OPEN_STATUSES: NoticeStatus[] = ['PENDING', 'APPROVED']

export function isOpen(status: NoticeStatus): boolean {
  return OPEN_STATUSES.includes(status)
}

/**
 * The workflow buttons to offer on a notice. This mirrors the server's rules, so the UI only offers steps that can
 * succeed. The server still checks every request.
 *
 *   Staff:     pending notices can be approved or rejected, approved ones marked as paid.
 *   Investors: pending notices can be cancelled (the server only ever returns their own notices).
 */
export function availableActions(status: NoticeStatus, role: Role): NoticeAction[] {
  if (role === 'ADMIN') {
    if (status === 'PENDING') return ['approve', 'reject']
    if (status === 'APPROVED') return ['pay']
    return []
  }
  return status === 'PENDING' ? ['cancel'] : []
}

/** How many of the notices are paid, and the total paid. Added up in cents to avoid floating point errors. */
export function paidTotal(notices: WithdrawalResponse[]): { count: number; amount: number } {
  const paid = notices.filter((notice) => notice.status === 'PAID')
  const cents = paid.reduce((sum, notice) => sum + toCents(notice.amount), 0)
  return { count: paid.length, amount: cents / 100 }
}

/** Oldest first: the order to work through a queue in. The server returns newest first. */
export function oldestFirst(notices: WithdrawalResponse[]): WithdrawalResponse[] {
  return [...notices].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id - b.id)
}

/** The steps a notice has been through, one per line. Shown as a tooltip on its status. */
export function statusHistory(notice: WithdrawalResponse): string {
  const lines = [`Submitted ${formatDateTime(notice.createdAt)}`]
  if (notice.reviewedAt) {
    const decision = notice.status === 'REJECTED' ? 'Rejected' : 'Approved'
    lines.push(`${decision} ${formatDateTime(notice.reviewedAt)} by ${notice.reviewedBy ?? 'staff'}`)
  }
  if (notice.paidAt) {
    lines.push(`Paid ${formatDateTime(notice.paidAt)} by ${notice.paidBy ?? 'staff'}`)
  }
  if (notice.cancelledAt) {
    lines.push(`Cancelled ${formatDateTime(notice.cancelledAt)}`)
  }
  return lines.join('\n')
}

export const REJECTION_REASON_MAX = 500

/** The investor is shown the rejection reason, so it has to say something. The server checks it again. */
export function validateRejectionReason(reason: string): string | undefined {
  const trimmed = reason.trim()
  if (trimmed === '') {
    return 'Give a reason. The investor will see it.'
  }
  if (trimmed.length > REJECTION_REASON_MAX) {
    return `Keep the reason to ${REJECTION_REASON_MAX} characters or fewer.`
  }
  return undefined
}
