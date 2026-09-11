import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { ApiError } from '@/api/client'
import { AuthProvider } from '@/auth/auth-provider'
import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import App from './App.tsx'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retrying can't fix a client error (401, 403, 404...), so only retry network or server problems, at most twice.
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
      // Other people change these figures too (staff pay a notice, an investor submits one), so cached data counts as
      // out of date straight away: a page fetches fresh figures when it opens and when the user comes back to the tab,
      // while still showing the previous figures instantly. useLiveRefresh also refreshes them while the app is in use.
      staleTime: 0,
      refetchOnWindowFocus: true,
    },
  },
})

const root = document.getElementById('root')
if (!root) throw new Error('Missing #root element in index.html')

createRoot(root).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <TooltipProvider>
            <App />
            <Toaster richColors position="top-right" />
          </TooltipProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
