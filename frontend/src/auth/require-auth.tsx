import { Loader2Icon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '@/auth/auth-context'

/**
 * Route guard: signed-out users are sent to /login, which brings them back here after signing in.
 *
 * This only improves the experience. The real protection is on the server, which rejects every API call without a
 * valid session, so hiding a page in the browser is never the security boundary.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return (
      <div className="flex min-h-svh items-center justify-center gap-2 text-muted-foreground" role="status">
        <Loader2Icon className="size-4 animate-spin" />
        Loading…
      </div>
    )
  }
  if (state.status === 'signed-out') {
    // Remember where the user was heading (deep link, expired session), but not after a deliberate sign-out:
    // otherwise the next person to sign in on this browser would land on the previous user's last page.
    const returnTo = state.signedOutByUser ? undefined : { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={returnTo} />
  }
  return children
}
