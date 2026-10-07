import { isMsgId, type MsgId } from '@/lib/messages'
import { handleMockRequest } from './mock/handlers'

/**
 * API クライアント。/api は開発時 Vite が backend（既定 :8080）へプロキシする。
 * backend 未実装の間は VITE_API_MOCK=false を指定しない限りモック（src/api/mock）で応答する。
 */
export const API_BASE = '/api'
export const useMock = import.meta.env.VITE_API_MOCK !== 'false'

/** 業務エラー応答（旧 CA-MSG-ID / CA-MSG-FLD-POS 相当） */
export interface ApiErrorBody {
  msgId: string
  field?: string | null
}

export class ApiError extends Error {
  readonly status: number
  readonly msgId: MsgId | null
  readonly field: string | null

  constructor(status: number, body: Partial<ApiErrorBody> | null) {
    const msgId = body?.msgId && isMsgId(body.msgId) ? body.msgId : null
    super(msgId ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.msgId = msgId
    this.field = body?.field ?? null
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  query?: Record<string, string | number | undefined>
  body?: unknown
  signal?: AbortSignal
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const qs = new URLSearchParams()
  for (const [k, v] of Object.entries(query ?? {})) {
    if (v !== undefined) qs.set(k, String(v))
  }
  const s = qs.toString()
  return `${API_BASE}${path}${s ? `?${s}` : ''}`
}

export async function apiRequest<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const method = opts.method ?? 'GET'
  const url = buildUrl(path, opts.query)

  const res = useMock
    ? await handleMockRequest(method, url, opts.body)
    : await fetch(url, {
        method,
        credentials: 'same-origin',
        headers: opts.body === undefined ? undefined : { 'Content-Type': 'application/json' },
        body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
        signal: opts.signal,
      })

  const json: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new ApiError(res.status, json as Partial<ApiErrorBody> | null)
  }
  return json as T
}
