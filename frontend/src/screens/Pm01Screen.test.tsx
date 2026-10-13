import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Pm01Screen } from './Pm01Screen'

const MESSAGES = {
  M001: '品目コードが不正です。チェックデジットを確認してください。',
  M002: '該当する品目は登録されていません。',
  M004: '必須項目が未入力です。',
  M006: '更新しました。',
  M017: '他の端末で更新されています。再照会してください。',
}

const codes = Array.from({ length: 15 }, (_, i) => String(10000000 + i * 7).padStart(8, '0'))
const row = (cd: string) => ({ itemCd: cd, itemName: `品目${cd}`, itemKbn: '2' })
const page1 = { items: codes.slice(0, 10).map(row), pageNo: 1, topKey: codes[0], hasPrev: false, hasNext: true, size: 10 }
const page2 = { items: codes.slice(10).map(row), pageNo: 2, topKey: codes[10], hasPrev: true, hasNext: false, size: 10 }

type Handler = (url: URL, init: RequestInit) => { status: number; body: unknown }
let calls: { method: string; url: URL; body?: unknown }[] = []

function mockApi(handler: Handler) {
  calls = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init: RequestInit = {}) => {
      const url = new URL(input, 'http://localhost')
      calls.push({ method: init.method ?? 'GET', url, body: init.body ? JSON.parse(String(init.body)) : undefined })
      const res = url.pathname === '/api/messages' ? { status: 200, body: MESSAGES } : handler(url, init)
      return new Response(JSON.stringify(res.body), { status: res.status })
    }),
  )
}

const listHandler: Handler = (url) => {
  if (url.pathname === '/api/items') {
    return { status: 200, body: url.searchParams.get('move') === 'next' ? page2 : page1 }
  }
  return { status: 404, body: { messageId: 'M002', message: MESSAGES.M002, field: 'itemCd' } }
}

describe('PM01 品目マスタ保守', () => {
  beforeEach(() => vi.unstubAllGlobals())

  it('一覧を10件表示し、PF8/F8で次頁・PF7/F7で前頁へ移動する', async () => {
    mockApi(listHandler)
    render(<Pm01Screen />)
    await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10))
    expect(screen.getByRole('button', { name: 'PF7 前頁' })).toBeDisabled()

    fireEvent.keyDown(window, { key: 'F8' })
    await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(5))
    const next = calls.find((c) => c.url.searchParams.get('move') === 'next')!
    expect(next.url.searchParams.get('top')).toBe(codes[0])
    expect(next.url.searchParams.get('pageNo')).toBe('1')
    expect(screen.getByText('2 頁')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'PF8 次頁' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'PF7 前頁' }))
    await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10))
    const prev = calls.find((c) => c.url.searchParams.get('move') === 'prev')!
    expect(prev.url.searchParams.get('top')).toBe(codes[10])
  })

  it('登録でチェックデジット不正(M001)ならメッセージを表示し品目コードへフォーカスする', async () => {
    mockApi((url, init) =>
      url.pathname === '/api/items' && init.method === 'POST'
        ? { status: 400, body: { messageId: 'M001', message: MESSAGES.M001, field: 'itemCd' } }
        : listHandler(url, init),
    )
    render(<Pm01Screen />)
    await userEvent.selectOptions(screen.getByLabelText('機能コード'), '2')
    await userEvent.type(screen.getByLabelText('品目コード'), '20000111')
    await userEvent.type(screen.getByLabelText('品目名'), 'スペーサー')
    await userEvent.selectOptions(screen.getByLabelText('品目区分'), '2')
    await userEvent.type(screen.getByLabelText('単位'), '個{Enter}')

    const msg = await screen.findByTestId('f-msg')
    await waitFor(() => expect(msg).toHaveTextContent(`M001${MESSAGES.M001}`))
    await waitFor(() => expect(screen.getByLabelText('品目コード')).toHaveFocus())
    expect(screen.getByLabelText('品目コード')).toHaveAttribute('aria-invalid', 'true')
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({
      itemCd: '20000111',
      itemName: 'スペーサー',
      itemKbn: '2',
      stockUnit: '個',
    })
  })

  it('照会時の UPD_TMS を保持して更新に送り、不一致なら M017 を表示する', async () => {
    const item = { itemCd: codes[3], itemName: '歯車', itemKbn: '2', stockUnit: '組', updTms: '2026-09-01-09.00.00.000000' }
    mockApi((url, init) => {
      if (url.pathname === `/api/items/${codes[3]}` && (init.method ?? 'GET') === 'GET') return { status: 200, body: item }
      if (init.method === 'PUT') return { status: 409, body: { messageId: 'M017', message: MESSAGES.M017 } }
      return listHandler(url, init)
    })
    render(<Pm01Screen />)
    await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10))
    await userEvent.click(within(screen.getAllByTestId('list-row')[3]).getByText(codes[3]))
    await waitFor(() => expect(screen.getByLabelText('品目名')).toHaveValue('歯車'))

    await userEvent.selectOptions(screen.getByLabelText('機能コード'), '3')
    await userEvent.clear(screen.getByLabelText('品目名'))
    await userEvent.type(screen.getByLabelText('品目名'), '歯車改')
    await userEvent.click(screen.getByRole('button', { name: '実行 (Enter)' }))

    await waitFor(() => expect(screen.getByTestId('f-msg')).toHaveTextContent(`M017${MESSAGES.M017}`))
    expect(calls.find((c) => c.method === 'PUT')?.body).toEqual({
      itemName: '歯車改',
      itemKbn: '2',
      stockUnit: '組',
      updTms: '2026-09-01-09.00.00.000000',
    })
  })

  it('品目コード未入力で実行すると M004（サーバー定義の文言）', async () => {
    mockApi(listHandler)
    render(<Pm01Screen />)
    await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10))
    await userEvent.click(screen.getByRole('button', { name: '実行 (Enter)' }))
    await waitFor(() => expect(screen.getByTestId('f-msg')).toHaveTextContent(`M004${MESSAGES.M004}`))
    expect(screen.getByLabelText('品目コード')).toHaveFocus()
  })
})
