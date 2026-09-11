import { Link } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { WithdrawalResponse } from '@/types'
import { formatDateTime, formatRand, productTypeLabel } from '@/utils/format'
import { toCents } from '@/utils/validation'

interface WithdrawalTableProps {
  rows: WithdrawalResponse[] | undefined
  loading: boolean
  emptyMessage: string
  showTotals?: boolean
  /** Staff views: add a Client column that links to the client's portfolio. */
  showClient?: boolean
}

export function WithdrawalTable({
  rows,
  loading,
  emptyMessage,
  showTotals = true,
  showClient = false,
}: WithdrawalTableProps) {
  const columns = showClient ? 8 : 7
  const totalCents = (rows ?? []).reduce((sum, row) => sum + toCents(row.amount), 0)

  let body
  if (loading && !rows) {
    body = Array.from({ length: 3 }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={columns}>
          <Skeleton className="h-5 w-full" />
        </TableCell>
      </TableRow>
    ))
  } else if (rows && rows.length > 0) {
    body = rows.map((row) => (
      <TableRow key={row.id}>
        <TableCell className="whitespace-nowrap">{formatDateTime(row.createdAt)}</TableCell>
        {showClient && (
          <TableCell>
            <Link to={`/clients/${row.investorId}`} className="font-medium hover:underline">
              {row.investorName}
            </Link>
          </TableCell>
        )}
        <TableCell className="font-medium">{row.productName}</TableCell>
        <TableCell>
          <Badge variant={row.productType === 'RETIREMENT' ? 'secondary' : 'outline'}>
            {productTypeLabel[row.productType]}
          </Badge>
        </TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(row.amount)}</TableCell>
        <TableCell className="text-right text-muted-foreground tabular-nums">
          {formatRand(row.balanceBefore)}
        </TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(row.balanceAfter)}</TableCell>
        <TableCell className="text-right text-muted-foreground">#{row.id}</TableCell>
      </TableRow>
    ))
  } else {
    body = (
      <TableRow>
        <TableCell colSpan={columns} className="h-24 text-center text-muted-foreground">
          {emptyMessage}
        </TableCell>
      </TableRow>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>Date</TableHead>
            {showClient && <TableHead>Client</TableHead>}
            <TableHead>Product</TableHead>
            <TableHead>Type</TableHead>
            <TableHead className="text-right">Amount</TableHead>
            <TableHead className="text-right">Balance before</TableHead>
            <TableHead className="text-right">Balance after</TableHead>
            <TableHead className="text-right">Ref</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>{body}</TableBody>
        {showTotals && rows && rows.length > 0 && (
          <TableFooter>
            <TableRow>
              <TableCell colSpan={showClient ? 4 : 3} className="font-medium">
                Total ({rows.length} {rows.length === 1 ? 'withdrawal' : 'withdrawals'})
              </TableCell>
              <TableCell className="text-right font-semibold tabular-nums">
                {formatRand(totalCents / 100)}
              </TableCell>
              <TableCell colSpan={3} />
            </TableRow>
          </TableFooter>
        )}
      </Table>
    </div>
  )
}
