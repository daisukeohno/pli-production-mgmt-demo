import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { DataTable, type Column } from '@/components/data-table'
import { FormField } from '@/components/form-field'
import { Pager } from '@/components/pager'
import { Screen } from '@/components/layout/screen'
import { fetchItems } from '@/api/items'
import { ITEM_KBN_LABEL, type Item, type Page } from '@/api/types'
import { ApiError } from '@/api/client'
import { MESSAGES, type MsgId } from '@/lib/messages'
import { notifyMessage } from '@/lib/notify'
import { isoDate, numeric, required, validateForm, type FieldError, type FormRules } from '@/lib/validation'

type DemoField = 'itemCd' | 'orderQty' | 'dueDate'

const DEMO_RULES: FormRules<DemoField> = [
  ['itemCd', [required, numeric]],
  ['orderQty', [required, numeric]],
  ['dueDate', [required, isoDate]],
]

const ITEM_COLUMNS: Column<Item>[] = [
  { key: 'itemCd', header: '品目コード', cell: (r) => <span className="font-mono">{r.itemCd}</span>, className: 'w-32' },
  { key: 'itemName', header: '品目名', cell: (r) => r.itemName },
  {
    key: 'itemKbn',
    header: '区分',
    cell: (r) => <Badge variant={r.itemKbn === '1' ? 'default' : 'secondary'}>{ITEM_KBN_LABEL[r.itemKbn]}</Badge>,
    className: 'w-24',
  },
  { key: 'stockUnit', header: '単位', cell: (r) => r.stockUnit, className: 'w-20' },
]

function useItemList() {
  const [page, setPage] = useState(1)
  const [data, setData] = useState<Page<Item> | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const ac = new AbortController()
    setLoading(true)
    fetchItems(page, ac.signal)
      .then((d) => !ac.signal.aborted && setData(d))
      .catch((e: unknown) => {
        if (e instanceof ApiError && e.msgId) notifyMessage(e.msgId)
      })
      .finally(() => !ac.signal.aborted && setLoading(false))
    return () => ac.abort()
  }, [page])

  const totalPages = data?.totalPages ?? 1
  const prev = useCallback(() => setPage((p) => Math.max(1, p - 1)), [])
  const next = useCallback(() => setPage((p) => Math.min(totalPages, p + 1)), [totalPages])
  return { page, data, loading, prev, next, totalPages }
}

/** 共通部品の見本と動作確認用ページ（画面実装の参考）。API はモック。 */
export function ComponentsShowcasePage() {
  const list = useItemList()
  const [values, setValues] = useState<Record<DemoField, string>>({ itemCd: '', orderQty: '', dueDate: '' })
  const [kbn, setKbn] = useState('1')
  const [error, setError] = useState<FieldError<DemoField> | null>(null)
  const refs = useRef<Partial<Record<DemoField, HTMLInputElement | null>>>({})

  const fieldError = (f: DemoField): MsgId | null => (error?.field === f ? error.msgId : null)
  const set = (f: DemoField) => (e: React.ChangeEvent<HTMLInputElement>) => setValues((v) => ({ ...v, [f]: e.target.value }))

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const err = validateForm(values, DEMO_RULES)
    setError(err)
    if (err) {
      notifyMessage(err.msgId)
      refs.current[err.field]?.focus()
      return
    }
    notifyMessage('M005')
  }

  return (
    <Screen
      title="UI 部品見本"
      description="画面実装で使う共通部品の見本です（API はモック）。"
      functionKeys={[
        { key: 'F7', label: '前頁', onPress: list.prev, disabled: list.page <= 1 || list.loading },
        { key: 'F8', label: '次頁', onPress: list.next, disabled: list.page >= list.totalPages || list.loading },
      ]}
    >
      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">フォーム検証</CardTitle>
            <CardDescription>エラーは項目順に 1 件だけ、トーストと項目下に旧メッセージと同じ文言で表示します。</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-4" onSubmit={submit} noValidate>
              <FormField id="demo-itemCd" label="品目コード" required error={fieldError('itemCd')} hint="8 桁">
                <Input ref={(el) => { refs.current.itemCd = el }} value={values.itemCd} onChange={set('itemCd')} maxLength={8} inputMode="numeric" className="font-mono" />
              </FormField>
              <div className="grid gap-1.5">
                <span className="text-sm font-medium">品目区分</span>
                <Select value={kbn} onValueChange={setKbn}>
                  <SelectTrigger className="w-full" aria-label="品目区分">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(ITEM_KBN_LABEL).map(([k, label]) => (
                      <SelectItem key={k} value={k}>
                        {k}: {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <FormField id="demo-orderQty" label="指示数量" required error={fieldError('orderQty')}>
                <Input ref={(el) => { refs.current.orderQty = el }} value={values.orderQty} onChange={set('orderQty')} maxLength={9} inputMode="numeric" />
              </FormField>
              <FormField id="demo-dueDate" label="完成予定日" required error={fieldError('dueDate')} hint="YYYY-MM-DD">
                <Input ref={(el) => { refs.current.dueDate = el }} value={values.dueDate} onChange={set('dueDate')} maxLength={10} placeholder="2026-11-15" />
              </FormField>
              <Button type="submit" className="justify-self-start">
                <kbd className="rounded border border-current/20 bg-current/5 px-1 text-[11px] leading-4 opacity-80">Enter</kbd>
                登録
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle className="text-base">一覧・ページング</CardTitle>
            <CardDescription>10 件/頁。F7 / F8 またはボタンでページ送りします（モック: db2/data/item_mst.csv）。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <DataTable columns={ITEM_COLUMNS} rows={list.data?.items ?? []} rowKey={(r) => r.itemCd} loading={list.loading} />
            <Pager
              page={list.page}
              totalPages={list.totalPages}
              totalCount={list.data?.totalCount}
              pageSize={list.data?.pageSize}
              onPrev={list.prev}
              onNext={list.next}
              disabled={list.loading}
            />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">メッセージ（旧 23 行目 → トースト）</CardTitle>
          <CardDescription>PMMSG の全メッセージ。クリックでトースト表示を確認できます。</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          {(Object.keys(MESSAGES) as MsgId[]).map((id) => (
            <Button key={id} type="button" size="sm" variant="outline" className="font-mono" onClick={() => notifyMessage(id)} title={MESSAGES[id]}>
              {id}
            </Button>
          ))}
        </CardContent>
      </Card>
    </Screen>
  )
}
