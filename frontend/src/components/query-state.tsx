import { AlertCircleIcon } from 'lucide-react'
import { errorMessage } from '@/api/client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

/** Shown when loading data fails. The message comes from the server's Problem Details where possible. */
export function ErrorState({ error, title = 'Something went wrong' }: { error: unknown; title?: string }) {
  return (
    <Alert variant="destructive">
      <AlertCircleIcon />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>{errorMessage(error)}</AlertDescription>
    </Alert>
  )
}
