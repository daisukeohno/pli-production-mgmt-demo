import itemMstCsv from '../../../../db2/data/item_mst.csv?raw'
import { LIST_PAGE_SIZE } from '@/lib/constants'
import type { Item, ItemKbn, Page } from '../types'
import { parseCsv } from './csv'

/**
 * backend 未実装の間のモック API。db2/data の初期データをそのまま使う。
 * 業務ルールは持たせない（一覧の DEL_FLG 除外・品目コード順・10 件/頁のみ）。
 */
interface ItemRow extends Item {
  delFlg: string
}

const items: ItemRow[] = parseCsv(itemMstCsv).map((r) => ({
  itemCd: r.ITEM_CD,
  itemName: r.ITEM_NAME,
  itemKbn: r.ITEM_KBN as ItemKbn,
  stockUnit: r.STOCK_UNIT,
  delFlg: r.DEL_FLG,
  updTms: r.UPD_TMS,
}))

const MOCK_LATENCY_MS = import.meta.env.MODE === 'test' ? 0 : 150

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function toItem({ delFlg: _delFlg, ...item }: ItemRow): Item {
  return item
}

export async function handleMockRequest(method: string, url: string, _body?: unknown): Promise<Response> {
  if (MOCK_LATENCY_MS > 0) await new Promise((r) => setTimeout(r, MOCK_LATENCY_MS))
  const { pathname, searchParams } = new URL(url, 'http://mock.local')

  if (method === 'GET' && pathname === '/api/items') {
    const active = items.filter((i) => i.delFlg === '0').sort((a, b) => a.itemCd.localeCompare(b.itemCd))
    const totalPages = Math.max(1, Math.ceil(active.length / LIST_PAGE_SIZE))
    const page = Math.min(Math.max(1, Number(searchParams.get('page') ?? '1') || 1), totalPages)
    const body: Page<Item> = {
      items: active.slice((page - 1) * LIST_PAGE_SIZE, page * LIST_PAGE_SIZE).map(toItem),
      page,
      pageSize: LIST_PAGE_SIZE,
      totalCount: active.length,
      totalPages,
    }
    return json(200, body)
  }

  const m = /^\/api\/items\/([^/]+)$/.exec(pathname)
  if (method === 'GET' && m) {
    const found = items.find((i) => i.itemCd === decodeURIComponent(m[1]) && i.delFlg === '0')
    return found ? json(200, toItem(found)) : json(404, { msgId: 'M002', field: 'itemCd' })
  }

  return json(404, { msgId: null })
}
