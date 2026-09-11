import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from '@/auth/require-auth'
import { AppLayout } from '@/components/app-layout'
import { ClientPage } from '@/pages/client-page'
import { ClientsPage } from '@/pages/clients-page'
import { HistoryPage } from '@/pages/history-page'
import { LoginPage } from '@/pages/login-page'
import { OverviewPage } from '@/pages/overview-page'
import { WithdrawPage } from '@/pages/withdraw-page'

/**
 * Routes: /login is public; everything else needs a session and shares the sidebar layout.
 *  /                    investor: own portfolio · staff: dashboard
 *  /clients             staff: every client
 *  /clients/:investorId staff: one client's portfolio
 *  /withdraw            investor: new withdrawal notice
 *  /history             investor: own history · staff: every client's notices (?investor=3 narrows to one client)
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<OverviewPage />} />
        <Route path="clients" element={<ClientsPage />} />
        <Route path="clients/:investorId" element={<ClientPage />} />
        <Route path="withdraw" element={<WithdrawPage />} />
        <Route path="history" element={<HistoryPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
