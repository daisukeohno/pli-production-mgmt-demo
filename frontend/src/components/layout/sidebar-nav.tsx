import { NavLink } from 'react-router'
import { LayoutTemplateIcon } from 'lucide-react'
import { NAV_ITEMS } from '@/navigation'
import { cn } from '@/lib/utils'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'group flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive
      ? 'bg-sidebar-accent text-sidebar-accent-foreground'
      : 'text-sidebar-foreground hover:bg-muted hover:text-foreground',
  )

export function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="メインメニュー" className="flex flex-1 flex-col gap-6 p-3">
      <div className="space-y-1">
        <p className="px-3 pb-1 text-[11px] font-semibold tracking-wider text-muted-foreground">業務メニュー</p>
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.to} to={item.to} className={linkClass} onClick={onNavigate}>
            <item.icon className="size-4 shrink-0" />
            <span className="flex-1">{item.label}</span>
            <span className="font-mono text-[10px] text-muted-foreground group-[.active]:text-sidebar-accent-foreground/70">
              {item.screenId}
            </span>
          </NavLink>
        ))}
      </div>
      <div className="mt-auto space-y-1">
        <p className="px-3 pb-1 text-[11px] font-semibold tracking-wider text-muted-foreground">開発用</p>
        <NavLink to="/dev/components" className={linkClass} onClick={onNavigate}>
          <LayoutTemplateIcon className="size-4 shrink-0" />
          <span className="flex-1">UI 部品見本</span>
        </NavLink>
      </div>
    </nav>
  )
}
