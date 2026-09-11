import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'

/** How often the figures on screen are refreshed while someone is using the app. */
export const LIVE_REFRESH_MS = 30_000

/** After this long without a click, key press or mouse movement, the user counts as away. */
export const IDLE_AFTER_MS = 5 * 60_000

const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'] as const

export function isActive(lastActivityAt: number, now: number): boolean {
  return now - lastActivityAt < IDLE_AFTER_MS
}

/**
 * Keeps the figures on screen current while other people change them: staff see new notices arrive, and an investor
 * sees their notice being approved and paid, without reloading the page.
 *
 * Every 30 seconds it marks the cached data as out of date, so the queries on the current page fetch again (TanStack
 * Query only re-renders when the data really changed). It only does this while the tab is visible and the user has done
 * something in the last five minutes. Refreshing an unattended screen for ever would also keep the server session
 * alive for ever and defeat the 30-minute idle sign-out.
 */
export function useLiveRefresh() {
  const queryClient = useQueryClient()

  useEffect(() => {
    let lastActivityAt = Date.now()
    const markActive = () => {
      lastActivityAt = Date.now()
    }
    ACTIVITY_EVENTS.forEach((event) => window.addEventListener(event, markActive, { passive: true }))

    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible' && isActive(lastActivityAt, Date.now())) {
        void queryClient.invalidateQueries()
      }
    }, LIVE_REFRESH_MS)

    return () => {
      window.clearInterval(timer)
      ACTIVITY_EVENTS.forEach((event) => window.removeEventListener(event, markActive))
    }
  }, [queryClient])
}
