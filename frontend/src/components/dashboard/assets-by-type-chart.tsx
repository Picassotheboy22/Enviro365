import { Label, Pie, PieChart } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from '@/components/ui/chart'
import type { ProductType, ProductTypeTotal } from '@/types'
import { formatPercent, formatRandCompact, shareOf } from '@/utils/dashboard'
import { formatRand, productTypeLabel } from '@/utils/format'

const chartConfig = {
  balance: { label: 'Balance' },
  RETIREMENT: { label: 'Retirement', color: 'var(--primary)' },
  SAVINGS: { label: 'Savings', color: 'oklch(0.765 0.177 163.223)' },
} satisfies ChartConfig

interface AssetsByTypeChartProps {
  totals: ProductTypeTotal[]
  /** Assets under management: the donut's centre figure and the base for each percentage. */
  total: number
}

/** Retirement vs savings share of assets under management (donut chart plus a legend with the figures). */
export function AssetsByTypeChart({ totals, total }: AssetsByTypeChartProps) {
  if (totals.length === 0) {
    return <p className="text-sm text-muted-foreground">There are no products yet.</p>
  }

  const data = totals.map((row) => ({
    type: row.type,
    balance: row.balance,
    fill: `var(--color-${row.type})`,
  }))

  return (
    <div className="grid gap-4">
      <ChartContainer config={chartConfig} className="mx-auto aspect-square h-52">
        <PieChart>
          <ChartTooltip
            cursor={false}
            content={
              <ChartTooltipContent
                hideLabel
                nameKey="type"
                formatter={(value, name) => (
                  <div className="flex w-full items-center justify-between gap-4">
                    <span className="text-muted-foreground">{productTypeLabel[name as ProductType]}</span>
                    <span className="font-mono font-medium text-foreground tabular-nums">
                      {formatRand(Number(value))}
                    </span>
                  </div>
                )}
              />
            }
          />
          <Pie data={data} dataKey="balance" nameKey="type" innerRadius={58} strokeWidth={4}>
            <Label
              content={({ viewBox }) =>
                viewBox && 'cx' in viewBox && 'cy' in viewBox ? (
                  <text x={viewBox.cx} y={viewBox.cy} textAnchor="middle" dominantBaseline="middle">
                    <tspan x={viewBox.cx} y={viewBox.cy} className="fill-foreground text-xl font-semibold">
                      {formatRandCompact(total)}
                    </tspan>
                    <tspan
                      x={viewBox.cx}
                      y={(viewBox.cy ?? 0) + 20}
                      className="fill-muted-foreground text-xs"
                    >
                      in total
                    </tspan>
                  </text>
                ) : null
              }
            />
          </Pie>
        </PieChart>
      </ChartContainer>

      <ul className="grid gap-2 text-sm">
        {totals.map((row) => (
          <li key={row.type} className="flex items-center gap-2">
            <span
              className="size-2.5 shrink-0 rounded-[2px]"
              style={{ backgroundColor: chartConfig[row.type].color }}
              aria-hidden="true"
            />
            <span className="font-medium">{productTypeLabel[row.type]}</span>
            <span className="text-muted-foreground">
              · {row.productCount} {row.productCount === 1 ? 'product' : 'products'}
            </span>
            <span className="ml-auto tabular-nums">{formatRand(row.balance)}</span>
            <span className="w-14 text-right text-muted-foreground tabular-nums">
              {formatPercent(shareOf(row.balance, total))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
