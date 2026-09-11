import { ChevronRightIcon } from 'lucide-react'
import { Link } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import type { InvestorSummary } from '@/types'
import { formatDateTime, formatRand } from '@/utils/format'

const COLUMNS = 8

interface ClientsTableProps {
  clients: InvestorSummary[] | undefined
  loading: boolean
}

/** Staff: every client with their totals. The name and the "View" button both open the client's portfolio. */
export function ClientsTable({ clients, loading }: ClientsTableProps) {
  let body
  if (loading && !clients) {
    body = Array.from({ length: 3 }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={COLUMNS}>
          <Skeleton className="h-5 w-full" />
        </TableCell>
      </TableRow>
    ))
  } else if (clients && clients.length > 0) {
    body = clients.map((client) => (
      <TableRow key={client.id}>
        <TableCell>
          <Link to={`/clients/${client.id}`} className="font-medium hover:underline">
            {client.fullName}
          </Link>
          <div className="text-xs text-muted-foreground">{client.email}</div>
        </TableCell>
        <TableCell className="text-right tabular-nums">{client.age}</TableCell>
        <TableCell className="text-right tabular-nums">{client.productCount}</TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(client.totalBalance)}</TableCell>
        <TableCell className="text-right tabular-nums">
          {client.openNoticeCount > 0 && (
            <Badge variant="outline" className="mr-2">
              {client.openNoticeCount} open
            </Badge>
          )}
          {client.withdrawalCount}
        </TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(client.totalWithdrawn)}</TableCell>
        <TableCell className="whitespace-nowrap text-muted-foreground">
          {client.lastWithdrawalAt ? formatDateTime(client.lastWithdrawalAt) : 'None yet'}
        </TableCell>
        <TableCell className="text-right">
          <Button variant="ghost" size="sm" asChild>
            <Link to={`/clients/${client.id}`} aria-label={`View ${client.fullName}`}>
              View
              <ChevronRightIcon />
            </Link>
          </Button>
        </TableCell>
      </TableRow>
    ))
  } else {
    body = (
      <TableRow>
        <TableCell colSpan={COLUMNS} className="h-24 text-center text-muted-foreground">
          There are no clients yet.
        </TableCell>
      </TableRow>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>Client</TableHead>
            <TableHead className="text-right">Age</TableHead>
            <TableHead className="text-right">Products</TableHead>
            <TableHead className="text-right">Total balance</TableHead>
            <TableHead className="text-right">Notices</TableHead>
            <TableHead className="text-right">Paid out</TableHead>
            <TableHead>Last notice</TableHead>
            <TableHead className="text-right">
              <span className="sr-only">Actions</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>{body}</TableBody>
      </Table>
    </div>
  )
}
