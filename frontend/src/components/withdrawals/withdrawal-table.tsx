import { Link } from 'react-router'
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
import { NoticeActions } from '@/components/withdrawals/notice-actions'
import { NoticeStatusBadge } from '@/components/withdrawals/notice-status-badge'
import type { WithdrawalResponse } from '@/types'
import { formatDateTime, formatRand, productTypeLabel } from '@/utils/format'
import { isOpen, paidTotal, statusHistory } from '@/utils/notices'

interface WithdrawalTableProps {
  rows: WithdrawalResponse[] | undefined
  loading: boolean
  emptyMessage: string
  showTotals?: boolean
  /** Staff views: add a Client column that links to the client's portfolio. */
  showClient?: boolean
  /** Add the workflow buttons (approve, reject, pay or cancel, depending on the signed-in user's role). */
  showActions?: boolean
}

export function WithdrawalTable({
  rows,
  loading,
  emptyMessage,
  showTotals = true,
  showClient = false,
  showActions = false,
}: WithdrawalTableProps) {
  const columns = 7 + (showClient ? 1 : 0) + (showActions ? 1 : 0)
  const paid = paidTotal(rows ?? [])

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
        <TableCell>
          <div className="font-medium">{row.productName}</div>
          <div className="text-xs text-muted-foreground">{productTypeLabel[row.productType]}</div>
        </TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(row.amount)}</TableCell>
        <TableCell>
          <NoticeStatusBadge status={row.status} title={statusHistory(row)} />
          {row.rejectionReason && (
            <p className="mt-1 max-w-64 text-xs whitespace-normal text-muted-foreground">
              {row.rejectionReason}
            </p>
          )}
        </TableCell>
        {row.balanceBefore === null || row.balanceAfter === null ? (
          // The balance only changes when a notice is paid, so until then there are no balances to show.
          <TableCell colSpan={2} className="text-center text-xs text-muted-foreground">
            {isOpen(row.status) ? 'On hold until paid' : 'Nothing paid'}
          </TableCell>
        ) : (
          <>
            <TableCell className="text-right text-muted-foreground tabular-nums">
              {formatRand(row.balanceBefore)}
            </TableCell>
            <TableCell className="text-right tabular-nums">{formatRand(row.balanceAfter)}</TableCell>
          </>
        )}
        <TableCell className="text-right text-muted-foreground">#{row.id}</TableCell>
        {showActions && (
          <TableCell className="text-right">
            <NoticeActions notice={row} />
          </TableCell>
        )}
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
            <TableHead>Submitted</TableHead>
            {showClient && <TableHead>Client</TableHead>}
            <TableHead>Product</TableHead>
            <TableHead className="text-right">Amount</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Balance before</TableHead>
            <TableHead className="text-right">Balance after</TableHead>
            <TableHead className="text-right">Ref</TableHead>
            {showActions && (
              <TableHead className="text-right">
                <span className="sr-only">Actions</span>
              </TableHead>
            )}
          </TableRow>
        </TableHeader>
        <TableBody>{body}</TableBody>
        {showTotals && rows && rows.length > 0 && (
          <TableFooter>
            <TableRow>
              <TableCell colSpan={showClient ? 3 : 2} className="font-medium">
                Paid out ({paid.count} of {rows.length} {rows.length === 1 ? 'notice' : 'notices'})
              </TableCell>
              <TableCell className="text-right font-semibold tabular-nums">
                {formatRand(paid.amount)}
              </TableCell>
              <TableCell colSpan={columns - (showClient ? 4 : 3)} />
            </TableRow>
          </TableFooter>
        )}
      </Table>
    </div>
  )
}
