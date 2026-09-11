import { Outlet, useLocation } from 'react-router'
import { useCurrentUser } from '@/auth/auth-context'
import { AppSidebar } from '@/components/app-sidebar'
import { SiteHeader } from '@/components/site-header'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { useLiveRefresh } from '@/hooks/use-live-refresh'

function pageTitle(pathname: string, isStaff: boolean): string {
  if (pathname.startsWith('/clients/')) return 'Client portfolio'
  if (pathname === '/clients') return 'Clients'
  if (pathname === '/') return isStaff ? 'Dashboard' : 'Overview'
  if (pathname === '/history') return isStaff ? 'Withdrawal notices' : 'Withdrawal history'
  if (pathname === '/withdraw') return 'New withdrawal'
  return 'Enviro365'
}

/** The signed-in shell: sidebar navigation, a header with the page title, and the current page. */
export function AppLayout() {
  const { pathname } = useLocation()
  const user = useCurrentUser()
  // Only signed-in pages refresh themselves; see useLiveRefresh for when and why.
  useLiveRefresh()

  return (
    <SidebarProvider>
      <AppSidebar variant="inset" />
      <SidebarInset>
        <SiteHeader title={pageTitle(pathname, user.role === 'ADMIN')} />
        <main className="flex flex-1 flex-col gap-6 p-4 lg:p-6">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
