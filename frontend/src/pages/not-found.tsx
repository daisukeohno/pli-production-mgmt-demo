import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-4xl font-semibold text-muted-foreground">404</p>
      <p className="text-sm text-muted-foreground">指定された画面は存在しません。</p>
      <Button asChild variant="outline">
        <Link to="/">品目マスタへ戻る</Link>
      </Button>
    </div>
  )
}
