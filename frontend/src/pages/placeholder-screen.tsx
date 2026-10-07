import { ConstructionIcon } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import type { FunctionKeyAction } from '@/components/function-key-bar'
import { Screen } from '@/components/layout/screen'
import type { NavItem } from '@/navigation'

/** 画面移行チケットで置き換えるまでの空ページ */
export function PlaceholderScreen({ nav, functionKeys }: { nav: NavItem; functionKeys: ReadonlyArray<FunctionKeyAction> }) {
  return (
    <Screen screenId={nav.screenId} title={nav.description} functionKeys={functionKeys}>
      <Card className="border-dashed">
        <CardContent className="flex flex-col items-center gap-3 py-16 text-center">
          <span className="flex size-12 items-center justify-center rounded-full bg-muted">
            <ConstructionIcon className="size-6 text-muted-foreground" />
          </span>
          <p className="font-medium">この画面は準備中です</p>
          <p className="max-w-md text-sm text-muted-foreground">
            {nav.screenId} {nav.description} は後続の画面移行で実装します。下部の操作ボタン（PF キー）は画面実装時に有効になります。
          </p>
        </CardContent>
      </Card>
    </Screen>
  )
}
