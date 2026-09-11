import { BanknoteIcon, FileTextIcon, UsersIcon, WalletIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useDashboard, useInvestors, useWithdrawals } from '@/api/queries'
import { AssetsByTypeChart } from '@/components/dashboard/assets-by-type-chart'
import { RecentNotices } from '@/components/dashboard/recent-notices'
import { TopClients } from '@/components/dashboard/top-clients'
import { WithdrawalsChart } from '@/components/dashboard/withdrawals-chart'
import { PageHeader } from '@/components/page-header'
import { PortfolioSkeleton } from '@/components/portfolio/portfolio-view'
import { ErrorState } from '@/components/query-state'
import { StatCards, type Stat } from '@/components/stat-cards'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { DashboardResponse } from '@/types'
import { formatRand } from '@/utils/format'

const RECENT_NOTICES = 6
const TOP_CLIENTS = 5

function headlineStats(dashboard: DashboardResponse): Stat[] {
  return [
    {
      label: 'Clients',
      value: String(dashboard.clientCount),
      icon: UsersIcon,
      footnote: `${dashboard.retirementEligibleClients} eligible for retirement withdrawals`,
    },
    {
      label: 'Assets under management',
      value: formatRand(dashboard.assetsUnderManagement),
      icon: WalletIcon,
      footnote: `Across ${dashboard.productCount} products`,
    },
    {
      label: 'Withdrawal notices',
      value: String(dashboard.noticeCount),
      icon: FileTextIcon,
      footnote: `${dashboard.noticesLast30Days} in the last 30 days`,
    },
    {
      label: 'Total withdrawn',
      value: formatRand(dashboard.totalWithdrawn),
      icon: BanknoteIcon,
      footnote: `Average ${formatRand(dashboard.averageWithdrawal)} per notice`,
    },
  ]
}

/** Staff home page: statistics across all clients, calculated by the server (GET /api/dashboard). */
export function StaffDashboardPage() {
  const dashboard = useDashboard()
  const clients = useInvestors(true)
  // No investorId filter: for staff the server returns every client's notices, newest first.
  const notices = useWithdrawals({})

  if (dashboard.isPending) return <PortfolioSkeleton />
  if (dashboard.isError) return <ErrorState error={dashboard.error} title="Could not load the dashboard" />

  const data = dashboard.data

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Enviro365 at a glance: clients, assets under management and withdrawal activity."
        actions={
          <Button variant="outline" asChild>
            <Link to="/clients">
              <UsersIcon />
              View clients
            </Link>
          </Button>
        }
      />

      <StatCards stats={headlineStats(data)} />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Withdrawals per month</CardTitle>
            <CardDescription>
              Amount withdrawn over the last six months. Last 30 days: {formatRand(data.withdrawnLast30Days)}.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <WithdrawalsChart months={data.withdrawalsByMonth} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Assets by product type</CardTitle>
            <CardDescription>Share of assets under management.</CardDescription>
          </CardHeader>
          <CardContent>
            <AssetsByTypeChart totals={data.assetsByProductType} total={data.assetsUnderManagement} />
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Top clients by balance</CardTitle>
            <CardDescription>Share of assets under management.</CardDescription>
          </CardHeader>
          <CardContent>
            {clients.isError ? (
              <ErrorState error={clients.error} title="Could not load clients" />
            ) : (
              <TopClients clients={clients.data} limit={TOP_CLIENTS} />
            )}
          </CardContent>
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Latest withdrawal notices</CardTitle>
            <CardDescription>The most recent notices from all clients.</CardDescription>
            <CardAction>
              <Button variant="outline" size="sm" asChild>
                <Link to="/history">View all</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent>
            {notices.isError ? (
              <ErrorState error={notices.error} title="Could not load withdrawal notices" />
            ) : (
              <RecentNotices notices={notices.data?.slice(0, RECENT_NOTICES)} />
            )}
          </CardContent>
        </Card>
      </div>
    </>
  )
}
