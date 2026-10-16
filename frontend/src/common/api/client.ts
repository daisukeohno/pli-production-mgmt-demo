/** 全画面共通の API クライアント。エラーは ErrorResponse（msgId/msgText/field/fieldPos）を ApiError にして投げる。 */

export interface ErrorResponse {
  msgId: string | null;
  msgText: string;
  /** エラー項目名（null は項目指定なし） */
  field: string | null;
  /** BMS 項目順の位置（CA-MSG-FLD-POS、0 は項目指定なし） */
  fieldPos: number;
}

export class ApiError extends Error {
  readonly status: number;
  readonly body: ErrorResponse;

  constructor(status: number, body: ErrorResponse) {
    super(body.msgText);
    this.status = status;
    this.body = body;
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';
export type Query = Record<string, string | number | undefined | null>;

export function buildPath(path: string, query?: Query): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params.set(k, String(v));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

export async function apiRequest<T>(method: Method, path: string, options: { query?: Query; body?: unknown } = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(buildPath(path, options.query), {
      method,
      headers: { Accept: 'application/json', ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, { msgId: null, msgText: 'サーバーに接続できません。', field: null, fieldPos: 0 });
  }
  const text = await res.text();
  const json = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new ApiError(res.status, json ?? { msgId: null, msgText: `通信エラー（${res.status}）`, field: null, fieldPos: 0 });
  }
  return json as T;
}

export const api = {
  get: <T,>(path: string, query?: Query) => apiRequest<T>('GET', path, { query }),
  post: <T,>(path: string, body: unknown) => apiRequest<T>('POST', path, { body }),
  put: <T,>(path: string, body: unknown) => apiRequest<T>('PUT', path, { body }),
  delete: <T,>(path: string, query?: Query) => apiRequest<T>('DELETE', path, { query }),
};
