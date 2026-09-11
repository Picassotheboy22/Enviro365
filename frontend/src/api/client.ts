import type {
  CurrentUser,
  DashboardResponse,
  InvestorSummary,
  PortfolioResponse,
  ProblemDetail,
  WithdrawalFilter,
  WithdrawalRequest,
  WithdrawalResponse,
} from '@/types'

/**
 * Base URL of the Spring Boot API. Empty by default: in development Vite proxies `/api` to http://localhost:8080
 * (see vite.config.ts), so the browser only ever talks to one origin and the session cookie just works.
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

// Names used by Spring Security's cookie-based CSRF protection (csrf.spa() in SecurityConfig).
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

const OFFLINE_MESSAGE =
  'Cannot reach the server. Please check that the backend is running on http://localhost:8080.'

/** An error returned by the API, carrying the parsed Problem Details so the UI can show useful messages. */
export class ApiError extends Error {
  readonly status: number
  readonly title?: string
  readonly code?: string
  readonly fieldErrors: Record<string, string>

  constructor(
    status: number,
    message: string,
    options: { title?: string; code?: string; fieldErrors?: Record<string, string> } = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = options.title
    this.code = options.code
    this.fieldErrors = options.fieldErrors ?? {}
  }
}

/** A readable message for any error thrown while calling the API. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'An unexpected error occurred. Please try again.'
}

// ---------------------------------------------------------------------------------------------------------------------
// Session expiry: any 401 from a normal API call means the server-side session has gone (it timed out, or the user
// signed out in another tab). The AuthProvider subscribes here and sends the user back to the sign-in page.
// ---------------------------------------------------------------------------------------------------------------------

type Listener = () => void
const unauthorizedListeners = new Set<Listener>()

/** Subscribe to "the session has expired". Returns an unsubscribe function (handy as a useEffect cleanup). */
export function onUnauthorized(listener: Listener): () => void {
  unauthorizedListeners.add(listener)
  return () => {
    unauthorizedListeners.delete(listener)
  }
}

// ---------------------------------------------------------------------------------------------------------------------
// Low-level request helpers
// ---------------------------------------------------------------------------------------------------------------------

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : undefined
}

async function rawFetch(path: string, init: RequestInit = {}): Promise<Response> {
  try {
    // credentials: 'include' sends the session cookie, including when the API is on another origin.
    return await fetch(API_BASE + path, { ...init, credentials: 'include' })
  } catch {
    // fetch only rejects when no response arrived at all (server down, network error, CORS failure).
    throw new ApiError(0, OFFLINE_MESSAGE)
  }
}

/**
 * Spring Security expects the XSRF-TOKEN cookie value back in the X-XSRF-TOKEN header on every POST. If there is no
 * cookie yet (first visit, or it was replaced at sign-in or sign-out), ask the server to issue one first.
 */
async function csrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE)
  if (!token) {
    await rawFetch('/api/auth/csrf')
    token = readCookie(CSRF_COOKIE)
  }
  if (!token) {
    throw new ApiError(0, 'Could not get a security token from the server. Please refresh the page.')
  }
  return token
}

async function toApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (contentType.includes('json')) {
    const problem = (await response.json().catch(() => null)) as ProblemDetail | null
    if (problem) {
      const message = problem.detail ?? problem.title ?? `Request failed (${response.status}).`
      return new ApiError(response.status, message, {
        title: problem.title,
        code: problem.code,
        fieldErrors: problem.errors,
      })
    }
  }
  // Every backend error is JSON, so a non-JSON 5xx almost always means the Vite proxy could not reach it.
  return new ApiError(
    response.status,
    response.status >= 500 ? OFFLINE_MESSAGE : `Request failed (${response.status}).`,
  )
}

interface SendOptions {
  method?: 'GET' | 'POST'
  body?: BodyInit
  contentType?: string
  /** Set to false for auth calls, where a 401 is an expected answer rather than an expired session. */
  notifyOnUnauthorized?: boolean
}

