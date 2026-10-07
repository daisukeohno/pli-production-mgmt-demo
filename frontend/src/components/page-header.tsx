import type { ReactNode } from 'react'
import { Badge } from '@/components/ui/badge'

export function PageHeader({ screenId, title, description, actions }: { screenId?: string; title: string; description?: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          {screenId && (
            <Badge variant="secondary" className="font-mono text-[11px]">
              {screenId}
            </Badge>
          )}
          <h1 className="text-xl font-semibold tracking-tight md:text-2xl">{title}</h1>
        </div>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {actions}
    </div>
  )
}
