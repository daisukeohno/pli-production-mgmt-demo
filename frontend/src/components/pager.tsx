import { ChevronLeftIcon, ChevronRightIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface PagerProps {
  page: number
  totalPages: number
  totalCount?: number
  pageSize?: number
  onPrev: () => void
  onNext: () => void
  disabled?: boolean
}

/** 一覧のページ送り。F7（前頁）/ F8（次頁）は画面の FunctionKeyBar 側で割り当てる。 */
export function Pager({ page, totalPages, totalCount, pageSize, onPrev, onNext, disabled }: PagerProps) {
  const from = totalCount && pageSize ? (page - 1) * pageSize + 1 : null
  const to = totalCount && pageSize ? Math.min(page * pageSize, totalCount) : null

  return (
    <nav aria-label="ページ送り" className="flex flex-wrap items-center justify-between gap-2 text-sm">
      <p className="text-muted-foreground" aria-live="polite">
        {totalCount !== undefined && from !== null ? `${totalCount} 件中 ${from}–${to} 件 ・ ` : ''}
        {page} / {totalPages} 頁
      </p>
      <div className="flex gap-2">
        <Button type="button" size="sm" variant="outline" onClick={onPrev} disabled={disabled || page <= 1} aria-keyshortcuts="F7">
          <ChevronLeftIcon />
          前頁
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={onNext} disabled={disabled || page >= totalPages} aria-keyshortcuts="F8">
          次頁
          <ChevronRightIcon />
        </Button>
      </div>
    </nav>
  )
}
