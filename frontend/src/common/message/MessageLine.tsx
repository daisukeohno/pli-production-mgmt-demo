import type { ScreenMessage } from './message';

/** 23行目のメッセージ行。エラーは強調表示。 */
export function MessageLine({ message }: { message: ScreenMessage | null }) {
  return (
    <div className={`row msg-line ${message?.level === 'error' ? 'msg-error' : 'msg-info'}`} role="status" aria-live="polite" data-testid="msg-line">
      {message ? (
        <>
          {message.msgId && <span className="msg-id">{message.msgId}</span>}
          <span className="msg-text">{message.msgText}</span>
        </>
      ) : (
        '\u00a0'
      )}
    </div>
  );
}
