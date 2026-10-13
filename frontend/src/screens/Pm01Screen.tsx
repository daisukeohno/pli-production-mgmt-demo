import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { ApiError, fetchMessages, type CursorPage, type PageMove } from '../api/client'
import { createItem, deleteItem, getItem, listItems, updateItem, type ItemListRow } from '../api/items'

/** F-FUNC 機能コード */
const FUNCS = [
  { value: '1', label: '1:照会' },
  { value: '2', label: '2:登録' },
  { value: '3', label: '3:更新' },
  { value: '4', label: '4:削除' },
] as const

const KBNS = [
  { value: '1', label: '1:製品' },
  { value: '2', label: '2:部品' },
  { value: '9', label: '9:消耗品' },
] as const

const LIST_ROWS = 10

type FieldName = 'func' | 'itemCd' | 'itemName' | 'itemKbn' | 'stockUnit'

interface Msg {
  id: string
  text: string
  kind: 'info' | 'error'
}

interface Held {
  itemCd: string
  updTms: string
}

const emptyPage: CursorPage<ItemListRow> = { items: [], pageNo: 1, topKey: null, hasPrev: false, hasNext: false, size: LIST_ROWS }

function today(): string {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

/** PM01 品目マスタ保守。 */
export function Pm01Screen() {
  const [func, setFunc] = useState('1')
  const [itemCd, setItemCd] = useState('')
  const [itemName, setItemName] = useState('')
  const [itemKbn, setItemKbn] = useState('')
  const [stockUnit, setStockUnit] = useState('')
  const [held, setHeld] = useState<Held | null>(null)
  const [page, setPage] = useState(emptyPage)
  const [msg, setMsg] = useState<Msg | null>(null)
  const [errorField, setErrorField] = useState<FieldName | null>(null)
  const [busy, setBusy] = useState(false)
  const [catalog, setCatalog] = useState<Record<string, string>>({})

  const refs = {
    func: useRef<HTMLSelectElement>(null),
    itemCd: useRef<HTMLInputElement>(null),
    itemName: useRef<HTMLInputElement>(null),
    itemKbn: useRef<HTMLSelectElement>(null),
    stockUnit: useRef<HTMLInputElement>(null),
  }
  const pageRef = useRef(page)
  pageRef.current = page

  const editable = func === '2' || func === '3'

  const showError = useCallback((id: string, text: string, field?: string | null) => {
    const target: FieldName = field && field in refs ? (field as FieldName) : 'itemCd'
    setMsg({ id, text, kind: 'error' })
    setErrorField(target)
    // CA-MSG-FLD-POS 相当：エラー項目へカーソルを置く
    setTimeout(() => refs[target].current?.focus(), 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleError = useCallback(
    (e: unknown) => {
      if (e instanceof ApiError && e.body.messageId) {
        showError(e.body.messageId, e.body.message, e.body.field)
      } else {
        showError('', e instanceof Error ? e.message : String(e))
      }
    },
    [showError],
  )

  const loadPage = useCallback(
    async (move: PageMove) => {
      const current = pageRef.current
      try {
        setPage(await listItems(move, move === 'first' ? null : current.topKey, current.pageNo))
      } catch (e) {
        handleError(e)
      }
    },
    [handleError],
  )

  useEffect(() => {
    fetchMessages().then(setCatalog).catch(() => setCatalog({}))
    void loadPage('first')
  }, [loadPage])

  // PF7:前頁 PF8:次頁（物理 F キー）
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'F7' || e.key === 'F8') {
        e.preventDefault()
        const p = pageRef.current
        if (e.key === 'F7' && p.hasPrev) void loadPage('prev')
        if (e.key === 'F8' && p.hasNext) void loadPage('next')
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [loadPage])

  const clientError = (id: string, field: FieldName) => showError(id, catalog[id] ?? '', field)

  const fill = (item: { itemCd: string; itemName: string; itemKbn: string; stockUnit: string; updTms: string }) => {
    setItemCd(item.itemCd)
    setItemName(item.itemName)
    setItemKbn(item.itemKbn)
    setStockUnit(item.stockUnit)
    // CA01-UPD-TMS：照会時の UPD_TMS を退避し、更新・削除で送り返す
    setHeld({ itemCd: item.itemCd, updTms: item.updTms })
  }

  const inquire = async (cd: string) => {
    const item = await getItem(cd)
    fill(item)
    setMsg(null)
    setErrorField(null)
  }

  const execute = async (e?: FormEvent) => {
    e?.preventDefault()
    if (busy) return
    if (!FUNCS.some((f) => f.value === func)) return clientError('M004', 'func')
    if (!itemCd.trim()) return clientError('M004', 'itemCd')
    const input = { itemName, itemKbn, stockUnit }
    const heldTms = held && held.itemCd === itemCd ? held.updTms : ''
    setBusy(true)
    try {
      switch (func) {
        case '1':
          await inquire(itemCd)
          break
        case '2': {
          const res = await createItem(itemCd, input)
          if (res.data) fill(res.data)
          done(res.messageId, res.message)
          await loadPage('current')
          break
        }
        case '3': {
          const res = await updateItem(itemCd, input, heldTms)
          if (res.data) fill(res.data)
          done(res.messageId, res.message)
          await loadPage('current')
          break
        }
        case '4': {
          const res = await deleteItem(itemCd, heldTms)
          setHeld(null)
          setItemName('')
          setItemKbn('')
          setStockUnit('')
          done(res.messageId, res.message)
          await loadPage('current')
          break
        }
      }
    } catch (err) {
      handleError(err)
    } finally {
      setBusy(false)
    }
  }

  const done = (id: string, text: string) => {
    setMsg({ id, text, kind: 'info' })
    setErrorField(null)
  }

  const clear = () => {
    setFunc('1')
    setItemCd('')
    setItemName('')
    setItemKbn('')
    setStockUnit('')
    setHeld(null)
    setMsg(null)
    setErrorField(null)
    refs.itemCd.current?.focus()
  }

  const selectRow = async (row: ItemListRow) => {
    setFunc('1')
    setItemCd(row.itemCd)
    try {
      await inquire(row.itemCd)
    } catch (err) {
      handleError(err)
    }
  }

  const invalid = (f: FieldName) => (errorField === f ? { 'aria-invalid': true as const, className: 'field-error' } : {})
  const rows: (ItemListRow | null)[] = [...page.items, ...Array(Math.max(LIST_ROWS - page.items.length, 0)).fill(null)]

  return (
    <div className="screen">
      <header className="screen-header">
        <span className="screen-id">PM01</span>
        <h1>品目マスタ保守</h1>
        <span className="screen-date">{today()}</span>
      </header>

      <form className="card form" onSubmit={execute} aria-label="品目入力">
        <div className="form-grid">
          <label htmlFor="f-func">機能コード</label>
          <select id="f-func" ref={refs.func} value={func} onChange={(e) => setFunc(e.target.value)} {...invalid('func')}>
            {FUNCS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </select>

          <label htmlFor="f-itemcd">品目コード</label>
          <input
            id="f-itemcd"
            ref={refs.itemCd}
            value={itemCd}
            maxLength={8}
            inputMode="numeric"
            autoComplete="off"
            onChange={(e) => setItemCd(e.target.value)}
            {...invalid('itemCd')}
          />

          <label htmlFor="f-itemnm">品目名</label>
          <input
            id="f-itemnm"
            ref={refs.itemName}
            value={itemName}
            maxLength={20}
            readOnly={!editable}
            onChange={(e) => setItemName(e.target.value)}
            {...invalid('itemName')}
          />

          <label htmlFor="f-kbn">品目区分</label>
          <select
            id="f-kbn"
            ref={refs.itemKbn}
            value={itemKbn}
            disabled={!editable}
            onChange={(e) => setItemKbn(e.target.value)}
            {...invalid('itemKbn')}
          >
            <option value="">（選択）</option>
            {KBNS.map((k) => (
              <option key={k.value} value={k.value}>
                {k.label}
              </option>
            ))}
          </select>

          <label htmlFor="f-unit">単位</label>
          <input
            id="f-unit"
            ref={refs.stockUnit}
            value={stockUnit}
            maxLength={4}
            readOnly={!editable}
            onChange={(e) => setStockUnit(e.target.value)}
            {...invalid('stockUnit')}
          />
        </div>
        <div className="actions">
          <button type="submit" className="primary" disabled={busy}>
            実行 (Enter)
          </button>
          <button type="button" onClick={clear}>
            クリア
          </button>
        </div>
      </form>

      <section className="card" aria-label="品目一覧">
        <table className="list">
          <thead>
            <tr>
              <th>品目コード</th>
              <th>品目名</th>
              <th>区分</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) =>
              row ? (
                <tr
                  key={row.itemCd}
                  className={row.itemCd === held?.itemCd ? 'selected' : undefined}
                  onClick={() => void selectRow(row)}
                  data-testid="list-row"
                >
                  <td className="mono">{row.itemCd}</td>
                  <td>{row.itemName}</td>
                  <td>{KBNS.find((k) => k.value === row.itemKbn)?.label ?? row.itemKbn}</td>
                </tr>
              ) : (
                <tr key={`empty-${i}`} className="empty">
                  <td colSpan={3}>&nbsp;</td>
                </tr>
              ),
            )}
          </tbody>
        </table>
        <div className="pager">
          <button type="button" onClick={() => void loadPage('prev')} disabled={!page.hasPrev}>
            PF7 前頁
          </button>
          <span aria-label="ページ番号">{page.pageNo} 頁</span>
          <button type="button" onClick={() => void loadPage('next')} disabled={!page.hasNext}>
            PF8 次頁
          </button>
        </div>
      </section>

      <footer className={`msg-line ${msg?.kind ?? ''}`} role="status" aria-live="polite" data-testid="f-msg">
        {msg ? (
          <>
            {msg.id && <span className="msg-id">{msg.id}</span>}
            {msg.text}
          </>
        ) : (
          '\u00a0'
        )}
      </footer>
      <div className="pf-guide">Enter:実行　F7:前頁　F8:次頁</div>
    </div>
  )
}
