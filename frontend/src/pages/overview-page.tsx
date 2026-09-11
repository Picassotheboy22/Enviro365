import { SendIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useCurrentUser } from '@/auth/auth-context'
import { PortfolioView } from '@/components/portfolio/portfolio-view'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { StaffDashboardPage } from '@/pages/staff-dashboard-page'

/** Home page: staff get the dashboard; an investor gets their own portfolio. */
export function OverviewPage() {
  const user = useCurrentUser()

  if (user.role === 'ADMIN') return <StaffDashboardPage />
  if (user.investorId === null) {
    return (
      <Alert>
        <AlertTitle>No portfolio</AlertTitle>
        <AlertDescription>This account is not linked to an investor portfolio.</AlertDescription>
      </Alert>
    )
  }

  return (
    <PortfolioView
      investorId={user.investorId}
      historyHref="/history"
      header={(investor) => ({
        title: `Welcome back, ${investor.firstName}`,
        description: 'Here is an overview of your investments.',
        actions: (
          <Button asChild>
            <Link to="/withdraw">
              <SendIcon />
              New withdrawal
            </Link>
          </Button>
        ),
      })}
    />
  )
}
