import type { ReactNode } from 'react'
import { FunctionKeyBar, type FunctionKeyAction } from '@/components/function-key-bar'
import { PageHeader } from '@/components/page-header'

interface ScreenProps {
  screenId?: string
  title: string
  description?: string
  functionKeys?: ReadonlyArray<FunctionKeyAction>
  children: ReactNode
}

/** 各画面の共通枠。上にタイトル、下に PF キーバー（ボタン + ファンクションキー）。 */
export function Screen({ screenId, title, description, functionKeys, children }: ScreenProps) {
  return (
    <div className="flex flex-1 flex-col">
      <div className="mx-auto w-full max-w-6xl flex-1 space-y-6 p-4 md:p-6">
        <PageHeader screenId={screenId} title={title} description={description} />
        {children}
      </div>
      {functionKeys && functionKeys.length > 0 && <FunctionKeyBar actions={functionKeys} className="sticky bottom-0" />}
    </div>
  )
}
