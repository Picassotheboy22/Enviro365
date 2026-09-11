import { describe, expect, it } from 'vitest'
import type { NoticeStatus, WithdrawalResponse } from '@/types'
import { availableActions, isOpen, oldestFirst, paidTotal, validateRejectionReason } from './notices'

function notice(
  id: number,
  status: NoticeStatus,
  amount: number,
  createdAt = '2026-09-01T09:00:00',
): WithdrawalResponse {
  return {
    id,
    investorId: 1,
    investorName: 'Thabo Mokoena',
    productId: 2,
    productName: 'Tax-Free Savings Account',
    productType: 'SAVINGS',
    amount,
    status,
    balanceBefore: null,
    balanceAfter: null,
    createdAt,
    reviewedBy: null,
    reviewedAt: null,
    rejectionReason: null,
    paidBy: null,
    paidAt: null,
    cancelledAt: null,
  }
}

describe('availableActions', () => {
  it('lets staff approve or reject pending notices and pay approved ones', () => {
    expect(availableActions('PENDING', 'ADMIN')).toEqual(['approve', 'reject'])
    expect(availableActions('APPROVED', 'ADMIN')).toEqual(['pay'])
  })

  it('lets investors cancel a notice only while it is pending', () => {
    expect(availableActions('PENDING', 'INVESTOR')).toEqual(['cancel'])
    expect(availableActions('APPROVED', 'INVESTOR')).toEqual([])
  })

  it.each<NoticeStatus>(['PAID', 'REJECTED', 'CANCELLED'])('offers nothing once a notice is %s', (status) => {
    expect(availableActions(status, 'ADMIN')).toEqual([])
    expect(availableActions(status, 'INVESTOR')).toEqual([])
  })
})

describe('isOpen', () => {
  it('treats pending and approved notices as open', () => {
    expect(isOpen('PENDING')).toBe(true)
    expect(isOpen('APPROVED')).toBe(true)
    expect(isOpen('PAID')).toBe(false)
    expect(isOpen('REJECTED')).toBe(false)
    expect(isOpen('CANCELLED')).toBe(false)
  })
})

describe('paidTotal', () => {
  it('adds up only the paid notices, in cents', () => {
    const total = paidTotal([
      notice(1, 'PAID', 0.1),
      notice(2, 'PAID', 0.2),
      notice(3, 'PENDING', 500),
      notice(4, 'REJECTED', 50),
    ])

    expect(total).toEqual({ count: 2, amount: 0.3 })
  })
})

describe('oldestFirst', () => {
  it('sorts by submission time, then id, without changing the input', () => {
    const input = [
      notice(3, 'PENDING', 1, '2026-09-03T09:00:00'),
      notice(2, 'APPROVED', 1, '2026-09-01T09:00:00'),
      notice(1, 'PENDING', 1, '2026-09-01T09:00:00'),
    ]

    expect(oldestFirst(input).map((n) => n.id)).toEqual([1, 2, 3])
    expect(input.map((n) => n.id)).toEqual([3, 2, 1])
  })
})

describe('validateRejectionReason', () => {
  it('requires a reason', () => {
    expect(validateRejectionReason('   ')).toMatch(/Give a reason/)
  })

  it('accepts up to 500 characters', () => {
    expect(validateRejectionReason('x'.repeat(500))).toBeUndefined()
    expect(validateRejectionReason('x'.repeat(501))).toMatch(/500 characters/)
  })
})
