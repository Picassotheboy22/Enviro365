import { describe, expect, it } from 'vitest'
import type { InvestorSummary } from '../types'
import { formatMonth, formatPercent, formatRandCompact, shareOf, topClientsByBalance } from './dashboard'

function client(id: number, totalBalance: number): InvestorSummary {
  return {
    id,
    fullName: `Client ${id}`,
    email: `client${id}@example.com`,
    age: 40,
    productCount: 1,
    totalBalance,
    withdrawalCount: 0,
    openNoticeCount: 0,
    totalWithdrawn: 0,
    lastWithdrawalAt: null,
  }
}

describe('formatMonth', () => {
  it('formats a "yyyy-MM" month for axes and tooltips', () => {
    expect(formatMonth('2026-01')).toBe('Jan')
    expect(formatMonth('2026-09', 'long')).toBe('September 2026')
  })
})

describe('formatRandCompact / formatPercent', () => {
  it('shortens large amounts for chart axes', () => {
    expect(formatRandCompact(1_858_000)).toBe('R 1.9M')
    expect(formatRandCompact(25_000)).toBe('R 25K')
    expect(formatRandCompact(0)).toBe('R 0')
  })

  it('formats fractions as percentages', () => {
    expect(formatPercent(0.4925)).toBe('49.3%')
  })
})

describe('shareOf', () => {
  it('divides in cents and never divides by zero', () => {
    expect(shareOf(0.1, 0.3)).toBeCloseTo(1 / 3)
    expect(shareOf(50, 0)).toBe(0)
  })
})

describe('topClientsByBalance', () => {
  it('returns the largest balances first, limited, with their share of all assets', () => {
    const top = topClientsByBalance([client(1, 100), client(2, 300), client(3, 600)], 2)

    expect(top.map(({ client }) => client.id)).toEqual([3, 2])
    expect(top.map(({ share }) => share)).toEqual([0.6, 0.3])
  })

  it('does not change the order of the list it was given', () => {
    const clients = [client(1, 100), client(2, 300)]
    topClientsByBalance(clients, 5)

    expect(clients.map(({ id }) => id)).toEqual([1, 2])
  })
})
