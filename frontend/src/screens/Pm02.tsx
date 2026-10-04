import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { EndedPanel } from '../components/EndedPanel';
import { FormGrid, FormRow, SelectInput, TextInput, focusField, withCurrent, type Option } from '../components/Fields';
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

const IOKBN_OPTIONS: Option[] = [
  { value: '1', label: '1:入庫' },
  { value: '2', label: '2:出庫' },
];

const PF_KEYS: PfKey[] = [
  { aid: 'ENTER', label: '実行' },
  { aid: 'PF3', label: '終了' },
  { aid: 'CLEAR', label: '取消' },
];

/** PM02 在庫照会・入出庫登録 */
export function Pm02() {
  const [screen, setScreen] = useState<Pm02Response | null>(null);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [commError, setCommError] = useState<string | null>(null);
  const refs = useRef<Partial<Record<InputPos, HTMLElement | null>>>({});

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
    focusField(el);
  }, [screen]);

  if (screen?.ended) {
    return <EndedPanel message="PM02 在庫照会・入出庫登録を終了しました。" onRestart={() => void start()} />;
  }

  const set = (k: 'itemCd' | 'ioKbn' | 'ioQty') => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  // 業務エラー（msgId あり）の項目のみ赤枠表示。照会後のカーソル位置指定（msgId なし）は対象外
  const errorAt = (pos: InputPos) => !!screen?.msgId && screen.fieldPos === pos;

  return (
    <ScreenFrame
      scrnId="PM02"
      title="在庫照会・入出庫登録"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <FormGrid>
        <FormRow label="品目コード" htmlFor="F-ITEMCD">
          <TextInput
            id="F-ITEMCD"
            ref={(el) => (refs.current['F-ITEMCD'] = el)}
            className="w-36 tabular-nums"
            invalid={errorAt('F-ITEMCD')}
            maxLength={8}
            value={fields.itemCd}
            onChange={set('itemCd')}
          />
        </FormRow>
        <FormRow label="品目名" htmlFor="F-ITEMNM">
          <TextInput id="F-ITEMNM" className="w-full max-w-md" readOnly tabIndex={-1} value={fields.itemName} />
        </FormRow>
        <FormRow label="現在庫数量" htmlFor="F-STOCKQTY">
          <TextInput
            id="F-STOCKQTY"
            className="w-36 text-right tabular-nums"
            readOnly
            tabIndex={-1}
            value={fields.stockQty == null ? '' : String(fields.stockQty)}
          />
        </FormRow>
      </FormGrid>

      <FormGrid>
        <FormRow label="入出庫区分" htmlFor="F-IOKBN">
          <SelectInput
            id="F-IOKBN"
            ref={(el) => (refs.current['F-IOKBN'] = el)}
            className="w-40"
            invalid={errorAt('F-IOKBN')}
            value={fields.ioKbn}
            onChange={set('ioKbn')}
          >
            <option value="">未選択</option>
            {withCurrent(IOKBN_OPTIONS, fields.ioKbn).map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </SelectInput>
        </FormRow>
        <FormRow label="入出庫数量" htmlFor="F-IOQTY">
          <TextInput
            id="F-IOQTY"
            ref={(el) => (refs.current['F-IOQTY'] = el)}
            className="w-36 text-right tabular-nums"
            invalid={errorAt('F-IOQTY')}
            maxLength={9}
            value={fields.ioQty}
            onChange={set('ioQty')}
          />
        </FormRow>
      </FormGrid>
    </ScreenFrame>
  );
}
