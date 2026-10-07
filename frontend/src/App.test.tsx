import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { Toaster } from '@/components/ui/sonner'
import { App } from './App'

function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
      <Toaster />
    </MemoryRouter>,
  )
}

describe('App shell', () => {
  it('3 画面の空ページへサイドナビで遷移できる', async () => {
    renderApp('/')
    expect(await screen.findByRole('heading', { name: '品目マスタ保守' })).toBeInTheDocument()

    const nav = screen.getByRole('navigation', { name: 'メインメニュー' })
    await userEvent.click(within(nav).getByRole('link', { name: /在庫/ }))
    expect(screen.getByRole('heading', { name: '在庫照会・入出庫登録' })).toBeInTheDocument()

    await userEvent.click(within(nav).getByRole('link', { name: /製造指示/ }))
    expect(screen.getByRole('heading', { name: '製造指示登録' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /F4.*不足部品一覧/ })).toBeDisabled()

    await userEvent.click(within(nav).getByRole('link', { name: /品目マスタ/ }))
    expect(screen.getByRole('heading', { name: '品目マスタ保守' })).toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: /品目マスタ/ })).toHaveAttribute('aria-current', 'page')
  })

  it('ヘッダーにシステム名と YYYY/MM/DD の日付を表示する', () => {
    renderApp('/items')
    expect(screen.getAllByText('生産管理システム').length).toBeGreaterThan(0)
    expect(screen.getByTestId('header-date').textContent).toMatch(/^\d{4}\/\d{2}\/\d{2}$/)
  })

  it('未定義の URL は 404', () => {
    renderApp('/nope')
    expect(screen.getByText('指定された画面は存在しません。')).toBeInTheDocument()
  })
})

describe('UI 部品見本', () => {
  it('未入力で送信すると M004 をトーストと項目下に表示し、その項目へフォーカスする', async () => {
    renderApp('/dev/components')
    await userEvent.click(screen.getByRole('button', { name: /登録/ }))
    const itemCd = screen.getByLabelText(/品目コード/)
    expect(itemCd).toHaveAttribute('aria-invalid', 'true')
    expect(itemCd).toHaveFocus()
    expect(await screen.findAllByText('必須項目が未入力です。')).toHaveLength(2)
  })

  it('完成予定日の形式不正は M016', async () => {
    renderApp('/dev/components')
    await userEvent.type(screen.getByLabelText(/品目コード/), '10000014')
    await userEvent.type(screen.getByLabelText(/指示数量/), '10')
    await userEvent.type(screen.getByLabelText(/完成予定日/), '2026/11/15')
    await userEvent.keyboard('{Enter}')
    expect(await screen.findAllByText('完成予定日の形式が不正です。YYYY-MM-DDで入力してください。')).toHaveLength(2)
  })

  it('F8 / F7 で一覧をページ送りする', async () => {
    renderApp('/dev/components')
    expect(await screen.findByText('10000014')).toBeInTheDocument()
    expect(screen.getByText(/1 \/ 2 頁/)).toBeInTheDocument()

    await userEvent.keyboard('{F8}')
    expect(await screen.findByText('90000034')).toBeInTheDocument()
    expect(screen.getByText(/2 \/ 2 頁/)).toBeInTheDocument()
    expect(screen.queryByText('10000014')).not.toBeInTheDocument()

    await userEvent.keyboard('{F7}')
    expect(await screen.findByText('10000014')).toBeInTheDocument()
  })
})
