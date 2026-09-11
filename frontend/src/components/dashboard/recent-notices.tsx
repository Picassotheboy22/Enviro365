import { Link } from 'react-router'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import type { WithdrawalResponse } from '@/types'
import { formatDateTime, formatRand } from '@/utils/format'

/** A compact list of the latest notices for the dashboard (the full table lives on the Withdrawal notices page). */
export function RecentNotices({ notices }: { notices: WithdrawalResponse[] | undefined }) {
  let body
  if (!notices) {
    body = Array.from({ length: 3 }, (_, index) => (
      <TableRow key={index}>
        <TableCell colSpan={4}>
          <Skeleton className="h-5 w-full" />
        </TableCell>
      </TableRow>
    ))
  } else if (notices.length === 0) {
    body = (
      <TableRow>
        <TableCell colSpan={4} className="h-24 text-center text-muted-foreground">
          No withdrawal notices yet.
        </TableCell>
      </TableRow>
    )
  } else {
    body = notices.map((notice) => (
      <TableRow key={notice.id}>
        <TableCell className="whitespace-nowrap text-muted-foreground">
          {formatDateTime(notice.createdAt)}
        </TableCell>
        <TableCell>
          <Link to={`/clients/${notice.investorId}`} className="font-medium hover:underline">
            {notice.investorName}
          </Link>
        </TableCell>
        <TableCell>{notice.productName}</TableCell>
        <TableCell className="text-right tabular-nums">{formatRand(notice.amount)}</TableCell>
      </TableRow>
    ))
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>Date</TableHead>
            <TableHead>Client</TableHead>
            <TableHead>Product</TableHead>
            <TableHead className="text-right">Amount</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>{body}</TableBody>
      </Table>
    </div>
  )
}
