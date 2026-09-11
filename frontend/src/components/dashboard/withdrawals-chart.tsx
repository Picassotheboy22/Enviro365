import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from '@/components/ui/chart'
import type { MonthlyWithdrawals } from '@/types'
import { formatMonth, formatRandCompact } from '@/utils/dashboard'
import { formatRand } from '@/utils/format'

// shadcn/ui charts: the config gives each data series a label and a colour (exposed as the CSS var --color-amount).
const chartConfig = {
  amount: { label: 'Withdrawn', color: 'var(--primary)' },
} satisfies ChartConfig

interface ChartRow {
  month: string
  amount: number
  notices: number
}

/** Amount withdrawn per calendar month (bar chart), with the number of notices in the tooltip. */
export function WithdrawalsChart({ months }: { months: MonthlyWithdrawals[] }) {
  const data: ChartRow[] = months.map((month) => ({
    month: month.month,
    amount: month.amount,
    notices: month.noticeCount,
  }))

  return (
    <ChartContainer config={chartConfig} className="aspect-auto h-64 w-full">
      <BarChart accessibilityLayer data={data} margin={{ top: 8, left: 4, right: 4 }}>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey="month"
          tickLine={false}
          axisLine={false}
          tickMargin={8}
          tickFormatter={(month: string) => formatMonth(month)}
        />
        <YAxis
          tickLine={false}
          axisLine={false}
          width={64}
          tickFormatter={(amount: number) => formatRandCompact(amount)}
        />
        <ChartTooltip
          cursor={false}
          content={
            <ChartTooltipContent
              labelFormatter={(month) => formatMonth(String(month), 'long')}
              formatter={(value, _name, item) => {
                const row = item.payload as ChartRow
                return (
                  <div className="flex w-full items-center justify-between gap-4">
                    <span className="text-muted-foreground">
                      {row.notices} {row.notices === 1 ? 'notice' : 'notices'}
                    </span>
                    <span className="font-mono font-medium text-foreground tabular-nums">
                      {formatRand(Number(value))}
                    </span>
                  </div>
                )
              }}
            />
          }
        />
        <Bar dataKey="amount" fill="var(--color-amount)" radius={6} />
      </BarChart>
    </ChartContainer>
  )
}
