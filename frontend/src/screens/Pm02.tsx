import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { ScreenFrame, type PfKey } from '../components/ScreenFrame';

interface Fields {
  itemCd: string;
  itemName: string;
  stockQty: number | null;
  ioKbn: string;
  ioQty: string;
}

interface Pm02Response extends ScreenMessage {
  scrnId: string;
  sysDate: string;
  fields: Fields;
  ended: boolean;
}

type InputPos = 'F-ITEMCD' | 'F-IOKBN' | 'F-IOQTY';

const EMPTY: Fields = { itemCd: '', itemName: '', stockQty: null, ioKbn: '', ioQty: '' };

/** 9 桁の数量項目（styles.css は共通のため画面側で幅を指定） */
const QTY_STYLE = { width: '10ch', textAlign: 'right' } as const;

const PF_KEYS: PfKey[] = [
  { aid: 'ENTER', label: 'Enter:実行' },
  { aid: 'PF3', label: 'PF3:終了' },
  { aid: 'CLEAR', label: 'CLEAR(Esc):取消' },
];

/** PM02 在庫照会・入出庫登録 */
export function Pm02() {
  const [screen, setScreen] = useState<Pm02Response | null>(null);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [commError, setCommError] = useState<string | null>(null);
  const refs = useRef<Partial<Record<InputPos, HTMLInputElement | null>>>({});

  const apply = (res: Pm02Response) => {
    setScreen(res);
    setFields(res.fields);
    setCommError(null);
  };

  const start = useCallback(async () => {
    setBusy(true);
    try {
      apply(await transact<Pm02Response>('/api/pm02'));
    } catch (e) {
      setCommError(String(e));
    } finally {
      setBusy(false);
    }
  }, []);

  useEffect(() => {
    void start();
  }, [start]);

  const onAid = async (aid: Aid) => {
    if (busy) return;
    setBusy(true);
    try {
      const { itemCd, ioKbn, ioQty } = fields;
      apply(await transact<Pm02Response>('/api/pm02', { aid, itemCd, ioKbn, ioQty }));
    } catch (e) {
      setCommError(String(e));
    } finally {
      setBusy(false);
    }
  };

  // CA-MSG-FLD-POS 相当：指定項目（無指定時は F-ITEMCD）へカーソルを置く
  useLayoutEffect(() => {
    if (!screen || screen.ended) return;
    const pos = (screen.fieldPos ?? 'F-ITEMCD') as InputPos;
    const el = refs.current[pos] ?? refs.current['F-ITEMCD'];
    el?.focus();
    el?.select();
  }, [screen]);

  if (screen?.ended) {
    return (
      <div className="terminal ended">
        <p>PM02 在庫照会・入出庫登録を終了しました。</p>
        <button type="button" onClick={() => void start()} autoFocus>
          再開
        </button>
      </div>
    );
  }

  const set = (k: 'itemCd' | 'ioKbn' | 'ioQty') => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  // 業務エラー（msgId あり）の項目のみ反転表示。照会後のカーソル位置指定（msgId なし）は対象外
  const errorAt = (pos: InputPos) => (screen?.msgId && screen.fieldPos === pos ? 'error' : '');

  return (
    <ScreenFrame
      scrnId="PM02"
      title="在庫照会・入出庫登録"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <div className="row">
        <label htmlFor="F-ITEMCD">品目コード</label>
        <input
          id="F-ITEMCD"
          ref={(el) => (refs.current['F-ITEMCD'] = el)}
          className={`fld w8 ${errorAt('F-ITEMCD')}`}
          maxLength={8}
          value={fields.itemCd}
          onChange={set('itemCd')}
        />
      </div>
      <div className="row">
        <label htmlFor="F-ITEMNM">品目名</label>
        <input id="F-ITEMNM" className="fld w20" readOnly tabIndex={-1} value={fields.itemName} />
      </div>
      <div className="row">
        <label htmlFor="F-STOCKQTY">現在庫数量</label>
        <input
          id="F-STOCKQTY"
          className="fld"
          style={QTY_STYLE}
          readOnly
          tabIndex={-1}
          value={fields.stockQty == null ? '' : String(fields.stockQty)}
        />
      </div>
      <div className="row">
        <label htmlFor="F-IOKBN">入出庫区分</label>
        <input
          id="F-IOKBN"
          ref={(el) => (refs.current['F-IOKBN'] = el)}
          className={`fld w1 ${errorAt('F-IOKBN')}`}
          maxLength={1}
          value={fields.ioKbn}
          onChange={set('ioKbn')}
        />
        <span className="hint">1:入庫 2:出庫</span>
      </div>
      <div className="row">
        <label htmlFor="F-IOQTY">入出庫数量</label>
        <input
          id="F-IOQTY"
          ref={(el) => (refs.current['F-IOQTY'] = el)}
          className={`fld ${errorAt('F-IOQTY')}`}
          style={QTY_STYLE}
          maxLength={9}
          value={fields.ioQty}
          onChange={set('ioQty')}
        />
      </div>
    </ScreenFrame>
  );
}
