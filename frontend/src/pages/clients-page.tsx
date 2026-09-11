import { Navigate } from 'react-router'
import { useInvestors } from '@/api/queries'
import { useCurrentUser } from '@/auth/auth-context'
import { ClientsTable } from '@/components/clients/clients-table'
import { PageHeader } from '@/components/page-header'
import { ErrorState } from '@/components/query-state'
import { Card, CardContent } from '@/components/ui/card'

/** Staff: every client with their totals. Opening a client shows their portfolio and withdrawal notices. */
export function ClientsPage() {
  const user = useCurrentUser()
  const isStaff = user.role === 'ADMIN'
  const clients = useInvestors(isStaff)

  // Only staff have a client list. The server would refuse an investor anyway; this just avoids a pointless error.
  if (!isStaff) return <Navigate to="/" replace />

  return (
    <>
      <PageHeader
        title="Clients"
        description="Every Enviro365 investor. Open a client to see their portfolio and withdrawal notices."
      />
      <Card>
        <CardContent>
          {clients.isError ? (
            <ErrorState error={clients.error} title="Could not load clients" />
          ) : (
            <ClientsTable clients={clients.data} loading={clients.isPending} />
          )}
        </CardContent>
      </Card>
    </>
  )
}
