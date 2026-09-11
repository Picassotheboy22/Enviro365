import { BanknoteIcon, CheckIcon, Loader2Icon, XIcon, type LucideIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { errorMessage } from '@/api/client'
import { useNoticeAction } from '@/api/queries'
import { useCurrentUser } from '@/auth/auth-context'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import type { NoticeAction, WithdrawalResponse } from '@/types'
import { formatRand } from '@/utils/format'
import { availableActions, REJECTION_REASON_MAX, validateRejectionReason } from '@/utils/notices'

type ConfirmedAction = Exclude<NoticeAction, 'reject'>

interface Confirmation {
  button: string
  title: string
  /** For the toast: "Notice #12 approved". */
  done: string
  icon: LucideIcon
  describe: (notice: WithdrawalResponse) => string
}

// Every step moves money or cannot be undone, so each one asks for confirmation and says what will happen.
const CONFIRMATIONS: Record<ConfirmedAction, Confirmation> = {
  approve: {
    button: 'Approve',
    title: 'Approve this notice?',
    done: 'approved',
    icon: CheckIcon,
    describe: (notice) =>
      `${formatRand(notice.amount)} from ${notice.investorName}'s ${notice.productName} stays on hold until you mark the notice as paid.`,
  },
  pay: {
    button: 'Mark as paid',
    title: 'Mark this notice as paid?',
    done: 'marked as paid',
    icon: BanknoteIcon,
    describe: (notice) =>
      `Confirm that ${formatRand(notice.amount)} has been paid to ${notice.investorName}. It will be deducted from the ${notice.productName} balance, and this cannot be undone.`,
  },
  cancel: {
    button: 'Cancel notice',
    title: 'Cancel this notice?',
    done: 'cancelled',
    icon: XIcon,
    describe: (notice) =>
      `The ${formatRand(notice.amount)} on hold on your ${notice.productName} becomes available again. You can submit a new notice at any time.`,
  },
}

/** The workflow buttons for one notice, for the signed-in user's role. Renders nothing when there is nothing to do. */
export function NoticeActions({ notice }: { notice: WithdrawalResponse }) {
  const user = useCurrentUser()
  const actions = availableActions(notice.status, user.role)
  if (actions.length === 0) return null

  return (
    <div className="flex justify-end gap-2">
      {actions.map((action) =>
        action === 'reject' ? (
          <RejectDialog key={action} notice={notice} />
        ) : (
          <ConfirmedActionButton key={action} notice={notice} action={action} />
        ),
      )}
    </div>
  )
}

function ConfirmedActionButton({ notice, action }: { notice: WithdrawalResponse; action: ConfirmedAction }) {
  const [open, setOpen] = useState(false)
  const mutation = useNoticeAction()
  const { button, title, done, icon: Icon, describe } = CONFIRMATIONS[action]

  async function confirm() {
    try {
      await mutation.mutateAsync({ action, id: notice.id })
      toast.success(`Notice #${notice.id} ${done}`)
    } catch (error) {
      toast.error(`Notice #${notice.id} was not ${done}`, { description: errorMessage(error) })
    } finally {
      setOpen(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <AlertDialogTrigger asChild>
        <Button
          size="sm"
          variant={action === 'cancel' ? 'outline' : 'default'}
          aria-label={`${button}: notice #${notice.id}`}
        >
          <Icon />
          {button}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{describe(notice)}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={mutation.isPending}>Go back</AlertDialogCancel>
          <AlertDialogAction
            disabled={mutation.isPending}
            onClick={(event) => {
              // Keep the dialog open until the server has answered.
              event.preventDefault()
              void confirm()
            }}
          >
            {mutation.isPending && <Loader2Icon className="animate-spin" />}
            {button}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

/** Staff: reject a pending notice. A reason is required, because the investor will see it. */
function RejectDialog({ notice }: { notice: WithdrawalResponse }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [touched, setTouched] = useState(false)
  const mutation = useNoticeAction()
  const error = validateRejectionReason(reason)
  const fieldId = `reject-reason-${notice.id}`

  function changeOpen(next: boolean) {
    setOpen(next)
    if (!next) {
      setReason('')
      setTouched(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTouched(true)
    if (error) return
    try {
      await mutation.mutateAsync({ action: 'reject', id: notice.id, reason: reason.trim() })
      toast.success(`Notice #${notice.id} rejected`, { description: 'The investor can see your reason.' })
      changeOpen(false)
    } catch (err) {
      toast.error(`Notice #${notice.id} was not rejected`, { description: errorMessage(err) })
    }
  }

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline" aria-label={`Reject: notice #${notice.id}`}>
          <XIcon />
          Reject
        </Button>
      </DialogTrigger>
      <DialogContent>
        <form noValidate onSubmit={(event) => void handleSubmit(event)} className="grid gap-4">
          <DialogHeader>
            <DialogTitle>Reject notice #{notice.id}?</DialogTitle>
            <DialogDescription>
              {formatRand(notice.amount)} from {notice.investorName}&apos;s {notice.productName}. The amount
              on hold becomes available again.
            </DialogDescription>
          </DialogHeader>
          <Field data-invalid={touched && Boolean(error)}>
            <FieldLabel htmlFor={fieldId}>Reason</FieldLabel>
            <Textarea
              id={fieldId}
              rows={4}
              maxLength={REJECTION_REASON_MAX}
              placeholder="For example: we could not verify your bank account details."
              value={reason}
              aria-invalid={touched && Boolean(error)}
              onChange={(event) => setReason(event.target.value)}
              onBlur={() => setTouched(true)}
            />
            {touched && error ? (
              <FieldError>{error}</FieldError>
            ) : (
              <FieldDescription>
                The investor will see this. {reason.trim().length}/{REJECTION_REASON_MAX} characters.
              </FieldDescription>
            )}
          </Field>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={mutation.isPending}>
                Go back
              </Button>
            </DialogClose>
            <Button type="submit" variant="destructive" disabled={mutation.isPending}>
              {mutation.isPending && <Loader2Icon className="animate-spin" />}
              Reject notice
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
