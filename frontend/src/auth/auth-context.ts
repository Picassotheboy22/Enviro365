import { createContext, useContext } from 'react'
import type { CurrentUser } from '@/types'

export type AuthState =
  | { status: 'loading' }
  // signedOutByUser: the user clicked "Sign out" (as opposed to never having signed in, or the session expiring).
  // Used so the next person to sign in on this browser is not sent back to the previous user's last page.
  | { status: 'signed-out'; notice?: string; signedOutByUser?: boolean }
  | { status: 'signed-in'; user: CurrentUser }

export interface AuthContextValue {
  state: AuthState
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>')
  return value
}

/** For screens behind <RequireAuth>, where a signed-in user is guaranteed. */
export function useCurrentUser(): CurrentUser {
  const { state } = useAuth()
  if (state.status !== 'signed-in') throw new Error('useCurrentUser must be used on a signed-in page')
  return state.user
}
