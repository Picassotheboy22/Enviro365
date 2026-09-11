import { AlertCircleIcon, Loader2Icon } from 'lucide-react'
import { useState, type ComponentProps, type FormEvent } from 'react'
import { errorMessage } from '@/api/client'
import { useAuth } from '@/auth/auth-context'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldDescription, FieldGroup, FieldLabel, FieldSeparator } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

// Seeded by the backend's DataSeeder in the dev profile only. Shown only in development builds (import.meta.env.DEV).
const DEMO_PASSWORD = 'Enviro365!'
const DEMO_ACCOUNTS = [
  { username: 'thabo.mokoena@example.com', label: 'Thabo, 70: retirement withdrawals allowed' },
  { username: 'sipho.ndlovu@example.com', label: 'Sipho, exactly 65: retirement blocked' },
  { username: 'lerato.dlamini@example.com', label: 'Lerato, 40: savings only' },
  { username: 'admin@enviro365.example', label: 'Enviro365 staff: reviews and pays notices' },
]

type LoginFormProps = ComponentProps<'div'> & {
  /** A message to show above the form, e.g. "Your session has expired". */
  notice?: string
}

/** Adapted from the shadcn/ui "login-03" block. */
export function LoginForm({ notice, className, ...props }: LoginFormProps) {
  const { signIn } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!username.trim() || !password) {
      setError('Enter your email address and password.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      // On success the AuthProvider switches to "signed in" and the login page redirects away.
      await signIn(username.trim(), password)
    } catch (err) {
      setError(errorMessage(err))
      setPassword('')
      setSubmitting(false)
    }
  }

  const message = error ?? notice

  return (
    <div className={cn('flex flex-col gap-6', className)} {...props}>
      <Card>
        <CardHeader className="text-center">
          <CardTitle className="text-xl">Welcome back</CardTitle>
          <CardDescription>Sign in to manage your investments and withdrawal notices</CardDescription>
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={(event) => void handleSubmit(event)}>
            <FieldGroup>
              {message && (
                <Alert variant="destructive">
                  <AlertCircleIcon />
                  <AlertTitle>{error ? 'Sign-in failed' : 'Signed out'}</AlertTitle>
                  <AlertDescription>{message}</AlertDescription>
                </Alert>
              )}
              <Field>
                <FieldLabel htmlFor="username">Email</FieldLabel>
                <Input
                  id="username"
                  type="email"
                  autoComplete="username"
                  placeholder="you@example.com"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  required
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="password">Password</FieldLabel>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </Field>
              <Field>
                <Button type="submit" disabled={submitting}>
                  {submitting && <Loader2Icon className="animate-spin" />}
                  Sign in
                </Button>
                <FieldDescription className="text-center">
                  Forgot your password? Contact your Enviro365 consultant.
                </FieldDescription>
              </Field>

              {import.meta.env.DEV && (
                <>
                  <FieldSeparator className="*:data-[slot=field-separator-content]:bg-card">
                    Demo accounts (development only)
                  </FieldSeparator>
                  <div className="grid gap-2">
                    {DEMO_ACCOUNTS.map((account) => (
                      <Button
                        key={account.username}
                        type="button"
                        variant="outline"
                        className="h-auto justify-start py-2 text-left whitespace-normal"
                        onClick={() => {
                          setUsername(account.username)
                          setPassword(DEMO_PASSWORD)
                          setError(null)
                        }}
                      >
                        <span className="grid gap-0.5">
                          <span className="font-medium">{account.username}</span>
                          <span className="text-xs font-normal text-muted-foreground">{account.label}</span>
                        </span>
                      </Button>
                    ))}
                    <FieldDescription className="text-center">
                      Password for every demo account: <code>{DEMO_PASSWORD}</code>
                    </FieldDescription>
                  </div>
                </>
              )}
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
      <FieldDescription className="px-6 text-center">
        Your session is kept in a secure, HttpOnly cookie and ends after 30 minutes of inactivity.
      </FieldDescription>
    </div>
  )
}
