import { apiRequest } from './client'
import type { Item, Page } from './types'

/** 暫定 API。正式な契約はバックエンド骨格のチケットで確定する。 */
export function fetchItems(page: number, signal?: AbortSignal) {
  return apiRequest<Page<Item>>('/items', { query: { page }, signal })
}

export function fetchItem(itemCd: string) {
  return apiRequest<Item>(`/items/${encodeURIComponent(itemCd)}`)
}
