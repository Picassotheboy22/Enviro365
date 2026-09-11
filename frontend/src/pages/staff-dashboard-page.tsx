import { BanknoteIcon, InboxIcon, UsersIcon, WalletIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useDashboard, useInvestors, useWithdrawals } from '@/api/queries'
import { AssetsByTypeChart } from '@/components/dashboard/assets-by-type-chart'
import { NoticeQueue } from '@/components/dashboard/notice-queue'
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
import { OPEN_STATUSES, oldestFirst } from '@/utils/notices'

const QUEUE_SIZE = 6
const TOP_CLIENTS = 5

function plural(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`
}

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
      footnote: `Across ${plural(dashboard.productCount, 'product')}`,
    },
    {
      label: 'Waiting for staff',
      value: String(dashboard.awaitingApproval + dashboard.awaitingPayment),
      icon: InboxIcon,
      footnote: `${dashboard.awaitingApproval} to review, ${dashboard.awaitingPayment} to pay`,
    },
    {
      label: 'Paid out',
      value: formatRand(dashboard.totalWithdrawn),
      icon: BanknoteIcon,
      footnote: `${plural(dashboard.paidCount, 'notice')}, average ${formatRand(dashboard.averageWithdrawal)}`,
    },
  ]
}

/** Staff home page: the notices waiting for staff, plus statistics across all clients (GET /api/dashboard). */
export function StaffDashboardPage() {
  const dashboard = useDashboard()
  const clients = useInvestors(true)
  // For staff the server returns every client's notices; only the open ones are needed here.
  const openNotices = useWithdrawals({ status: OPEN_STATUSES })

  if (dashboard.isPending) return <PortfolioSkeleton />
  if (dashboard.isError) return <ErrorState error={dashboard.error} title="Could not load the dashboard" />

  const data = dashboard.data

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Enviro365 at a glance: the notices waiting for staff, clients, assets under management and payments."
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
            <CardTitle>Waiting for staff</CardTitle>
            <CardDescription>
              Open notices, oldest first. {formatRand(data.amountOnHold)} is on hold until they are paid,
              rejected or cancelled.
            </CardDescription>
            <CardAction>
              <Button variant="outline" size="sm" asChild>
                <Link to="/history?status=open">View all</Link>
              </Button>
            </CardAction>
          </CardHeader>
          <CardContent>
            {openNotices.isError ? (
              <ErrorState error={openNotices.error} title="Could not load withdrawal notices" />
            ) : (
              <NoticeQueue notices={openNotices.data && oldestFirst(openNotices.data).slice(0, QUEUE_SIZE)} />
            )}
          </CardContent>
        </Card>
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
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Paid out per month</CardTitle>
            <CardDescription>
              Payments over the last six months. Last 30 days: {formatRand(data.withdrawnLast30Days)} paid,{' '}
              {plural(data.noticesLast30Days, 'notice')} submitted.
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
    </>
  )
}
