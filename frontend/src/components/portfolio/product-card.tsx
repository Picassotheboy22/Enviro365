import { LockIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { ProductResponse } from '@/types'
import { formatRand, productTypeLabel } from '@/utils/format'

export function ProductCard({ product }: { product: ProductResponse }) {
  return (
    <Card className="shadow-xs">
      <CardHeader>
        <CardDescription>{product.name}</CardDescription>
        <CardTitle className="text-2xl tabular-nums">{formatRand(product.balance)}</CardTitle>
        <CardAction>
          <Badge variant={product.type === 'RETIREMENT' ? 'secondary' : 'outline'}>
            {productTypeLabel[product.type]}
          </Badge>
        </CardAction>
      </CardHeader>
      <CardContent className="text-sm">
        {product.withdrawalAllowed ? (
          <p className="text-muted-foreground">
            Available to withdraw:{' '}
            <span className="font-medium text-foreground tabular-nums">
              {formatRand(product.maxWithdrawalAmount)}
            </span>
          </p>
        ) : (
          <p className="flex items-start gap-2 text-destructive">
            <LockIcon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            {product.restrictionReason}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
