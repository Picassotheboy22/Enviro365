import type { ProductResponse } from '../types'
import { formatRand } from './format'

export interface WithdrawalFormErrors {
  productId?: string
  amount?: string
}

// Digits with an optional 1-2 digit decimal part: "1500", "1500.5", "1500.50". No signs, commas or exponents.
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

/**
 * Converts Rands to whole cents, so comparisons are exact. JavaScript numbers are binary floating point
 * (0.1 + 0.2 !== 0.3), which can make "amount <= limit" give the wrong answer at the boundary.
 */
export function toCents(amount: number): number {
  return Math.round(amount * 100)
}

/**
 * Client-side checks for instant feedback while the user types.
 *
 * The backend re-checks everything when the form is submitted, so this is a convenience for the user,
 * not a security boundary. The limits (availableBalance, maxWithdrawalAmount, withdrawalAllowed) come from the
 * server's portfolio response, so the age, on-hold and 90% calculations are not duplicated here.
 */
export function validateWithdrawal(
  product: ProductResponse | undefined,
  amountText: string,
): WithdrawalFormErrors {
  const errors: WithdrawalFormErrors = {}

  if (!product) {
    errors.productId = 'Select a product.'
  } else if (!product.withdrawalAllowed) {
    errors.productId = product.restrictionReason ?? 'Withdrawals are not allowed from this product.'
  }

  const text = amountText.trim()
  if (text === '') {
    errors.amount = 'Enter an amount.'
    return errors
  }
  if (!AMOUNT_PATTERN.test(text)) {
    errors.amount = 'Enter a valid amount, e.g. 1500 or 1500.50 (at most 2 decimals).'
    return errors
  }

  const cents = toCents(Number(text))
  if (cents <= 0) {
    errors.amount = 'Amount must be greater than zero.'
  } else if (product?.withdrawalAllowed) {
    if (cents > toCents(product.availableBalance)) {
      errors.amount = `Amount exceeds the available balance of ${formatRand(product.availableBalance)}${onHoldNote(product)}.`
    } else if (cents > toCents(product.maxWithdrawalAmount)) {
      errors.amount = `You can withdraw at most 90% of the available balance: ${formatRand(product.maxWithdrawalAmount)}.`
    }
  }
  return errors
}

// Explains why the available balance is lower than the balance, with the same wording as the server.
function onHoldNote(product: ProductResponse): string {
  return product.heldAmount > 0
    ? ` (${formatRand(product.heldAmount)} is on hold for open withdrawal notices)`
    : ''
}

/** Filter dates are yyyy-MM-dd strings, and those compare correctly as plain strings. */
export function validateDateRange(from: string, to: string): string | undefined {
  if (from && to && from > to) {
    return "'From' date must be on or before 'To' date."
  }
  return undefined
}

export function hasErrors(errors: WithdrawalFormErrors): boolean {
  return Boolean(errors.productId || errors.amount)
}
