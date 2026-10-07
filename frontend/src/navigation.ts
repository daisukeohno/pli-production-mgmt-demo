import { BoxesIcon, ClipboardListIcon, FactoryIcon, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  screenId: string
  label: string
  description: string
  icon: LucideIcon
}

export const NAV_ITEMS: ReadonlyArray<NavItem> = [
  { to: '/items', screenId: 'PM01', label: '品目マスタ', description: '品目マスタ保守', icon: ClipboardListIcon },
  { to: '/stock', screenId: 'PM02', label: '在庫', description: '在庫照会・入出庫登録', icon: BoxesIcon },
  { to: '/work-orders', screenId: 'PM03', label: '製造指示', description: '製造指示登録', icon: FactoryIcon },
]
