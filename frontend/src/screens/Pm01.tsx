import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { ScreenFrame, type PfKey } from '../components/ScreenFrame';

interface Fields {
  func: string;
  itemCd: string;
  itemName: string;
  itemKbn: string;
  stockUnit: string;
}

interface ListRow {
  itemCd: string;
  itemName: string;
  itemKbn: string;
}

interface Pm01Response extends ScreenMessage {
  scrnId: string;
  sysDate: string;
  fields: Fields;
  list: ListRow[];
  pageNo: number;
  ended: boolean;
}

type FieldPos = 'F-FUNC' | 'F-ITEMCD' | 'F-ITEMNM' | 'F-KBN' | 'F-UNIT';

const PAGE_SIZE = 10;
const EMPTY: Fields = { func: '', itemCd: '', itemName: '', itemKbn: '', stockUnit: '' };

const PF_KEYS: PfKey[] = [
  { aid: 'ENTER', label: 'Enter:実行' },
  { aid: 'PF3', label: 'PF3:終了' },
  { aid: 'PF7', label: 'PF7:前頁' },
  { aid: 'PF8', label: 'PF8:次頁' },
  { aid: 'CLEAR', label: 'CLEAR(Esc):取消' },
];

const KBN_LABEL: Record<string, string> = { '1': '製品', '2': '部品', '9': '消耗品' };

/** PM01 品目マスタ保守 */
export function Pm01() {
  const [screen, setScreen] = useState<Pm01Response | null>(null);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [commError, setCommError] = useState<string | null>(null);
  const refs = useRef<Partial<Record<FieldPos, HTMLInputElement | null>>>({});

  const apply = (res: Pm01Response) => {
    setScreen(res);
    setFields(res.fields);
    setCommError(null);
  };

  const start = useCallback(async () => {
    setBusy(true);
    try {
      apply(await transact<Pm01Response>('/api/pm01'));
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
      apply(await transact<Pm01Response>('/api/pm01', { aid, ...fields }));
    } catch (e) {
      setCommError(String(e));
    } finally {
      setBusy(false);
    }
  };

  // CA-MSG-FLD-POS 相当：エラー項目（無指定時は F-FUNC）へカーソルを置く
  useLayoutEffect(() => {
    if (!screen || screen.ended) return;
    const pos = (screen.fieldPos ?? 'F-FUNC') as FieldPos;
    const el = refs.current[pos] ?? refs.current['F-FUNC'];
    el?.focus();
    el?.select();
  }, [screen]);

  if (screen?.ended) {
    return (
      <div className="terminal ended">
        <p>PM01 品目マスタ保守を終了しました。</p>
        <button type="button" onClick={() => void start()} autoFocus>
          再開
        </button>
      </div>
    );
  }

  // F-ITEMNM / F-KBN / F-UNIT は機能 2（登録）・3（更新）のみ入力可
  const attrEditable = fields.func === '2' || fields.func === '3';
  const set = (k: keyof Fields) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  const errorAt = (pos: FieldPos) => (screen?.msgId && screen.fieldPos === pos ? 'error' : '');

  const rows: (ListRow | null)[] = [...(screen?.list ?? [])];
  while (rows.length < PAGE_SIZE) rows.push(null);

  return (
    <ScreenFrame
      scrnId="PM01"
      title="品目マスタ保守"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <div className="row">
        <label htmlFor="F-FUNC">機能コード</label>
        <input
          id="F-FUNC"
          ref={(el) => (refs.current['F-FUNC'] = el)}
          className={`fld w1 ${errorAt('F-FUNC')}`}
          maxLength={1}
          value={fields.func}
          onChange={set('func')}
        />
        <span className="hint">1:照会 2:登録 3:更新 4:削除</span>
      </div>
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
        <input
          id="F-ITEMNM"
          ref={(el) => (refs.current['F-ITEMNM'] = el)}
          className={`fld w20 ${errorAt('F-ITEMNM')}`}
          maxLength={20}
          readOnly={!attrEditable}
          tabIndex={attrEditable ? 0 : -1}
          value={fields.itemName}
          onChange={set('itemName')}
        />
      </div>
      <div className="row">
        <label htmlFor="F-KBN">品目区分</label>
        <input
          id="F-KBN"
          ref={(el) => (refs.current['F-KBN'] = el)}
          className={`fld w1 ${errorAt('F-KBN')}`}
          maxLength={1}
          readOnly={!attrEditable}
          tabIndex={attrEditable ? 0 : -1}
          value={fields.itemKbn}
          onChange={set('itemKbn')}
        />
        <span className="hint">1:製品 2:部品 9:消耗品</span>
      </div>
      <div className="row">
        <label htmlFor="F-UNIT">単位</label>
        <input
          id="F-UNIT"
          ref={(el) => (refs.current['F-UNIT'] = el)}
          className={`fld w4 ${errorAt('F-UNIT')}`}
          maxLength={4}
          readOnly={!attrEditable}
          tabIndex={attrEditable ? 0 : -1}
          value={fields.stockUnit}
          onChange={set('stockUnit')}
        />
      </div>

      <div className="row list-head">
        <span className="c-cd">品目コード</span>
        <span className="c-nm">品目名</span>
        <span className="c-kbn">区分</span>
        <span className="page">頁 {screen?.pageNo ?? ''}</span>
      </div>
      <ol className="list" data-testid="item-list">
        {rows.map((r, i) => (
          <li key={i} className="row" data-testid={`F-LIST${String(i + 1).padStart(2, '0')}`}>
            {r && (
              <>
                <span className="c-cd">{r.itemCd}</span>
                <span className="c-nm">{r.itemName}</span>
                <span className="c-kbn">
                  {r.itemKbn} {KBN_LABEL[r.itemKbn] ?? ''}
                </span>
              </>
            )}
          </li>
        ))}
      </ol>
    </ScreenFrame>
  );
}
