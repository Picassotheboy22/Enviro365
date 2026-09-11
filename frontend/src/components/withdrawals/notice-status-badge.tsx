import { BanIcon, CheckIcon, CircleCheckIcon, ClockIcon, XIcon, type LucideIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import type { NoticeStatus } from '@/types'
import { noticeStatusLabel } from '@/utils/notices'

// Every status has its own icon and label as well as a colour, so the status never depends on colour alone.
const STYLES: Record<NoticeStatus, { icon: LucideIcon; className: string }> = {
  PENDING: {
    icon: ClockIcon,
    className: 'border-amber-500/40 bg-amber-500/10 text-amber-700 dark:text-amber-400',
  },
  APPROVED: {
    icon: CheckIcon,
    className: 'border-sky-500/40 bg-sky-500/10 text-sky-700 dark:text-sky-400',
  },
  PAID: {
    icon: CircleCheckIcon,
    className: 'border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400',
  },
  REJECTED: {
    icon: XIcon,
    className: 'border-destructive/40 bg-destructive/10 text-destructive',
  },
  CANCELLED: {
    icon: BanIcon,
    className: 'text-muted-foreground',
  },
}

/** A notice's status. The optional title becomes a tooltip, e.g. the dates of each step. */
export function NoticeStatusBadge({ status, title }: { status: NoticeStatus; title?: string }) {
  const { icon: Icon, className } = STYLES[status]
  return (
    <Badge variant="outline" className={className} title={title}>
      <Icon aria-hidden="true" />
      {noticeStatusLabel[status]}
    </Badge>
  )
}
