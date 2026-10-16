import { useEffect, useState, type ReactNode } from 'react';
import { fetchSystemDate } from '../api/system';
import { MessageLine } from '../message/MessageLine';
import type { ScreenMessage } from '../message/message';

export type PfKey = 'ENTER' | 'CLEAR' | 'PF3' | 'PF7' | 'PF8';

export interface PfKeyDef {
  key: PfKey;
  label: string;
  onPress: () => void;
  disabled?: boolean;
}

const KEYBOARD: Record<string, PfKey> = { Enter: 'ENTER', Escape: 'CLEAR', F3: 'PF3', F7: 'PF7', F8: 'PF8' };

/**
 * 24x80 端末画面の枠（全画面共通）。
 * 1行目：画面ID・画面名・日付、23行目：メッセージ、24行目：PF キーガイド。
 * キーボードの Enter / Esc（CLEAR）/ F3 / F7 / F8 を PF キーとして扱う。
 */
export function ScreenFrame(props: {
  screenId: string;
  title: string;
  message: ScreenMessage | null;
  pfKeys: PfKeyDef[];
  children: ReactNode;
}) {
  const { screenId, title, message, pfKeys, children } = props;
  const [screenDate, setScreenDate] = useState('');

  useEffect(() => {
    fetchSystemDate()
      .then((d) => setScreenDate(d.screenDate))
      .catch(() => setScreenDate(new Date().toLocaleDateString('ja-JP', { year: 'numeric', month: '2-digit', day: '2-digit' })));
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const pf = KEYBOARD[e.key];
      const def = pf && pfKeys.find((k) => k.key === pf);
      if (!def) return;
      e.preventDefault();
      if (!def.disabled) def.onPress();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [pfKeys]);

  return (
    <div className="terminal" data-screen={screenId}>
      <div className="row header" data-testid="screen-header">
        <span className="screen-id">{screenId}</span>
        <span className="screen-title">{title}</span>
        <span className="screen-date" data-testid="screen-date">{screenDate}</span>
      </div>
      <div className="body">{children}</div>
      <MessageLine message={message} />
      <div className="row pf-bar">
        {pfKeys.map((k) => (
          <button key={k.key} type="button" className="pf-key" onClick={k.onPress} disabled={k.disabled} data-testid={`pf-${k.key}`}>
            {k.key}:{k.label}
          </button>
        ))}
      </div>
    </div>
  );
}
