import { Link } from 'react-router'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { NoticeActions } from '@/components/withdrawals/notice-actions'
import { NoticeStatusBadge } from '@/components/withdrawals/notice-status-badge'
import type { WithdrawalResponse } from '@/types'
import { formatDateTime, formatRand, formatShortDate } from '@/utils/format'
import { statusHistory } from '@/utils/notices'

const COLUMNS = 5

/**
 * Staff: the notices waiting for them, each with the button for its next step. A compact version of the full table on
 * the Withdrawal notices page, so it fits next to the top clients on the dashboard.
 */
export function NoticeQueue({ notices }: { notices: WithdrawalResponse[] | undefined }) {
  let body
  if (!notices) {
    body = Array.from({ length: 3 }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={COLUMNS}>
          <Skeleton className="h-5 w-full" />
        </TableCell>
      </TableRow>
    ))
  } else if (notices.length === 0) {
    body = (
      <TableRow>
        <TableCell colSpan={COLUMNS} className="h-24 text-center text-muted-foreground">
          Nothing is waiting for you. New notices will appear here.
        </TableCell>
      </TableRow>
    )
  } else {
    body = notices.map((notice) => (
      <TableRow key={notice.id}>
        <TableCell className="text-muted-foreground" title={formatDateTime(notice.createdAt)}>
          {formatShortDate(notice.createdAt)}
        </TableCell>
        <TableCell>
          <Link to={`/clients/${notice.investorId}`} className="font-medium hover:underline">
            {notice.investorName}
          </Link>
          <div className="text-xs text-muted-foreground">{notice.productName}</div>
        </TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(notice.amount)}</TableCell>
        <TableCell>
          <NoticeStatusBadge status={notice.status} title={statusHistory(notice)} />
        </TableCell>
        <TableCell className="text-right">
          <NoticeActions notice={notice} />
        </TableCell>
      </TableRow>
    ))
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>Submitted</TableHead>
            <TableHead>Client</TableHead>
            <TableHead className="text-right">Amount</TableHead>
            <TableHead>Status</TableHead>
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
