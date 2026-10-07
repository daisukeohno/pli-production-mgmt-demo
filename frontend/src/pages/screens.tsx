import { NAV_ITEMS } from '@/navigation'
import type { FunctionKeyAction } from '@/components/function-key-bar'
import { PlaceholderScreen } from './placeholder-screen'

const noop = () => {}
const [pm01, pm02, pm03] = NAV_ITEMS

/** PF キーは docs/design.md に記載のあるもののみ配置（画面実装チケットで有効化する） */
const PM01_KEYS: FunctionKeyAction[] = [
  { key: 'F7', label: '前頁', onPress: noop, disabled: true },
  { key: 'F8', label: '次頁', onPress: noop, disabled: true },
]
const PM03_KEYS: FunctionKeyAction[] = [{ key: 'F4', label: '不足部品一覧', onPress: noop, disabled: true }]

export function Pm01Page() {
  return <PlaceholderScreen nav={pm01} functionKeys={PM01_KEYS} />
}

export function Pm02Page() {
  return <PlaceholderScreen nav={pm02} functionKeys={[]} />
}

export function Pm03Page() {
  return <PlaceholderScreen nav={pm03} functionKeys={PM03_KEYS} />
}
