import { ApiError } from '../api/client';

/** メッセージ行（23行目 F-MSG）の内容。 */
export interface ScreenMessage {
  msgId: string | null;
  msgText: string;
  level: 'info' | 'error';
}

export const infoMessage = (msgId: string | null, msgText: string | null): ScreenMessage | null =>
  msgText ? { msgId, msgText, level: 'info' } : null;

export function errorMessage(e: unknown): ScreenMessage {
  if (e instanceof ApiError) return { msgId: e.body.msgId, msgText: e.body.msgText, level: 'error' };
  return { msgId: null, msgText: 'システムエラーが発生しました。', level: 'error' };
}
