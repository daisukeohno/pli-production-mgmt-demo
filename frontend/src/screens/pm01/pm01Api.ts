import { api } from '../../common/api/client';

export type FuncCd = '1' | '2' | '3' | '4';
export type PageMove = 'next' | 'prev';

export interface ItemInput {
  itemCd: string;
  itemName: string;
  itemKbn: string;
  stockUnit: string;
}

export interface ItemResponse extends ItemInput {
  /** UPD_TMS（DB2 形式 YYYY-MM-DD-HH.MM.SS.ffffff）。CA01-UPD-TMS として保持する */
  updTms: string;
  msgId: string | null;
  msgText: string | null;
}

export interface DeleteResponse {
  itemCd: string;
  msgId: string;
  msgText: string;
}

export interface ListRow {
  itemCd: string;
  itemName: string;
  itemKbn: string;
}

export interface ListResponse {
  rows: ListRow[];
  /** 当該頁の先頭品目コード（CA01-TOP-ITEM-CD） */
  cursor: string | null;
  /** CA01-PAGE-NO */
  pageNo: number;
  hasPrev: boolean;
  hasNext: boolean;
}

const BASE = '/api/pm01/items';

export const pm01Api = {
  list: (cursor: string | null, pageNo: number, move?: PageMove) =>
    api.get<ListResponse>(BASE, { cursor, pageNo, move }),
  inquire: (itemCd: string) => api.get<ItemResponse>(`${BASE}/${encodeURIComponent(itemCd)}`),
  register: (input: ItemInput) => api.post<ItemResponse>(BASE, input),
  update: (input: ItemInput, updTms: string) =>
    api.put<ItemResponse>(`${BASE}/${encodeURIComponent(input.itemCd)}`, { ...input, updTms }),
  remove: (itemCd: string, updTms: string) =>
    api.delete<DeleteResponse>(`${BASE}/${encodeURIComponent(itemCd)}`, { updTms }),
};
