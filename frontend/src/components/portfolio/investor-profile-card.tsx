import { CakeIcon, MailIcon, PhoneIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { InvestorDetails, ProductResponse } from '@/types'
import { formatDate } from '@/utils/format'

interface InvestorProfileCardProps {
  investor: InvestorDetails
  products: ProductResponse[]
}

export function InvestorProfileCard({ investor, products }: InvestorProfileCardProps) {
  const retirementProducts = products.filter((product) => product.type === 'RETIREMENT')
  const retirementEligible = retirementProducts.some((product) => product.withdrawalAllowed)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{investor.fullName}</CardTitle>
        <CardDescription>Investor profile</CardDescription>
      </CardHeader>
      <CardContent>
        <dl className="grid gap-3 text-sm">
          <div className="flex items-center gap-2">
            <MailIcon className="size-4 text-muted-foreground" aria-hidden="true" />
            <dt className="sr-only">Email</dt>
            <dd>{investor.email}</dd>
          </div>
          {investor.phone && (
            <div className="flex items-center gap-2">
              <PhoneIcon className="size-4 text-muted-foreground" aria-hidden="true" />
              <dt className="sr-only">Phone</dt>
              <dd>{investor.phone}</dd>
            </div>
          )}
          <div className="flex items-center gap-2">
            <CakeIcon className="size-4 text-muted-foreground" aria-hidden="true" />
            <dt className="sr-only">Date of birth</dt>
            <dd>
              {formatDate(investor.dateOfBirth)} (age {investor.age})
            </dd>
          </div>
          {retirementProducts.length > 0 && (
            <div className="flex flex-wrap items-center justify-between gap-2 border-t pt-3">
              <dt className="text-muted-foreground">Retirement withdrawals</dt>
              <dd>
                {retirementEligible ? (
                  <Badge>Eligible</Badge>
                ) : (
                  <Badge variant="destructive">Not eligible (65 or younger)</Badge>
                )}
              </dd>
            </div>
          )}
        </dl>
      </CardContent>
    </Card>
  )
}
