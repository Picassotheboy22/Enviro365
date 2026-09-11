import type { LucideIcon } from 'lucide-react'
import { NavLink, useLocation } from 'react-router'
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@/components/ui/sidebar'

export interface NavItem {
  title: string
  url: string
  icon: LucideIcon
  /** Also highlight this item on pages below this path (e.g. "Clients" stays active on /clients/3). */
  activePrefix?: string
}

export function NavMain({ label, items }: { label: string; items: NavItem[] }) {
  const { pathname } = useLocation()

  return (
    <SidebarGroup>
      <SidebarGroupLabel>{label}</SidebarGroupLabel>
      <SidebarMenu>
        {items.map((item) => {
          const isActive =
            pathname === item.url ||
            (item.activePrefix !== undefined && pathname.startsWith(item.activePrefix))
          return (
            <SidebarMenuItem key={item.url}>
              {/* tooltip: shows the label when the sidebar is collapsed to icons. */}
              <SidebarMenuButton asChild isActive={isActive} tooltip={item.title}>
                <NavLink to={item.url} end>
                  <item.icon />
                  <span>{item.title}</span>
                </NavLink>
              </SidebarMenuButton>
            </SidebarMenuItem>
          )
        })}
      </SidebarMenu>
    </SidebarGroup>
  )
}
