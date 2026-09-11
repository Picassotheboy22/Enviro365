import { XIcon } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useInvestors, usePortfolio, useWithdrawals } from '@/api/queries'
import { useCurrentUser } from '@/auth/auth-context'
import { PageHeader } from '@/components/page-header'
import { ErrorState } from '@/components/query-state'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { CsvDownloadButton } from '@/components/withdrawals/csv-download-button'
import { WithdrawalTable } from '@/components/withdrawals/withdrawal-table'
import { cn } from '@/lib/utils'
import type { WithdrawalFilter } from '@/types'
import { validateDateRange } from '@/utils/validation'

const ALL = 'all'

/**
 * Investors: their own withdrawal history. Staff: the withdrawal notices of every client, optionally narrowed to one
 * client. The chosen client is kept in the URL (?investor=3), so a client page can link straight to that client's
 * notices and a refresh keeps the selection.
 */
export function HistoryPage() {
  const user = useCurrentUser()
  const isStaff = user.role === 'ADMIN'
  const [searchParams, setSearchParams] = useSearchParams()
  const clientId = isStaff ? (searchParams.get('investor') ?? ALL) : ALL

  function changeClient(value: string) {
    setSearchParams(value === ALL ? {} : { investor: value })
  }

  let investorId: number | undefined
  if (isStaff) {
    investorId = clientId === ALL ? undefined : Number(clientId)
  } else {
    // Investors always see only their own notices (the server enforces this as well).
    investorId = user.investorId ?? undefined
  }

  // key: choosing another client starts the other filters afresh, because a product belongs to a single client.
  return (
    <HistoryView
      key={clientId}
      isStaff={isStaff}
      investorId={investorId}
      clientId={clientId}
      onClientChange={changeClient}
    />
  )
}

interface HistoryViewProps {
  isStaff: boolean
  investorId: number | undefined
  clientId: string
  onClientChange: (value: string) => void
}

function HistoryView({ isStaff, investorId, clientId, onClientChange }: HistoryViewProps) {
  const clients = useInvestors(isStaff)
  // Only needed for the product list, which only makes sense once a single client is chosen.
  const portfolio = usePortfolio(investorId ?? null)
  const [productId, setProductId] = useState(ALL)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const dateRangeError = validateDateRange(from, to)
  // Don't send a request the server would reject anyway; the inline error explains why.
  const filter: WithdrawalFilter | null = dateRangeError
    ? null
    : {
        investorId,
        productId: productId === ALL ? undefined : Number(productId),
        from: from || undefined,
        to: to || undefined,
      }
  const withdrawals = useWithdrawals(filter)
  const hasFilters = clientId !== ALL || productId !== ALL || from !== '' || to !== ''

  function clearFilters() {
    setProductId(ALL)
    setFrom('')
    setTo('')
    if (clientId !== ALL) onClientChange(ALL)
  }

  return (
    <>
      <PageHeader
        title={isStaff ? 'Withdrawal notices' : 'Withdrawal history'}
        description={
          isStaff
            ? 'Every withdrawal notice submitted by Enviro365 clients. Filter by client, product or date, and download what you see as CSV.'
            : 'Filter by product or date range, then download exactly what you see as a CSV statement.'
        }
        actions={<CsvDownloadButton filter={filter ?? {}} disabled={!filter || !withdrawals.data?.length} />}
      />
      <Card>
        <CardContent className="grid gap-4">
          <div
            className={cn(
              'grid items-end gap-4 sm:grid-cols-2',
              isStaff ? 'lg:grid-cols-5' : 'lg:grid-cols-4',
            )}
            role="group"
            aria-label="Filters"
          >
            {isStaff && (
              <Field>
                <FieldLabel htmlFor="filter-client">Client</FieldLabel>
                <Select value={clientId} onValueChange={onClientChange}>
                  <SelectTrigger id="filter-client" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ALL}>All clients</SelectItem>
                    {(clients.data ?? []).map((client) => (
                      <SelectItem key={client.id} value={String(client.id)}>
                        {client.fullName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
            )}
            <Field data-disabled={investorId === undefined}>
              <FieldLabel htmlFor="filter-product">Product</FieldLabel>
              <Select value={productId} onValueChange={setProductId} disabled={investorId === undefined}>
                <SelectTrigger id="filter-product" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>
                    {investorId === undefined ? 'Pick a client first' : 'All products'}
                  </SelectItem>
                  {(portfolio.data?.products ?? []).map((product) => (
                    <SelectItem key={product.id} value={String(product.id)}>
                      {product.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
            <Field data-invalid={Boolean(dateRangeError)}>
              <FieldLabel htmlFor="filter-from">From</FieldLabel>
              <Input
                id="filter-from"
                type="date"
                value={from}
                max={to || undefined}
                aria-invalid={Boolean(dateRangeError)}
                onChange={(event) => setFrom(event.target.value)}
              />
            </Field>
            <Field data-invalid={Boolean(dateRangeError)}>
              <FieldLabel htmlFor="filter-to">To</FieldLabel>
              <Input
                id="filter-to"
                type="date"
                value={to}
                min={from || undefined}
                aria-invalid={Boolean(dateRangeError)}
                onChange={(event) => setTo(event.target.value)}
              />
            </Field>
            <Button variant="ghost" onClick={clearFilters} disabled={!hasFilters}>
              <XIcon />
              Clear filters
            </Button>
          </div>
          {dateRangeError && <FieldError>{dateRangeError}</FieldError>}

          {withdrawals.isError ? (
            <ErrorState error={withdrawals.error} title="Could not load withdrawals" />
          ) : (
            <WithdrawalTable
              rows={withdrawals.data}
              loading={filter !== null && withdrawals.isPending}
              emptyMessage={hasFilters ? 'No withdrawals match these filters.' : 'No withdrawals yet.'}
              showClient={isStaff}
            />
          )}
        </CardContent>
      </Card>
    </>
  )
}
