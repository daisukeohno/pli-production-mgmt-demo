import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import { CalendarDaysIcon, MenuIcon, PackageIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { useMock } from '@/api/client'
import { useToday } from '@/hooks/use-today'
import { COMPANY_NAME, SYSTEM_NAME } from '@/lib/constants'
import { formatHeaderDate } from '@/lib/date'
import { SidebarNav } from './sidebar-nav'

function Brand() {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <PackageIcon className="size-4" />
      </span>
      <span className="leading-tight">
        <span className="block text-sm font-semibold">{SYSTEM_NAME}</span>
        <span className="block text-[11px] text-muted-foreground">{COMPANY_NAME}</span>
      </span>
    </Link>
  )
}

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const today = useToday()
  const location = useLocation()

  return (
    <div className="flex min-h-svh">
      <aside className="sticky top-0 hidden h-svh w-60 shrink-0 flex-col border-r bg-sidebar md:flex">
        <div className="flex h-14 items-center border-b px-4">
          <Brand />
        </div>
        <SidebarNav />
      </aside>

      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" className="w-64 gap-0 bg-sidebar p-0">
          <SheetHeader className="h-14 justify-center border-b px-4">
            <SheetTitle className="sr-only">メニュー</SheetTitle>
            <SheetDescription className="sr-only">画面を選択してください</SheetDescription>
            <Brand />
          </SheetHeader>
          <SidebarNav onNavigate={() => setMenuOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b bg-background/90 px-4 backdrop-blur md:px-6">
          <Button variant="ghost" size="icon" className="md:hidden" aria-label="メニューを開く" onClick={() => setMenuOpen(true)}>
            <MenuIcon />
          </Button>
          <span className="truncate text-sm font-semibold whitespace-nowrap md:hidden">{SYSTEM_NAME}</span>
          <div className="ml-auto flex items-center gap-3">
            {useMock && (
              <Badge variant="outline" className="border-amber-300 bg-amber-50 text-amber-800" title="backend 未接続。モック API で応答しています">
                モック API
              </Badge>
            )}
            <span className="flex items-center gap-1.5 text-sm text-muted-foreground" data-testid="header-date">
              <CalendarDaysIcon className="size-4" />
              <time dateTime={today.toISOString().slice(0, 10)}>{formatHeaderDate(today)}</time>
            </span>
          </div>
        </header>
        <main key={location.pathname} className="flex flex-1 flex-col">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
