import type { LucideIcon } from 'lucide-react'
import { Card, CardAction, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'

export interface Stat {
  label: string
  value: string
  icon: LucideIcon
  footnote: string
}

/** A row of headline numbers, in the style of the shadcn/ui "dashboard-01" section cards. */
export function StatCards({ stats }: { stats: Stat[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {stats.map((stat) => (
        <Card key={stat.label} className="bg-linear-to-t from-primary/5 to-card shadow-xs">
          <CardHeader>
            <CardDescription>{stat.label}</CardDescription>
            <CardTitle className="text-2xl font-semibold tabular-nums">{stat.value}</CardTitle>
            <CardAction>
              <stat.icon className="size-5 text-muted-foreground" aria-hidden="true" />
            </CardAction>
          </CardHeader>
          <CardFooter className="text-sm text-muted-foreground">{stat.footnote}</CardFooter>
        </Card>
      ))}
    </div>
  )
}
