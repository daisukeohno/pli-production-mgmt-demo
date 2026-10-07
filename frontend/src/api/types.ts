/** 品目区分 1:製品 2:部品 9:消耗品（ITEM_MST.ITEM_KBN） */
export type ItemKbn = '1' | '2' | '9'

export const ITEM_KBN_LABEL: Record<ItemKbn, string> = {
  '1': '製品',
  '2': '部品',
  '9': '消耗品',
}

export interface Item {
  itemCd: string
  itemName: string
  itemKbn: ItemKbn
  stockUnit: string
  updTms: string
}

export interface Page<T> {
  items: T[]
  page: number
  pageSize: number
  totalCount: number
  totalPages: number
}
