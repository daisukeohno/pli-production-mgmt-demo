/** 注意キー（EIBAID 相当）。PF9（旧・一括出力）は実装しない。 */
export type Aid = 'ENTER' | 'CLEAR' | 'PF3' | 'PF4' | 'PF7' | 'PF8';

/** 全画面共通のメッセージ領域（F-MSG / CA-MSG）。 */
export interface ScreenMessage {
  msgId: string | null;
  msgText: string | null;
  fieldPos: string | null;
}

/**
 * 疑似会話のトランザクション呼び出し。COMMAREA はサーバ側セッションに保持されるため Cookie を送る。
 * 業務エラー（4xx）も画面マップを返すので、JSON があればそのまま画面に反映する。
 */
export async function transact<T>(path: string, body?: unknown): Promise<T> {
  const res = await fetch(path, {
    method: body === undefined ? 'GET' : 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const json = await res.json().catch(() => null);
  if (json && typeof json === 'object' && 'scrnId' in json) {
    return json as T;
  }
  throw new Error(`HTTP ${res.status}`);
}
