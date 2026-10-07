import { Button } from '@/components/ui/button'
import { useFunctionKeys, type FunctionKeyBinding } from '@/hooks/use-function-keys'
import { cn } from '@/lib/utils'

export interface FunctionKeyAction extends FunctionKeyBinding {
  label: string
  variant?: 'default' | 'outline' | 'secondary' | 'destructive'
}

/** 旧 24 行目の PF キーガイドの置き換え。ボタン押下とファンクションキーの両方で操作できる。 */
export function FunctionKeyBar({ actions, className }: { actions: ReadonlyArray<FunctionKeyAction>; className?: string }) {
  useFunctionKeys(actions)

  return (
    <div
      role="toolbar"
      aria-label="操作"
      className={cn('flex flex-wrap items-center gap-2 border-t bg-card/95 px-4 py-3 backdrop-blur md:px-6', className)}
    >
      {actions.map((a) => (
        <Button
          key={a.key}
          type="button"
          size="sm"
          variant={a.variant ?? 'outline'}
          disabled={a.disabled}
          onClick={a.onPress}
          aria-keyshortcuts={a.key}
        >
          <kbd className="rounded border border-current/20 bg-current/5 px-1 text-[11px] leading-4 opacity-80">{a.key}</kbd>
          {a.label}
        </Button>
      ))}
    </div>
  )
}
