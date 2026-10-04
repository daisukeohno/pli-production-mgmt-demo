import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { EndedPanel } from '../components/EndedPanel';
import {
  FormGrid,
  FormRow,
  SegmentedControl,
  SelectInput,
  TextInput,
  focusField,
  withCurrent,
  type Option,
} from '../components/Fields';
import { ListTable } from '../components/ListTable';
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
  { aid: 'ENTER', label: '実行' },
  { aid: 'PF3', label: '終了' },
  { aid: 'PF7', label: '前頁' },
  { aid: 'PF8', label: '次頁' },
  { aid: 'CLEAR', label: '取消' },
];

const KBN_LABEL: Record<string, string> = { '1': '製品', '2': '部品', '9': '消耗品' };

const FUNC_OPTIONS: Option[] = [
  { value: '1', label: '照会' },
  { value: '2', label: '登録' },
  { value: '3', label: '更新' },
  { value: '4', label: '削除' },
];

const KBN_OPTIONS: Option[] = Object.entries(KBN_LABEL).map(([value, label]) => ({ value, label: `${value}:${label}` }));

/** PM01 品目マスタ保守 */
export function Pm01() {
  const [screen, setScreen] = useState<Pm01Response | null>(null);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [commError, setCommError] = useState<string | null>(null);
  const refs = useRef<Partial<Record<FieldPos, HTMLElement | null>>>({});

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
    focusField(el);
  }, [screen]);

  if (screen?.ended) {
    return <EndedPanel message="PM01 品目マスタ保守を終了しました。" onRestart={() => void start()} />;
  }

  // F-ITEMNM / F-KBN / F-UNIT は機能 2（登録）・3（更新）のみ入力可
  const attrEditable = fields.func === '2' || fields.func === '3';
  const set = (k: keyof Fields) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  const errorAt = (pos: FieldPos) => !!screen?.msgId && screen.fieldPos === pos;

  return (
    <ScreenFrame
      scrnId="PM01"
      title="品目マスタ保守"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <FormGrid>
        <FormRow label="機能" labelId="F-FUNC-label">
          <SegmentedControl
            id="F-FUNC"
            labelledBy="F-FUNC-label"
            options={FUNC_OPTIONS}
            value={fields.func}
            onChange={(func) => setFields({ ...fields, func })}
            invalid={errorAt('F-FUNC')}
            focusRef={(el) => (refs.current['F-FUNC'] = el)}
          />
        </FormRow>
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
          <TextInput
            id="F-ITEMNM"
            ref={(el) => (refs.current['F-ITEMNM'] = el)}
            className="w-full max-w-md"
            invalid={errorAt('F-ITEMNM')}
            maxLength={20}
            readOnly={!attrEditable}
            tabIndex={attrEditable ? 0 : -1}
            value={fields.itemName}
            onChange={set('itemName')}
          />
        </FormRow>
        <FormRow label="品目区分" htmlFor="F-KBN">
          <SelectInput
            id="F-KBN"
            ref={(el) => (refs.current['F-KBN'] = el)}
            className="w-40"
            invalid={errorAt('F-KBN')}
            disabled={!attrEditable}
            value={fields.itemKbn}
            onChange={set('itemKbn')}
          >
            <option value="">未選択</option>
            {withCurrent(KBN_OPTIONS, fields.itemKbn).map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </SelectInput>
        </FormRow>
        <FormRow label="単位" htmlFor="F-UNIT">
          <TextInput
            id="F-UNIT"
            ref={(el) => (refs.current['F-UNIT'] = el)}
            className="w-24"
            invalid={errorAt('F-UNIT')}
            maxLength={4}
            readOnly={!attrEditable}
            tabIndex={attrEditable ? 0 : -1}
            value={fields.stockUnit}
            onChange={set('stockUnit')}
          />
        </FormRow>
      </FormGrid>

      <ListTable<ListRow>
        title="品目一覧"
        aside={
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium tabular-nums text-slate-600">
            頁 {screen?.pageNo ?? ''}
          </span>
        }
        columns={[
          { header: '品目コード', className: 'w-32 tabular-nums', render: (r) => r.itemCd },
          { header: '品目名', render: (r) => r.itemName },
          {
            header: '品目区分',
            className: 'w-32',
            render: (r) => (
              <span className="inline-flex items-center gap-1.5">
                <span className="tabular-nums text-slate-400">{r.itemKbn}</span> {KBN_LABEL[r.itemKbn] ?? ''}
              </span>
            ),
          },
        ]}
        rows={screen?.list ?? []}
        size={PAGE_SIZE}
        testId="item-list"
        rowTestIdPrefix="F-LIST"
      />
    </ScreenFrame>
  );
}
