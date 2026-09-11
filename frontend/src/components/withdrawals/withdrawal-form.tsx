import { Loader2Icon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { ApiError, errorMessage } from '@/api/client'
import { useCreateWithdrawal } from '@/api/queries'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { ProductResponse, WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'
import { hasErrors, toCents, validateWithdrawal, type WithdrawalFormErrors } from '@/utils/validation'

interface WithdrawalFormProps {
  products: ProductResponse[]
  onSuccess: (withdrawal: WithdrawalResponse) => void
}

/**
 * Validation happens at two levels:
 *  1. In the browser (validateWithdrawal) on every change, for instant feedback. A field's error only shows once that
 *     field has been touched, so the user isn't shown errors before they have typed anything.
 *  2. On the server when the form is submitted. Its messages (400 field errors, 422 rule violations) appear in the
 *     same places.
 */
export function WithdrawalForm({ products, onSuccess }: WithdrawalFormProps) {
  const [productId, setProductId] = useState('')
  const [amount, setAmount] = useState('')
  const [touched, setTouched] = useState({ productId: false, amount: false })
  const [serverErrors, setServerErrors] = useState<WithdrawalFormErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const mutation = useCreateWithdrawal()

  const product = products.find((p) => String(p.id) === productId)
  // Worked out from the current inputs on every render, so it can never be out of date.
  const clientErrors = validateWithdrawal(product, amount)
  const productError = serverErrors.productId ?? (touched.productId ? clientErrors.productId : undefined)
  const amountError = serverErrors.amount ?? (touched.amount ? clientErrors.amount : undefined)
  // What is left for new notices once this one holds its amount. The balance itself only changes on payment.
  const availableAfter =
    product && !hasErrors(clientErrors)
      ? (toCents(product.availableBalance) - toCents(Number(amount))) / 100
      : null

  function clearServerFeedback() {
    setServerErrors({})
    setFormError(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTouched({ productId: true, amount: true })
    clearServerFeedback()
    if (!product || hasErrors(clientErrors)) return

    try {
      const created = await mutation.mutateAsync({ productId: product.id, amount: Number(amount) })
      setAmount('')
      setTouched({ productId: false, amount: false })
      onSuccess(created)
    } catch (error) {
      if (error instanceof ApiError) {
        setServerErrors({ productId: error.fieldErrors.productId, amount: error.fieldErrors.amount })
      }
      setFormError(errorMessage(error))
    }
  }

  return (
    // noValidate: use our own messages instead of the browser's inconsistent built-in ones.
    <form noValidate onSubmit={(event) => void handleSubmit(event)}>
      <FieldGroup>
        {formError && (
          <Alert variant="destructive">
            <AlertTitle>Withdrawal not submitted</AlertTitle>
            <AlertDescription>{formError}</AlertDescription>
          </Alert>
        )}

        <Field data-invalid={Boolean(productError)}>
          <FieldLabel htmlFor="withdrawal-product">Product</FieldLabel>
          <Select
            value={productId}
            onValueChange={(value) => {
              setProductId(value)
              setTouched((current) => ({ ...current, productId: true }))
              clearServerFeedback()
            }}
          >
            <SelectTrigger id="withdrawal-product" className="w-full" aria-invalid={Boolean(productError)}>
              <SelectValue placeholder="Select a product" />
            </SelectTrigger>
            <SelectContent>
              {products.map((p) => (
                <SelectItem key={p.id} value={String(p.id)}>
                  {p.name} ·{' '}
                  {p.withdrawalAllowed ? `${formatRand(p.availableBalance)} available` : 'not eligible'}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {productError && <FieldError>{productError}</FieldError>}
        </Field>

        <Field data-invalid={Boolean(amountError)}>
          <FieldLabel htmlFor="withdrawal-amount">Amount (R)</FieldLabel>
          <div className="flex gap-2">
            {/* A text input with a decimal keypad: number inputs accept "e" and silently drop invalid input. */}
            <Input
              id="withdrawal-amount"
              inputMode="decimal"
              autoComplete="off"
              placeholder="0.00"
              value={amount}
              aria-invalid={Boolean(amountError)}
              onChange={(event) => {
                setAmount(event.target.value)
                clearServerFeedback()
              }}
              onBlur={() => setTouched((current) => ({ ...current, amount: true }))}
            />
            <Button
              type="button"
              variant="outline"
              className="text-foreground"
              disabled={!product?.withdrawalAllowed}
              onClick={() => {
                if (!product) return
                setAmount(product.maxWithdrawalAmount.toFixed(2))
                setTouched((current) => ({ ...current, amount: true }))
                clearServerFeedback()
              }}
            >
              Max
            </Button>
          </div>
          {amountError ? (
            <FieldError>{amountError}</FieldError>
          ) : (
            product?.withdrawalAllowed && (
              <FieldDescription>
                You can withdraw up to {formatRand(product.maxWithdrawalAmount)} (90% of the available{' '}
                {formatRand(product.availableBalance)}).
              </FieldDescription>
            )
          )}
        </Field>

        {product && availableAfter !== null && (
          <div className="grid gap-2 rounded-lg border border-dashed bg-muted/40 p-4 text-sm">
            <dl className="grid gap-1 tabular-nums">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Balance</dt>
                <dd>{formatRand(product.balance)}</dd>
              </div>
              {product.heldAmount > 0 && (
                <div className="flex justify-between">
                  <dt className="text-muted-foreground">On hold for open notices</dt>
                  <dd>− {formatRand(product.heldAmount)}</dd>
                </div>
              )}
              <div className="flex justify-between">
                <dt className="text-muted-foreground">This notice</dt>
                <dd>− {formatRand(Number(amount))}</dd>
              </div>
              <div className="mt-1 flex justify-between border-t pt-2 font-semibold">
                <dt>Available after this notice</dt>
                <dd>{formatRand(availableAfter)}</dd>
              </div>
            </dl>
            <p className="text-xs text-muted-foreground">
              The amount is put on hold now. Your balance changes when Enviro365 pays the notice.
            </p>
          </div>
        )}

        <Button type="submit" disabled={mutation.isPending} className="w-full sm:w-auto sm:self-start">
          {mutation.isPending && <Loader2Icon className="animate-spin" />}
          Submit withdrawal notice
        </Button>
      </FieldGroup>
    </form>
  )
}
