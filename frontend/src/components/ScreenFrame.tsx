import { type KeyboardEvent, type ReactNode } from 'react';
import type { Aid, ScreenMessage } from '../api/client';

export interface PfKey {
  aid: Aid;
  label: string;
}

const KEY_TO_AID: Record<string, Aid> = {
  Enter: 'ENTER',
  Escape: 'CLEAR',
  F3: 'PF3',
  F4: 'PF4',
  F7: 'PF7',
  F8: 'PF8',
};

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
 * 24x80 端末画面の共通枠。
 * 1 行目: 画面ID・タイトル・日付 / 23 行目: メッセージ（F-MSG） / 24 行目: PF キーガイド
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

  const isError = message?.msgId != null && message.fieldPos != null;

  return (
    <div className="terminal" onKeyDown={onKeyDown}>
      <div className="row header" data-testid="line-01">
        <span>{scrnId}</span>
        <span className="title">{title}</span>
        <span>{sysDate}</span>
      </div>
      <div className="body">{children}</div>
      <div className={`row msg ${isError ? 'error' : ''}`} data-testid="line-23" role="status">
        {message?.msgId ? `${message.msgId} ${message.msgText ?? ''}` : ''}
      </div>
      <div className="row pfkeys" data-testid="line-24">
        {pfKeys.map((k) => (
          <button key={k.aid} type="button" onClick={() => onAid(k.aid)}>
            {k.label}
          </button>
        ))}
      </div>
    </div>
  );
}
