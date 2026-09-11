import { CircleCheckIcon, InfoIcon } from 'lucide-react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { usePortfolio } from '@/api/queries'
import { PageHeader } from '@/components/page-header'
import { ErrorState } from '@/components/query-state'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { WithdrawalForm } from '@/components/withdrawals/withdrawal-form'
import { useCurrentUser } from '@/auth/auth-context'
import type { WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'

const RULES = [
  'Retirement products can only be withdrawn from if you are older than 65.',
  'A withdrawal cannot be more than the product balance.',
  'Each withdrawal is limited to 90% of the product balance.',
  'Amounts are in Rand, with at most 2 decimal places.',
]

export function WithdrawPage() {
  const user = useCurrentUser()
  const isAdmin = user.role === 'ADMIN'
  const portfolio = usePortfolio(isAdmin ? null : user.investorId)
  const navigate = useNavigate()

  if (isAdmin) {
    return (
      <>
        <PageHeader title="New withdrawal" />
        <Alert>
          <InfoIcon />
          <AlertTitle>Staff accounts are read-only</AlertTitle>
          <AlertDescription>
            Only the investor who owns a product can submit a withdrawal notice for it.
          </AlertDescription>
        </Alert>
      </>
    )
  }

  function handleSuccess(withdrawal: WithdrawalResponse) {
    toast.success(`Withdrawal notice #${withdrawal.id} submitted`, {
      description: `${formatRand(withdrawal.amount)} from ${withdrawal.productName}. New balance: ${formatRand(withdrawal.balanceAfter)}.`,
      action: { label: 'View history', onClick: () => void navigate('/history') },
    })
  }

  let content
  if (portfolio.isPending) {
    content = (
      <div className="grid gap-4">
        <Skeleton className="h-9 w-full" />
        <Skeleton className="h-9 w-full" />
      </div>
    )
  } else if (portfolio.isError) {
    content = <ErrorState error={portfolio.error} title="Could not load your products" />
  } else {
    content = <WithdrawalForm products={portfolio.data.products} onSuccess={handleSuccess} />
  }

  return (
    <>
      <PageHeader
        title="New withdrawal notice"
        description="The amount is deducted from the product balance as soon as the notice is accepted."
      />
      <div className="grid items-start gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle>Withdrawal details</CardTitle>
            <CardDescription>Choose a product and the amount to withdraw.</CardDescription>
          </CardHeader>
          <CardContent>{content}</CardContent>
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Withdrawal rules</CardTitle>
            <CardDescription>Checked in your browser as you type, and again by the server.</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="grid gap-3 text-sm">
              {RULES.map((rule) => (
                <li key={rule} className="flex gap-2">
                  <CircleCheckIcon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
                  {rule}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
