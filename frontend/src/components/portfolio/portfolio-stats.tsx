import { BanknoteIcon, LayersIcon, TrendingDownIcon, WalletIcon } from 'lucide-react'
import { StatCards, type Stat } from '@/components/stat-cards'
import type { PortfolioResponse, WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'
import { isOpen, paidTotal } from '@/utils/notices'
import { toCents } from '@/utils/validation'

interface PortfolioStatsProps {
  portfolio: PortfolioResponse
  withdrawals: WithdrawalResponse[] | undefined
}

/** Headline numbers for one portfolio. Sums are done in cents. */
export function PortfolioStats({ portfolio, withdrawals }: PortfolioStatsProps) {
  const { products } = portfolio
  const availableCents = products.reduce((sum, product) => sum + toCents(product.maxWithdrawalAmount), 0)
  const heldCents = products.reduce((sum, product) => sum + toCents(product.heldAmount), 0)
  const paid = paidTotal(withdrawals ?? [])
  const openCount = (withdrawals ?? []).filter((withdrawal) => isOpen(withdrawal.status)).length
  const eligible = products.filter((product) => product.withdrawalAllowed).length

  const stats: Stat[] = [
    {
      label: 'Total balance',
      value: formatRand(portfolio.totalBalance),
      icon: WalletIcon,
      footnote: `Across ${products.length} ${products.length === 1 ? 'product' : 'products'}`,
    },
    {
      label: 'Available to withdraw',
      value: formatRand(availableCents / 100),
      icon: BanknoteIcon,
      footnote: 'Up to 90% of each eligible balance not on hold',
    },
    {
      label: 'Paid out to date',
      value: withdrawals ? formatRand(paid.amount) : '…',
      icon: TrendingDownIcon,
      footnote:
        openCount > 0
          ? `${formatRand(heldCents / 100)} on hold for ${openCount} open ${openCount === 1 ? 'notice' : 'notices'}`
          : `${paid.count} paid ${paid.count === 1 ? 'notice' : 'notices'}, none open`,
    },
    {
      label: 'Eligible products',
      value: `${eligible} of ${products.length}`,
      icon: LayersIcon,
      footnote:
        eligible < products.length
          ? 'Retirement products need age over 65'
          : 'All products can be withdrawn from',
    },
  ]

  return <StatCards stats={stats} />
}
