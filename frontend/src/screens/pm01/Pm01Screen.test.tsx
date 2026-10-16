import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pm01Screen } from './Pm01Screen';

type Handler = (url: URL, init: RequestInit) => { status?: number; body: unknown };

const page = (from: number, n: number, pageNo: number, hasPrev: boolean, hasNext: boolean) => ({
  rows: Array.from({ length: n }, (_, i) => ({ itemCd: String(from + i).padStart(8, '0'), itemName: `品目${from + i}`, itemKbn: '2' })),
  cursor: String(from).padStart(8, '0'),
  pageNo,
  hasPrev,
  hasNext,
});

let calls: { method: string; url: URL; body: unknown }[];

function mockApi(handler: Handler) {
  calls = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: string, init: RequestInit = {}) => {
      const url = new URL(input, 'http://localhost');
      const method = init.method ?? 'GET';
      calls.push({ method, url, body: init.body ? JSON.parse(String(init.body)) : undefined });
      if (url.pathname === '/api/system/date') return new Response(JSON.stringify({ sysDate: '2026-10-16', screenDate: '2026/10/16' }));
      const r = handler(url, init);
      return new Response(JSON.stringify(r.body), { status: r.status ?? 200 });
    }),
  );
}

afterEach(() => vi.unstubAllGlobals());

const item = { itemCd: '20000011', itemName: 'ボルトM8', itemKbn: '2', stockUnit: '個', updTms: '2026-09-01-09.00.00.000000', msgId: null, msgText: null };

test('1行目に画面ID・日付、一覧10行を表示する', async () => {
  mockApi(() => ({ body: page(1, 10, 1, false, true) }));
  render(<Pm01Screen />);
  expect(screen.getByTestId('screen-header')).toHaveTextContent('PM01');
  await waitFor(() => expect(screen.getByTestId('screen-date')).toHaveTextContent('2026/10/16'));
  await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10));
});

test('照会で得た UPD_TMS を保持し、更新時に送信する（CA01-UPD-TMS）', async () => {
  mockApi((url, init) => {
    if (url.pathname === '/api/pm01/items') return { body: page(1, 10, 1, false, true) };
    if (init.method === 'PUT') return { body: { ...item, itemName: 'ボルトM8改', updTms: '2026-10-16-10.00.00.000000', msgId: 'M006', msgText: '更新しました。' } };
    return { body: item };
  });
  const user = userEvent.setup();
  render(<Pm01Screen />);
  await user.type(screen.getByTestId('itemCd'), '20000011');
  await user.click(screen.getByTestId('pf-ENTER'));
  await waitFor(() => expect(screen.getByTestId('itemName')).toHaveValue('ボルトM8'));

  await user.selectOptions(screen.getByTestId('funcCd'), '3');
  await user.clear(screen.getByTestId('itemName'));
  await user.type(screen.getByTestId('itemName'), 'ボルトM8改');
  await user.click(screen.getByTestId('pf-ENTER'));

  await waitFor(() => expect(screen.getByTestId('msg-line')).toHaveTextContent('M006'));
  const put = calls.find((c) => c.method === 'PUT')!;
  expect(put.url.pathname).toBe('/api/pm01/items/20000011');
  expect(put.body).toMatchObject({ itemName: 'ボルトM8改', updTms: '2026-09-01-09.00.00.000000' });
});

test('エラー時はメッセージ行に表示し、CA-MSG-FLD-POS の項目へフォーカスする', async () => {
  mockApi((_url, init) => {
    if (init.method === 'POST')
      return { status: 400, body: { msgId: 'M001', msgText: '品目コードが不正です。チェックデジットを確認してください。', field: 'itemCd', fieldPos: 2 } };
    return { body: page(1, 10, 1, false, true) };
  });
  const user = userEvent.setup();
  render(<Pm01Screen />);
  await user.selectOptions(screen.getByTestId('funcCd'), '2');
  await user.type(screen.getByTestId('itemCd'), '20000111');
  await user.type(screen.getByTestId('itemName'), 'テスト');
  await user.click(screen.getByTestId('pf-ENTER'));
  await waitFor(() => expect(screen.getByTestId('msg-line')).toHaveTextContent('M001品目コードが不正です。'));
  expect(screen.getByTestId('itemCd')).toHaveFocus();
});

test('PF8/PF7 は CA01-TOP-ITEM-CD をカーソルとして送る（キーボード F8/F7）', async () => {
  mockApi((url) => {
    const move = url.searchParams.get('move');
    if (move === 'next') return { body: page(11, 5, 2, true, false) };
    return { body: page(1, 10, 1, false, true) };
  });
  const user = userEvent.setup();
  render(<Pm01Screen />);
  await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10));
  await user.keyboard('{F8}');
  await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(5));
  expect(screen.getByTestId('page-no')).toHaveTextContent('頁 2');
  await user.keyboard('{F7}');
  await waitFor(() => expect(screen.getAllByTestId('list-row')).toHaveLength(10));
  const lists = calls.filter((c) => c.url.pathname === '/api/pm01/items');
  expect(lists[1].url.search).toBe('?cursor=00000001&pageNo=1&move=next');
  expect(lists[2].url.search).toBe('?cursor=00000011&pageNo=2&move=prev');
});
