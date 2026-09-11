import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { usePortfolio, useWithdrawals } from '@/api/queries'
import { PageHeader } from '@/components/page-header'
import { InvestorProfileCard } from '@/components/portfolio/investor-profile-card'
import { PortfolioStats } from '@/components/portfolio/portfolio-stats'
import { ProductCard } from '@/components/portfolio/product-card'
import { ErrorState } from '@/components/query-state'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { WithdrawalTable } from '@/components/withdrawals/withdrawal-table'
import type { InvestorDetails } from '@/types'

interface PortfolioHeader {
  breadcrumb?: ReactNode
  title: string
  description: string
  actions?: ReactNode
}

interface PortfolioViewProps {
  investorId: number
  /** Builds the page heading once the investor has loaded (e.g. "Welcome back, Thabo"). */
  header: (investor: InvestorDetails) => PortfolioHeader
  /** Where "View all" under the recent notices leads. */
  historyHref: string
}

/**
 * One investor's portfolio: headline numbers, profile, products and latest notices. Shared by the investor's own
 * overview and the staff client page, so both always show the same information. The notice buttons adapt to the
 * signed-in user: staff can approve, reject and pay, and the investor can cancel a pending notice.
 */
export function PortfolioView({ investorId, header, historyHref }: PortfolioViewProps) {
  const portfolio = usePortfolio(investorId)
  const withdrawals = useWithdrawals({ investorId })

  if (portfolio.isPending) return <PortfolioSkeleton />
  if (portfolio.isError) return <ErrorState error={portfolio.error} title="Could not load the portfolio" />

  const { investor, products } = portfolio.data
  const { breadcrumb, title, description, actions } = header(investor)

  return (
    <>
      {breadcrumb}
      <PageHeader title={title} description={description} actions={actions} />

      <PortfolioStats portfolio={portfolio.data} withdrawals={withdrawals.data} />

      <div className="grid gap-6 xl:grid-cols-3">
        <InvestorProfileCard investor={investor} products={products} />
        <section className="grid gap-4 sm:grid-cols-2 xl:col-span-2" aria-label="Products">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </section>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent withdrawal notices</CardTitle>
          <CardDescription>The latest notices on this portfolio and where each one is.</CardDescription>
          <CardAction>
            <Button variant="outline" size="sm" asChild>
              <Link to={historyHref}>View all</Link>
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          {withdrawals.isError ? (
            <ErrorState error={withdrawals.error} title="Could not load withdrawal notices" />
          ) : (
            <WithdrawalTable
              rows={withdrawals.data?.slice(0, 5)}
              loading={withdrawals.isPending}
              emptyMessage="No withdrawal notices yet."
              showTotals={false}
              showActions
            />
          )}
        </CardContent>
      </Card>
    </>
  )
}

export function PortfolioSkeleton() {
  return (
    <div className="grid gap-6" role="status" aria-label="Loading">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-32 rounded-xl" />
        ))}
      </div>
      <Skeleton className="h-64 rounded-xl" />
    </div>
  )
}
