import { LandmarkIcon, Loader2Icon } from 'lucide-react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '@/auth/auth-context'
import { LoginForm } from '@/components/login-form'
import { safeReturnPath } from '@/utils/navigation'

/** Layout from the shadcn/ui "login-03" block: a centred card on a muted background. */
export function LoginPage() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'signed-in') {
    // Go back to the page the user originally asked for (set by RequireAuth), or to the overview.
    const from = safeReturnPath((location.state as { from?: unknown } | null)?.from)
    return <Navigate to={from} replace />
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col gap-6">
        <div className="flex items-center gap-2 self-center font-medium">
          <div className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <LandmarkIcon className="size-4" />
          </div>
          Enviro365 Investments
        </div>
        {state.status === 'loading' ? (
          <div className="flex justify-center text-muted-foreground" role="status">
            <Loader2Icon className="size-5 animate-spin" />
          </div>
        ) : (
          <LoginForm notice={state.notice} />
        )}
      </div>
    </div>
  )
}
