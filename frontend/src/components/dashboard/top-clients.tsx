import { Link } from 'react-router'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'
import type { InvestorSummary } from '@/types'
import { formatPercent, topClientsByBalance } from '@/utils/dashboard'
import { formatRand } from '@/utils/format'

/** The clients with the largest balances, each with a bar showing their share of all assets. */
export function TopClients({
  clients,
  limit = 5,
}: {
  clients: InvestorSummary[] | undefined
  limit?: number
}) {
  if (!clients) {
    return (
      <div className="grid gap-4">
        {Array.from({ length: 3 }, (_, index) => (
          <Skeleton key={index} className="h-12 w-full" />
        ))}
      </div>
    )
  }
  if (clients.length === 0) {
    return <p className="text-sm text-muted-foreground">There are no clients yet.</p>
  }

  return (
    <ul className="grid gap-5">
      {topClientsByBalance(clients, limit).map(({ client, share }) => (
        <li key={client.id} className="grid gap-2">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <Link to={`/clients/${client.id}`} className="truncate font-medium hover:underline">
              {client.fullName}
            </Link>
            <span className="shrink-0 tabular-nums">{formatRand(client.totalBalance)}</span>
          </div>
          <Progress
            value={share * 100}
            aria-label={`${client.fullName} holds ${formatPercent(share)} of assets`}
          />
          <p className="text-xs text-muted-foreground">
            {formatPercent(share)} of assets · {client.withdrawalCount}{' '}
            {client.withdrawalCount === 1 ? 'notice' : 'notices'}
          </p>
        </li>
      ))}
    </ul>
  )
}
