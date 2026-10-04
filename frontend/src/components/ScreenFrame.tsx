import { type KeyboardEvent, type ReactNode } from 'react';
import type { Aid, ScreenMessage } from '../api/client';

export interface PfKey {
  aid: Aid;
  label: string;
}

export const SYSTEM_NAME = '生産管理システム';

const KEY_TO_AID: Record<string, Aid> = {
  Enter: 'ENTER',
  Escape: 'CLEAR',
  F3: 'PF3',
  F4: 'PF4',
  F7: 'PF7',
  F8: 'PF8',
};

const AID_KEY_LABEL: Record<Aid, string> = {
  ENTER: 'Enter',
  CLEAR: 'Esc',
  PF3: 'F3',
  PF4: 'F4',
  PF7: 'F7',
  PF8: 'F8',
};

/** 正常終了のメッセージ（サーバは HTTP 200 で返す）。それ以外の msgId はエラー表示 */
const SUCCESS_MSG_IDS = new Set(['M005', 'M006', 'M007', 'M012', 'M013']);

interface Props {
  scrnId: string;
  title: string;
  sysDate: string;
  message: ScreenMessage | null;
  pfKeys: PfKey[];
  onAid: (aid: Aid) => void;
  children: ReactNode;
}

/**
 * 画面の共通枠（旧 24x80 端末画面の 1 / 23 / 24 行目に相当）。
 * line-01: ヘッダバー / line-23: メッセージ（F-MSG） / line-24: PF キーのボタンバー
 */
export function ScreenFrame({ scrnId, title, sysDate, message, pfKeys, onAid, children }: Props) {
  const enabled = new Set(pfKeys.map((k) => k.aid));

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    // IME 変換中（確定の Enter を含む）は注意キーとして扱わない
    if (e.nativeEvent.isComposing || e.nativeEvent.keyCode === 229) return;
    const aid = KEY_TO_AID[e.key];
    if (aid && enabled.has(aid)) {
      e.preventDefault();
      onAid(aid);
    } else if (e.key.startsWith('F') && e.key.length <= 3) {
      e.preventDefault();
    }
  };

  const hasMessage = !!message?.msgId;
  const isSuccess = hasMessage && SUCCESS_MSG_IDS.has(message!.msgId!);

  return (
    <div className="flex min-h-screen flex-col" onKeyDown={onKeyDown}>
      <header
        className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-slate-200 bg-white px-6 shadow-sm"
        data-testid="line-01"
      >
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-slate-500">{SYSTEM_NAME}</span>
          <span className="text-slate-300" aria-hidden="true">
            /
          </span>
          <span className="rounded bg-blue-50 px-2 py-0.5 text-xs font-bold tracking-wide text-blue-700">
            {scrnId}
          </span>{' '}
          <h1 className="text-lg font-bold text-slate-900">{title}</h1>
        </div>
        <time className="text-sm tabular-nums text-slate-500">{sysDate}</time>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 space-y-5 px-6 py-6">
        <div data-testid="line-23" role="status" aria-live="polite">
          {hasMessage && (
            <div
              className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${
                isSuccess
                  ? 'border-green-200 bg-green-50 text-green-800'
                  : 'border-red-200 bg-red-50 text-red-800'
              }`}
              data-tone={isSuccess ? 'success' : 'error'}
            >
              <svg
                aria-hidden="true"
                viewBox="0 0 20 20"
                fill="currentColor"
                className={`mt-0.5 h-5 w-5 shrink-0 ${isSuccess ? 'text-green-600' : 'text-red-600'}`}
              >
                {isSuccess ? (
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z"
                    clipRule="evenodd"
                  />
                ) : (
                  <path
                    fillRule="evenodd"
                    d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z"
                    clipRule="evenodd"
                  />
                )}
              </svg>
              <p>
                <span className="font-semibold tabular-nums">{message!.msgId}</span>{' '}
                <span>{message!.msgText ?? ''}</span>
              </p>
            </div>
          )}
        </div>
        {children}
      </main>

      <footer
        className="sticky bottom-0 z-10 flex flex-wrap items-center gap-2 border-t border-slate-200 bg-white px-6 py-3 shadow-[0_-1px_3px_rgba(0,0,0,0.05)]"
        data-testid="line-24"
      >
        {pfKeys.map((k) => (
          <button
            key={k.aid}
            type="button"
            onClick={() => onAid(k.aid)}
            className={
              k.aid === 'ENTER'
                ? 'rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2'
                : 'rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2'
            }
          >
            {k.label}{' '}
            <span className={`text-xs ${k.aid === 'ENTER' ? 'text-blue-100' : 'text-slate-400'}`}>
              ({AID_KEY_LABEL[k.aid]})
            </span>
          </button>
        ))}
      </footer>
    </div>
  );
}
