/** 共通エラーレスポンス（backend の ErrorResponse）。field は CA-MSG-FLD-POS 相当のエラー項目。 */
export interface ErrorResponse {
  messageId: string
  message: string
  field?: string
}

/** 正常終了レスポンス（F-MSG に表示するメッセージ＋結果）。 */
export interface MessageResponse<T> {
  messageId: string
  message: string
  data?: T
}

/** キーセット方式の1頁（topKey=CA01-TOP-ITEM-CD、pageNo=CA01-PAGE-NO）。 */
export interface CursorPage<T> {
  items: T[]
  pageNo: number
  topKey: string | null
  hasPrev: boolean
  hasNext: boolean
  size: number
}

export type PageMove = 'first' | 'current' | 'next' | 'prev'

export class ApiError extends Error {
  readonly status: number
  readonly body: ErrorResponse

  constructor(status: number, body: ErrorResponse) {
    super(`${body.messageId} ${body.message}`)
    this.status = status
    this.body = body
  }
}

export async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: { Accept: 'application/json', ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  const json = text ? JSON.parse(text) : undefined
  if (!res.ok) {
    throw new ApiError(res.status, json ?? { messageId: '', message: `HTTP ${res.status}` })
  }
  return json as T
}

export function query(params: Record<string, string | number | null | undefined>): string {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined && v !== '') q.set(k, String(v))
  }
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const fetchMessages = () => request<Record<string, string>>('GET', '/api/messages')
