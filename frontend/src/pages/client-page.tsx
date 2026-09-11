import { Link, Navigate, useNavigate, useParams } from 'react-router'
import { useInvestors } from '@/api/queries'
import { useCurrentUser } from '@/auth/auth-context'
import { PortfolioView } from '@/components/portfolio/portfolio-view'
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

/** Staff: a read-only view of one client's portfolio, opened from the Clients overview (/clients/3). */
export function ClientPage() {
  const user = useCurrentUser()
  const isStaff = user.role === 'ADMIN'
  const clients = useInvestors(isStaff)
  const navigate = useNavigate()
  const investorId = Number(useParams().investorId)

  // Only staff have client pages. The server would refuse an investor anyway; this just avoids a pointless error.
  if (!isStaff || !Number.isInteger(investorId) || investorId <= 0) {
    return <Navigate to="/" replace />
  }

  return (
    <PortfolioView
      investorId={investorId}
      historyHref={`/history?investor=${investorId}`}
      header={(investor) => ({
        breadcrumb: (
          <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>
                <BreadcrumbLink asChild>
                  <Link to="/clients">Clients</Link>
                </BreadcrumbLink>
              </BreadcrumbItem>
              <BreadcrumbSeparator />
              <BreadcrumbItem>
                <BreadcrumbPage>{investor.fullName}</BreadcrumbPage>
              </BreadcrumbItem>
            </BreadcrumbList>
          </Breadcrumb>
        ),
        title: investor.fullName,
        description: "A read-only view of this client's portfolio and withdrawal notices.",
        actions: (
          <Select value={String(investorId)} onValueChange={(value) => void navigate(`/clients/${value}`)}>
            <SelectTrigger className="w-60" aria-label="Switch client">
              <SelectValue placeholder="Switch client" />
            </SelectTrigger>
            <SelectContent>
              {(clients.data ?? []).map((client) => (
                <SelectItem key={client.id} value={String(client.id)}>
                  {client.fullName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        ),
      })}
    />
  )
}
