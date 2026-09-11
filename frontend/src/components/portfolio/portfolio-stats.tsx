import { BanknoteIcon, LayersIcon, TrendingDownIcon, WalletIcon } from 'lucide-react'
import { StatCards, type Stat } from '@/components/stat-cards'
import type { PortfolioResponse, WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'
import { toCents } from '@/utils/validation'

interface PortfolioStatsProps {
  portfolio: PortfolioResponse
  withdrawals: WithdrawalResponse[] | undefined
}

/** Headline numbers for one portfolio. Sums are done in cents. */
export function PortfolioStats({ portfolio, withdrawals }: PortfolioStatsProps) {
  const { products } = portfolio
  const availableCents = products.reduce((sum, product) => sum + toCents(product.maxWithdrawalAmount), 0)
  const withdrawnCents = (withdrawals ?? []).reduce((sum, withdrawal) => sum + toCents(withdrawal.amount), 0)
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
      footnote: 'Up to 90% of each eligible balance',
    },
    {
      label: 'Withdrawn to date',
      value: withdrawals ? formatRand(withdrawnCents / 100) : '…',
      icon: TrendingDownIcon,
      footnote: `${withdrawals?.length ?? 0} withdrawal notices`,
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
