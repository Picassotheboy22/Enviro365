import type { InvestorSummary } from '@/types'
import { toCents } from '@/utils/validation'

const shortMonth = new Intl.DateTimeFormat('en-ZA', { month: 'short' })
const longMonth = new Intl.DateTimeFormat('en-ZA', { month: 'long', year: 'numeric' })
const compactNumber = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 })
const percentage = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 1 })

/** "2026-09" as "Sept" (short, for chart axes) or "September 2026" (long, for tooltips). */
export function formatMonth(month: string, style: 'short' | 'long' = 'short'): string {
  const [year, monthNumber] = month.split('-').map(Number)
  const firstOfMonth = new Date(year, monthNumber - 1, 1)
  return (style === 'short' ? shortMonth : longMonth).format(firstOfMonth)
}

/** Short amounts for chart axes, e.g. "R 1.9M" or "R 25K". */
export function formatRandCompact(amount: number): string {
  return `R ${compactNumber.format(amount)}`
}

/** 0.4925 as "49.3%". */
export function formatPercent(fraction: number): string {
  return percentage.format(fraction)
}

/** part ÷ total, worked out in cents; 0 when the total is 0 (no division by zero). */
export function shareOf(part: number, total: number): number {
  const totalCents = toCents(total)
  return totalCents === 0 ? 0 : toCents(part) / totalCents
}

/** The clients with the largest balances, largest first, each with their share of all assets. */
export function topClientsByBalance(clients: InvestorSummary[], limit: number) {
  const total = clients.reduce((sum, client) => sum + toCents(client.totalBalance), 0) / 100
  return [...clients]
    .sort((a, b) => b.totalBalance - a.totalBalance)
    .slice(0, limit)
    .map((client) => ({ client, share: shareOf(client.totalBalance, total) }))
}
