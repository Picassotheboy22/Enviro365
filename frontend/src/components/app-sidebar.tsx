import {
  FileTextIcon,
  HistoryIcon,
  LandmarkIcon,
  LayoutDashboardIcon,
  SendIcon,
  UsersIcon,
} from 'lucide-react'
import type { ComponentProps } from 'react'
import { Link } from 'react-router'
import { useCurrentUser } from '@/auth/auth-context'
import { NavMain, type NavItem } from '@/components/nav-main'
import { NavUser } from '@/components/nav-user'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
} from '@/components/ui/sidebar'

// Staff work across clients; investors work with their own portfolio. Staff review and pay notices rather than submit
// them, so they don't get the withdrawal form.
const STAFF_ITEMS: NavItem[] = [
  { title: 'Dashboard', url: '/', icon: LayoutDashboardIcon },
  { title: 'Clients', url: '/clients', icon: UsersIcon, activePrefix: '/clients/' },
  { title: 'Withdrawal notices', url: '/history', icon: FileTextIcon },
]

const INVESTOR_ITEMS: NavItem[] = [
  { title: 'Overview', url: '/', icon: LayoutDashboardIcon },
  { title: 'New withdrawal', url: '/withdraw', icon: SendIcon },
  { title: 'History', url: '/history', icon: HistoryIcon },
]

/** Adapted from the shadcn/ui "sidebar-07" block: collapses to icons, with the signed-in user at the bottom. */
export function AppSidebar(props: ComponentProps<typeof Sidebar>) {
  const user = useCurrentUser()
  const isStaff = user.role === 'ADMIN'

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" asChild>
              <Link to="/">
                <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                  <LandmarkIcon className="size-4" />
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">Enviro365</span>
                  <span className="truncate text-xs text-muted-foreground">
                    {isStaff ? 'Staff portal' : 'Investments'}
                  </span>
                </div>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain label={isStaff ? 'Staff' : 'Menu'} items={isStaff ? STAFF_ITEMS : INVESTOR_ITEMS} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
