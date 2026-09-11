import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, onUnauthorized } from '@/api/client'
import { AuthContext, type AuthContextValue, type AuthState } from '@/auth/auth-context'

/**
 * Knows who is signed in.
 *
 * The session itself lives on the server, identified by an HttpOnly cookie that JavaScript cannot read. So on
 * start-up the app simply asks the server "who am I?" (GET /api/auth/me). No token or password is ever stored in
 * localStorage, where an XSS bug could steal it.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    let ignore = false
    api
      .currentUser()
      .then((user) => {
        if (!ignore) setState(user ? { status: 'signed-in', user } : { status: 'signed-out' })
      })
      .catch(() => {
        if (!ignore) setState({ status: 'signed-out' })
      })
    return () => {
      ignore = true
    }
  }, [])

  // A 401 from any API call means the session has gone (idle timeout, or signed out in another tab).
  useEffect(
    () =>
      onUnauthorized(() => {
        queryClient.clear()
        setState({ status: 'signed-out', notice: 'Your session has expired. Please sign in again.' })
      }),
    [queryClient],
  )

  const signIn = useCallback(
    async (username: string, password: string) => {
      const user = await api.signIn(username, password)
      // Drop anything cached for a previous user before showing this user's data.
      queryClient.clear()
      setState({ status: 'signed-in', user })
    },
    [queryClient],
  )

  const signOut = useCallback(async () => {
    try {
      await api.signOut()
    } finally {
      queryClient.clear()
      setState({ status: 'signed-out', signedOutByUser: true })
    }
  }, [queryClient])

  const value = useMemo<AuthContextValue>(() => ({ state, signIn, signOut }), [state, signIn, signOut])
  return <AuthContext value={value}>{children}</AuthContext>
}
