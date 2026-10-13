import { query, request, type CursorPage, type MessageResponse, type PageMove } from './client'

export type ItemKbn = '1' | '2' | '9'

export interface Item {
  itemCd: string
  itemName: string
  itemKbn: string
  stockUnit: string
  updTms: string
}

export interface ItemListRow {
  itemCd: string
  itemName: string
  itemKbn: string
}

export interface ItemInput {
  itemName: string
  itemKbn: string
  stockUnit: string
}

export const listItems = (move: PageMove, top: string | null, pageNo: number) =>
  request<CursorPage<ItemListRow>>('GET', `/api/items${query({ move, top, pageNo })}`)

export const getItem = (itemCd: string) => request<Item>('GET', `/api/items/${encodeURIComponent(itemCd)}`)

export const createItem = (itemCd: string, input: ItemInput) =>
  request<MessageResponse<Item>>('POST', '/api/items', { itemCd, ...input })

export const updateItem = (itemCd: string, input: ItemInput, updTms: string) =>
  request<MessageResponse<Item>>('PUT', `/api/items/${encodeURIComponent(itemCd)}`, { ...input, updTms })

export const deleteItem = (itemCd: string, updTms: string) =>
  request<MessageResponse<void>>('DELETE', `/api/items/${encodeURIComponent(itemCd)}${query({ updTms })}`)
