import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { transact, type Aid, type ScreenMessage } from '../api/client';
import { EndedPanel } from '../components/EndedPanel';
import { FormGrid, FormRow, TextInput, focusField } from '../components/Fields';
import { ListTable } from '../components/ListTable';
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
  { aid: 'ENTER', label: '登録' },
  { aid: 'PF4', label: '不足一覧' },
  { aid: 'PF3', label: '終了' },
  { aid: 'CLEAR', label: '取消' },
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
    focusField(el);
  }, [screen]);

  if (screen?.ended) {
    return <EndedPanel message="PM03 製造指示登録を終了しました。" onRestart={() => void start()} />;
  }

  const set = (k: keyof Fields) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setFields({ ...fields, [k]: e.target.value });
  const errorAt = (pos: FieldPos) => !!screen?.msgId && screen.fieldPos === pos;

  return (
    <ScreenFrame
      scrnId="PM03"
      title="製造指示登録"
      sysDate={screen?.sysDate ?? ''}
      message={commError ? { msgId: '----', msgText: `通信エラー ${commError}`, fieldPos: null } : screen}
      pfKeys={PF_KEYS}
      onAid={(aid) => void onAid(aid)}
    >
      <FormGrid>
        <FormRow label="製品コード" htmlFor="F-ITEMCD" hint={fields.itemName}>
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
        <FormRow label="指示数量" htmlFor="F-ORDERQTY">
          <TextInput
            id="F-ORDERQTY"
            ref={(el) => (refs.current['F-ORDERQTY'] = el)}
            className="w-36 text-right tabular-nums"
            invalid={errorAt('F-ORDERQTY')}
            maxLength={9}
            inputMode="numeric"
            value={fields.orderQty}
            onChange={set('orderQty')}
          />
        </FormRow>
        <FormRow label="完成予定日" htmlFor="F-DUEDATE">
          <TextInput
            id="F-DUEDATE"
            ref={(el) => (refs.current['F-DUEDATE'] = el)}
            className="w-40 tabular-nums"
            invalid={errorAt('F-DUEDATE')}
            maxLength={10}
            placeholder="YYYY-MM-DD"
            value={fields.dueDate}
            onChange={set('dueDate')}
          />
        </FormRow>
        <FormRow label="製造指示番号" htmlFor="F-ORDERNO">
          <TextInput
            id="F-ORDERNO"
            className="w-36 tabular-nums"
            readOnly
            tabIndex={-1}
            value={fields.orderNo}
          />
        </FormRow>
      </FormGrid>

      {/* F-NGLIST01〜10 は PF4 で送信された不足部品一覧。未表示時は空行 */}
      <ListTable<NgRow>
        title="不足部品一覧（F4）"
        columns={[
          { header: '部品コード', className: 'tabular-nums', render: (r) => r.itemCd },
          { header: '必要数量', align: 'right', className: 'w-40', render: (r) => r.needQty },
          {
            header: '在庫数量',
            align: 'right',
            className: 'w-40',
            render: (r) => <span className="font-semibold text-red-600">{r.stockQty}</span>,
          },
        ]}
        rows={screen?.ngList ?? []}
        size={NG_LIST_SIZE}
        testId="ng-list"
        rowTestIdPrefix="F-NGLIST"
      />
    </ScreenFrame>
  );
}
