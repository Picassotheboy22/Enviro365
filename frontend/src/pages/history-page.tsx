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
import type { NoticeStatus, WithdrawalFilter } from '@/types'
import { noticeStatusLabel, OPEN_STATUSES } from '@/utils/notices'
import { validateDateRange } from '@/utils/validation'

const ALL = 'all'
const OPEN = 'open'
const STATUSES: NoticeStatus[] = ['PENDING', 'APPROVED', 'PAID', 'REJECTED', 'CANCELLED']

/** The Status filter's value ("all", "open" or a single status) as the list the API expects. */
function statusesFor(value: string): NoticeStatus[] | undefined {
  if (value === OPEN) return OPEN_STATUSES
  const status = STATUSES.find((candidate) => candidate === value)
  return status ? [status] : undefined
}

/**
 * Investors: their own withdrawal notices. Staff: the notices of every client, optionally narrowed to one client, with
 * the buttons to approve, reject and pay them. The chosen client and status are kept in the URL (?investor=3,
 * ?status=open), so other pages can link straight to them and a refresh keeps the selection.
 */
export function HistoryPage() {
  const user = useCurrentUser()
  const isStaff = user.role === 'ADMIN'
  const [searchParams, setSearchParams] = useSearchParams()
  const clientId = isStaff ? (searchParams.get('investor') ?? ALL) : ALL
  const requestedStatus = searchParams.get('status') ?? ALL
  // An unknown value in a hand-typed URL falls back to "All statuses".
  const status = statusesFor(requestedStatus) ? requestedStatus : ALL

  function changeParam(name: 'investor' | 'status', value: string) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (value === ALL) {
        next.delete(name)
      } else {
        next.set(name, value)
      }
      return next
    })
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
      status={status}
      onClientChange={(value) => changeParam('investor', value)}
      onStatusChange={(value) => changeParam('status', value)}
      onClearUrlFilters={() => setSearchParams({})}
    />
  )
}

interface HistoryViewProps {
  isStaff: boolean
  investorId: number | undefined
  clientId: string
  status: string
  onClientChange: (value: string) => void
  onStatusChange: (value: string) => void
  onClearUrlFilters: () => void
}

function HistoryView({
  isStaff,
  investorId,
  clientId,
  status,
  onClientChange,
  onStatusChange,
  onClearUrlFilters,
}: HistoryViewProps) {
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
        status: statusesFor(status),
      }
  const withdrawals = useWithdrawals(filter)
  const hasFilters = clientId !== ALL || status !== ALL || productId !== ALL || from !== '' || to !== ''

  function clearFilters() {
    setProductId(ALL)
    setFrom('')
    setTo('')
    if (clientId !== ALL || status !== ALL) onClearUrlFilters()
  }

  return (
    <>
      <PageHeader
        title={isStaff ? 'Withdrawal notices' : 'Withdrawal history'}
        description={
          isStaff
            ? 'Every withdrawal notice from Enviro365 clients. Approve or reject pending notices, mark approved ones as paid, and download what you see as CSV.'
            : 'Follow each notice from submission to payment. Filter by status, product or date, then download exactly what you see as a CSV statement.'
        }
        actions={<CsvDownloadButton filter={filter ?? {}} disabled={!filter || !withdrawals.data?.length} />}
      />
      <Card>
        <CardContent className="grid gap-4">
          <div
            className={cn(
              'grid items-end gap-4 sm:grid-cols-2 lg:grid-cols-3',
              isStaff ? 'xl:grid-cols-6' : 'xl:grid-cols-5',
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
            <Field>
              <FieldLabel htmlFor="filter-status">Status</FieldLabel>
              <Select value={status} onValueChange={onStatusChange}>
                <SelectTrigger id="filter-status" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>All statuses</SelectItem>
                  <SelectItem value={OPEN}>Open (pending or approved)</SelectItem>
                  {STATUSES.map((option) => (
                    <SelectItem key={option} value={option}>
                      {noticeStatusLabel[option]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
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
            <ErrorState error={withdrawals.error} title="Could not load withdrawal notices" />
          ) : (
            <WithdrawalTable
              rows={withdrawals.data}
              loading={filter !== null && withdrawals.isPending}
              emptyMessage={hasFilters ? 'No notices match these filters.' : 'No withdrawal notices yet.'}
              showClient={isStaff}
              showActions
            />
          )}
        </CardContent>
      </Card>
    </>
  )
}