async function send(path: string, options: SendOptions = {}, isRetry = false): Promise<Response> {
  const { method = 'GET', body, contentType, notifyOnUnauthorized = true } = options
  const headers = new Headers({ Accept: 'application/json' })
  if (contentType) headers.set('Content-Type', contentType)
  if (method !== 'GET') headers.set(CSRF_HEADER, await csrfToken())

  const response = await rawFetch(path, { method, body, headers })
  if (response.ok) return response

  const error = await toApiError(response)
  // An out-of-date CSRF token (e.g. replaced by a sign-in in another tab): get a fresh one and retry once.
  if (error.status === 403 && error.title === 'Invalid CSRF token' && !isRetry) {
    await rawFetch('/api/auth/csrf')
    return send(path, options, true)
  }
  if (error.status === 401 && notifyOnUnauthorized) {
    unauthorizedListeners.forEach((listener) => listener())
  }
  throw error
}

async function getJson<T>(path: string): Promise<T> {
  const response = await send(path)
  return (await response.json()) as T
}

/** Builds "?investorId=1&from=2026-01-01", leaving out empty filters. */
export function toQueryString(filter: WithdrawalFilter): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filter)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

function filenameFrom(contentDisposition: string | null, fallback: string): string {
  const match = contentDisposition?.match(/filename="?([^";]+)"?/)
  return match?.[1] ?? fallback
}

// ---------------------------------------------------------------------------------------------------------------------
// The API, one function per endpoint
// ---------------------------------------------------------------------------------------------------------------------

export const api = {
  /** The signed-in user, or null when there is no valid session. */
  currentUser: async (): Promise<CurrentUser | null> => {
    try {
      const response = await send('/api/auth/me', { notifyOnUnauthorized: false })
      return (await response.json()) as CurrentUser
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) return null
      throw error
    }
  },

  /** Spring Security's form login expects form-encoded fields (username, password), not JSON. */
  signIn: async (username: string, password: string): Promise<CurrentUser> => {
    await send('/api/auth/login', {
      method: 'POST',
      body: new URLSearchParams({ username, password }),
      notifyOnUnauthorized: false,
    })
    const user = await api.currentUser()
    if (!user) throw new ApiError(401, 'Sign-in did not complete. Please try again.')
    return user
  },

  signOut: async (): Promise<void> => {
    await send('/api/auth/logout', { method: 'POST', notifyOnUnauthorized: false })
  },

  listInvestors: () => getJson<InvestorSummary[]>('/api/investors'),

  getDashboard: () => getJson<DashboardResponse>('/api/dashboard'),

  getPortfolio: (investorId: number) => getJson<PortfolioResponse>(`/api/investors/${investorId}/portfolio`),

  listWithdrawals: (filter: WithdrawalFilter) =>
    getJson<WithdrawalResponse[]>(`/api/withdrawals${toQueryString(filter)}`),

  createWithdrawal: async (request: WithdrawalRequest): Promise<WithdrawalResponse> => {
    const response = await send('/api/withdrawals', {
      method: 'POST',
      body: JSON.stringify(request),
      contentType: 'application/json',
    })
    return (await response.json()) as WithdrawalResponse
  },

  /**
   * Downloads the CSV statement. Uses fetch plus a temporary link rather than a plain <a href>, so that if the server
   * returns an error the UI can show it instead of the browser opening an error page. Returns the file name.
   */
  downloadStatement: async (filter: WithdrawalFilter): Promise<string> => {
    const response = await send(`/api/withdrawals/export${toQueryString(filter)}`)
    const filename = filenameFrom(response.headers.get('Content-Disposition'), 'withdrawal-statement.csv')
    const url = URL.createObjectURL(await response.blob())

    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    // Give the browser a moment to start the download before releasing the in-memory file.
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    return filename
  },
}
