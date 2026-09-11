import { describe, expect, it } from 'vitest'
import type { ProductResponse } from '../types'
import { hasErrors, toCents, validateDateRange, validateWithdrawal } from './validation'

const savings: ProductResponse = {
  id: 1,
  name: 'Unit Trust',
  type: 'SAVINGS',
  balance: 1000,
  maxWithdrawalAmount: 900,
  withdrawalAllowed: true,
  restrictionReason: null,
}

const restrictedRetirement: ProductResponse = {
  id: 2,
  name: 'Retirement Annuity',
  type: 'RETIREMENT',
  balance: 50000,
  maxWithdrawalAmount: 0,
  withdrawalAllowed: false,
  restrictionReason:
    'Retirement withdrawals are only allowed for investors older than 65. The investor is 40.',
}

describe('validateWithdrawal', () => {
  it('accepts a valid amount within the 90% limit', () => {
    expect(hasErrors(validateWithdrawal(savings, '500'))).toBe(false)
  })

  it('accepts exactly 90% of the balance (the limit is inclusive)', () => {
    expect(validateWithdrawal(savings, '900.00')).toEqual({})
  })

  it('rejects one cent over the 90% limit', () => {
    expect(validateWithdrawal(savings, '900.01').amount).toMatch(/at most 90%/)
  })

  it('rejects more than the balance with a balance-specific message', () => {
    expect(validateWithdrawal(savings, '1000.01').amount).toMatch(/exceeds the available balance/)
  })

  it('requires a product', () => {
    expect(validateWithdrawal(undefined, '100').productId).toBe('Select a product.')
  })

  it('shows the server-provided reason for a restricted retirement product', () => {
    expect(validateWithdrawal(restrictedRetirement, '100').productId).toMatch(/older than 65/)
  })

  it.each([
    ['', 'Enter an amount.'],
    ['   ', 'Enter an amount.'],
  ])('requires an amount (%j)', (input, message) => {
    expect(validateWithdrawal(savings, input).amount).toBe(message)
  })

  it.each(['abc', '-5', '1.234', '1,000', '1e3'])('rejects badly formatted amount %j', (input) => {
    expect(validateWithdrawal(savings, input).amount).toMatch(/valid amount/)
  })

  it.each(['0', '0.00'])('rejects zero (%j)', (input) => {
    expect(validateWithdrawal(savings, input).amount).toBe('Amount must be greater than zero.')
  })
})

describe('toCents', () => {
  it('avoids floating point surprises', () => {
    expect(toCents(0.1 + 0.2)).toBe(30)
    expect(toCents(1234.56)).toBe(123456)
  })
})

describe('validateDateRange', () => {
  it('allows open-ended and equal ranges', () => {
    expect(validateDateRange('', '')).toBeUndefined()
    expect(validateDateRange('2026-01-01', '')).toBeUndefined()
    expect(validateDateRange('2026-01-01', '2026-01-01')).toBeUndefined()
  })

  it('rejects a from date after the to date', () => {
    expect(validateDateRange('2026-02-01', '2026-01-01')).toMatch(/on or before/)
  })
})
