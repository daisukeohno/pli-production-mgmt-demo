/**
 * PMMSG（src/include/PMMSG.inc）のメッセージID と文言。
 * 文言は旧システムと一字一句同じ（CHAR(60) の末尾空白のみ除去）。messages.test.ts で PMMSG.inc と突き合わせている。
 */
export const MESSAGES = {
  M001: '品目コードが不正です。チェックデジットを確認してください。',
  M002: '該当する品目は登録されていません。',
  M003: '品目コードは既に使用されています。',
  M004: '必須項目が未入力です。',
  M005: '登録しました。',
  M006: '更新しました。',
  M007: '削除しました。',
  M008: '数値項目に数字以外が入力されています。',
  M009: '在庫数量が不正です。',
  M010: '入出庫区分は1(入庫)または2(出庫)を入力してください。',
  M011: '在庫が不足しています。出庫数量を確認してください。',
  M012: '入出庫を登録しました。',
  M013: '製造指示を登録しました。',
  M014: '部品構成が登録されていません。',
  M015: '在庫不足の部品があります。23行目にPF4で一覧を表示します。',
  M016: '完成予定日の形式が不正です。YYYY-MM-DDで入力してください。',
  M017: '他の端末で更新されています。再照会してください。',
} as const

export type MsgId = keyof typeof MESSAGES

/** 正常終了を表すメッセージ。それ以外はエラーとして扱う。 */
const SUCCESS_IDS: ReadonlySet<MsgId> = new Set<MsgId>(['M005', 'M006', 'M007', 'M012', 'M013'])

export function isMsgId(id: string): id is MsgId {
  return Object.prototype.hasOwnProperty.call(MESSAGES, id)
}

export function messageText(id: MsgId): string {
  return MESSAGES[id]
}

export function isSuccessMessage(id: MsgId): boolean {
  return SUCCESS_IDS.has(id)
}
