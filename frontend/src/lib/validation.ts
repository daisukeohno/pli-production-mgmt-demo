import type { MsgId } from './messages'

/**
 * 共通フォーム検証。
 * 旧画面は 1 回の送信で 1 つのメッセージだけを 23 行目に出し、カーソルをエラー項目へ置く（CA-MSG-ID / CA-MSG-FLD-POS）。
 * それに合わせ、ルールを BMS 項目順に評価して最初のエラー 1 件だけを返す。
 * 業務判定（チェックデジット・存在チェック・在庫不足など）の正はバックエンドで、ここでは形式チェックのみを扱う。
 */
export type Rule = (value: string) => MsgId | null

export interface FieldError<F extends string = string> {
  field: F
  msgId: MsgId
}

export const required: Rule = (v) => (v.trim() === '' ? 'M004' : null)

/** 空欄は required に任せ、入力がある場合のみ数字以外を検出する */
export const numeric: Rule = (v) => (v.trim() !== '' && !/^[0-9]+$/.test(v.trim()) ? 'M008' : null)

/** YYYY-MM-DD 形式かつ暦日として妥当か */
export const isoDate: Rule = (v) => {
  const s = v.trim()
  if (s === '') return null
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s)
  if (!m) return 'M016'
  const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const dt = new Date(Date.UTC(y, mo - 1, d))
  return dt.getUTCFullYear() === y && dt.getUTCMonth() === mo - 1 && dt.getUTCDate() === d ? null : 'M016'
}

export type FormRules<F extends string> = ReadonlyArray<readonly [F, ReadonlyArray<Rule>]>

/** rules は画面の項目順（BMS 項目順）に並べる */
export function validateForm<F extends string>(
  values: Readonly<Record<F, string>>,
  rules: FormRules<F>,
): FieldError<F> | null {
  for (const [field, fieldRules] of rules) {
    for (const rule of fieldRules) {
      const msgId = rule(values[field] ?? '')
      if (msgId) return { field, msgId }
    }
  }
  return null
}
