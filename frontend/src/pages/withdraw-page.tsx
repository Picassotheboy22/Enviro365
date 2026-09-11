import { CircleCheckIcon, InfoIcon } from 'lucide-react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { usePortfolio } from '@/api/queries'
import { useCurrentUser } from '@/auth/auth-context'
import { PageHeader } from '@/components/page-header'
import { ErrorState } from '@/components/query-state'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { WithdrawalForm } from '@/components/withdrawals/withdrawal-form'
import type { WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'

const RULES = [
  'Retirement products can only be withdrawn from if you are older than 65.',
  'A withdrawal cannot be more than the available balance: the balance minus amounts on hold for open notices.',
  'Each withdrawal is limited to 90% of the available balance.',
  'Amounts are in Rand, with at most 2 decimal places.',
]

const STEPS = [
  {
    title: 'Pending',
    text: 'The notice passes the rules and the amount is put on hold. You can still cancel it.',
  },
  {
    title: 'Approved',
    text: 'Enviro365 has reviewed it. If a notice is rejected instead, you will see the reason.',
  },
  { title: 'Paid', text: 'The money is paid out and deducted from your balance.' },
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
          <AlertTitle>Staff do not submit withdrawals</AlertTitle>
          <AlertDescription>
            Only the investor who owns a product can submit a withdrawal notice for it. Staff review, approve
            and pay notices on the Withdrawal notices page.
          </AlertDescription>
        </Alert>
      </>
    )
  }

  function handleSuccess(withdrawal: WithdrawalResponse) {
    toast.success(`Withdrawal notice #${withdrawal.id} submitted`, {
      description: `${formatRand(withdrawal.amount)} from ${withdrawal.productName} is on hold until Enviro365 reviews and pays it.`,
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
        description="Enviro365 reviews every notice before paying it. The amount is put on hold straight away and deducted from your balance once it is paid."
      />
      <div className="grid items-start gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle>Withdrawal details</CardTitle>
            <CardDescription>Choose a product and the amount to withdraw.</CardDescription>
          </CardHeader>
          <CardContent>{content}</CardContent>
        </Card>
        <div className="grid gap-6 lg:col-span-2">
          <Card>
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
          <Card>
            <CardHeader>
              <CardTitle>What happens next</CardTitle>
              <CardDescription>Follow each step on your History page.</CardDescription>
            </CardHeader>
            <CardContent>
              <ol className="grid gap-4 text-sm">
                {STEPS.map((step, index) => (
                  <li key={step.title} className="flex gap-3">
                    <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                      {index + 1}
                    </span>
                    <div>
                      <p className="font-medium">{step.title}</p>
                      <p className="text-muted-foreground">{step.text}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  )
}
