import type { ProductType } from '../types'

const amountFormat = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const dateTimeFormat = new Intl.DateTimeFormat('en-ZA', { dateStyle: 'medium', timeStyle: 'short' })
const dateFormat = new Intl.DateTimeFormat('en-ZA', { dateStyle: 'long' })
const shortDateFormat = new Intl.DateTimeFormat('en-ZA', { day: 'numeric', month: 'short' })

/** "R 1,250.00". This matches the format of the backend's error messages, so the UI reads consistently. */
export function formatRand(amount: number): string {
  return `R ${amountFormat.format(amount)}`
}

/** Formats a backend LocalDateTime ("2026-09-10T14:03:00") in the user's local time zone. */
export function formatDateTime(isoDateTime: string): string {
  return dateTimeFormat.format(new Date(isoDateTime))
}

/** Formats a backend LocalDate ("1956-05-10"). Midnight local time is added so the day never shifts across time zones. */
export function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(`${isoDate}T00:00:00`))
}

/** A compact day and month ("8 Sept") for dense tables such as the dashboard queue. */
export function formatShortDate(isoDateTime: string): string {
  return shortDateFormat.format(new Date(isoDateTime))
}

export const productTypeLabel: Record<ProductType, string> = {
  RETIREMENT: 'Retirement',
  SAVINGS: 'Savings',
}
