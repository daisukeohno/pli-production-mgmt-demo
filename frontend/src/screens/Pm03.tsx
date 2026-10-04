import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { ScreenFrame, type PfKey } from '../components/ScreenFrame';

interface Fields {
  itemCd: string;
  itemName: string;
  orderQty: string;
  dueDate: string;
  orderNo: string;
}

interface NgRow {
  itemCd: string;
  needQty: number;
  stockQty: number;
}

interface Pm03Response extends ScreenMessage {
  scrnId: string;
  sysDate: string;
  fields: Fields;
  ngList: NgRow[];
  ended: boolean;
}

type FieldPos = 'F-ITEMCD' | 'F-ORDERQTY' | 'F-DUEDATE';

const NG_LIST_SIZE = 10;
const EMPTY: Fields = { itemCd: '', itemName: '', orderQty: '', dueDate: '', orderNo: '' };

const PF_KEYS: PfKey[] = [
  { aid: 'ENTER', label: 'Enter:登録' },
  { aid: 'PF4', label: 'PF4:不足一覧' },
  { aid: 'PF3', label: 'PF3:終了' },
  { aid: 'CLEAR', label: 'CLEAR(Esc):取消' },
];

/** PM03 製造指示登録 */
export function Pm03() {
  const [screen, setScreen] = useState<Pm03Response | null>(null);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [commError, setCommError] = useState<string | null>(null);
  const refs = useRef<Partial<Record<FieldPos, HTMLInputElement | null>>>({});

  const apply = (res: Pm03Response) => {
    setScreen(res);
    setFields(res.fields);
    setCommError(null);
  };

  const start = useCallback(async () => {
    setBusy(true);
    try {
      apply(await transact<Pm03Response>('/api/pm03'));
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
      apply(
        await transact<Pm03Response>('/api/pm03', {
          aid,
          itemCd: fields.itemCd,
          orderQty: fields.orderQty,
          dueDate: fields.dueDate,
        }),
      );
    } catch (e) {
      setCommError(String(e));
    } finally {
      setBusy(false);
    }
  };

  // CA-MSG-FLD-POS 相当：エラー項目（無指定時は F-ITEMCD）へカーソルを置く
  useLayoutEffect(() => {
    if (!screen || screen.ended) return;
    const pos = (screen.fieldPos ?? 'F-ITEMCD') as FieldPos;
    const el = refs.current[pos] ?? refs.current['F-ITEMCD'];
    el?.focus();
    el?.select();
  }, [screen]);

  if (screen?.ended) {
    return (
      <div className="terminal ended">
        <p>PM03 製造指示登録を終了しました。</p>
        <button type="button" onClick={() => void start()} autoFocus>
          再開
        </button>
      </div>
    );
  }

  const set = (k: keyof Fields) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  const errorAt = (pos: FieldPos) => (screen?.msgId && screen.fieldPos === pos ? 'error' : '');

  // F-NGLIST01〜10 は PF4 で送信された不足部品一覧。未表示時は空行
  const ngRows: (NgRow | null)[] = [...(screen?.ngList ?? [])];
  while (ngRows.length < NG_LIST_SIZE) ngRows.push(null);

  return (
    <ScreenFrame
      scrnId="PM03"
      title="製造指示登録"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <div className="row">
        <label htmlFor="F-ITEMCD">製品コード</label>
        <input
          id="F-ITEMCD"
          ref={(el) => (refs.current['F-ITEMCD'] = el)}
          className={`fld w8 ${errorAt('F-ITEMCD')}`}
          maxLength={8}
          value={fields.itemCd}
          onChange={set('itemCd')}
        />
        <span className="hint">{fields.itemName}</span>
      </div>
      <div className="row">
        <label htmlFor="F-ORDERQTY">指示数量</label>
        <input
          id="F-ORDERQTY"
          ref={(el) => (refs.current['F-ORDERQTY'] = el)}
          className={`fld ${errorAt('F-ORDERQTY')}`}
          style={{ width: '10ch' }}
          maxLength={9}
          inputMode="numeric"
          value={fields.orderQty}
          onChange={set('orderQty')}
        />
      </div>
      <div className="row">
        <label htmlFor="F-DUEDATE">完成予定日</label>
        <input
          id="F-DUEDATE"
          ref={(el) => (refs.current['F-DUEDATE'] = el)}
          className={`fld ${errorAt('F-DUEDATE')}`}
          style={{ width: '12ch' }}
          maxLength={10}
          placeholder="YYYY-MM-DD"
          value={fields.dueDate}
          onChange={set('dueDate')}
        />
      </div>
      <div className="row">
        <label htmlFor="F-ORDERNO">製造指示番号</label>
        <input
          id="F-ORDERNO"
          className="fld"
          style={{ width: '8ch' }}
          readOnly
          tabIndex={-1}
          value={fields.orderNo}
        />
      </div>

      <div className="row list-head">
        <span className="c-cd">部品コード</span>
        <span className="c-nm">不足一覧（PF4）</span>
      </div>
      <ol className="list" data-testid="ng-list">
        {ngRows.map((r, i) => (
          <li key={i} className="row" data-testid={`F-NGLIST${String(i + 1).padStart(2, '0')}`}>
            {r && (
              <span>
                {r.itemCd} 必要 {r.needQty} 在庫 {r.stockQty}
              </span>
            )}
          </li>
        ))}
      </ol>
    </ScreenFrame>
  );
}
