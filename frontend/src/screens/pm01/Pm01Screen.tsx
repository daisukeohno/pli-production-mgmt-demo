import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../common/api/client';
import { useFieldFocus } from '../../common/focus/useFieldFocus';
import { errorMessage, infoMessage, type ScreenMessage } from '../../common/message/message';
import { ScreenFrame, type PfKeyDef } from '../../common/screen/ScreenFrame';
import { pm01Api, type FuncCd, type ItemInput, type ListResponse, type PageMove } from './pm01Api';

const FUNC_LABELS: Record<FuncCd, string> = { '1': '照会', '2': '登録', '3': '更新', '4': '削除' };
const KBN_LABELS: Record<string, string> = { '1': '製品', '2': '部品', '9': '消耗品' };
const LIST_ROWS = 10;
const EMPTY: ItemInput = { itemCd: '', itemName: '', itemKbn: '', stockUnit: '' };

/** COMMAREA（PMCOMM.inc CA01-*）の PM01 部。CA01-LOCK-FLG は旧排他のデッドコードのため持たない。 */
interface Ca01 {
  pageNo: number;
  topItemCd: string | null;
  /** 照会で取得した品目コードと UPD_TMS。更新・削除時に送る */
  heldItemCd: string;
  updTms: string;
}

const INITIAL_CA01: Ca01 = { pageNo: 1, topItemCd: null, heldItemCd: '', updTms: '' };

/** PM01 品目マスタ保守（BMS マップ PM01M 相当）。 */
export function Pm01Screen() {
  const [funcCd, setFuncCd] = useState<FuncCd>('1');
  const [form, setForm] = useState<ItemInput>(EMPTY);
  const [ca01, setCa01] = useState<Ca01>(INITIAL_CA01);
  const [list, setList] = useState<ListResponse | null>(null);
  const [message, setMessage] = useState<ScreenMessage | null>(null);
  const [busy, setBusy] = useState(false);
  const { register, focus } = useFieldFocus();

  const attrEditable = funcCd === '2' || funcCd === '3';

  const showError = useCallback(
    (e: unknown) => {
      setMessage(errorMessage(e));
      if (e instanceof ApiError) focus(e.body.field, e.body.fieldPos);
    },
    [focus],
  );

  const loadList = useCallback(
    async (cursor: string | null, pageNo: number, move?: PageMove) => {
      const res = await pm01Api.list(cursor, pageNo, move);
      setList(res);
      setCa01((c) => ({ ...c, topItemCd: res.cursor, pageNo: res.pageNo }));
      return res;
    },
    [],
  );

  useEffect(() => {
    loadList(null, 1).catch(showError);
    focus('itemCd');
  }, [loadList, showError, focus]);

  const run = useCallback(
    async (action: () => Promise<void>) => {
      if (busy) return;
      setBusy(true);
      try {
        await action();
      } catch (e) {
        showError(e);
      } finally {
        setBusy(false);
      }
    },
    [busy, showError],
  );

  const enter = useCallback(
    () =>
      run(async () => {
        const updTmsFor = form.itemCd === ca01.heldItemCd ? ca01.updTms : '';
        switch (funcCd) {
          case '1': {
            const res = await pm01Api.inquire(form.itemCd);
            setForm({ itemCd: res.itemCd, itemName: res.itemName, itemKbn: res.itemKbn, stockUnit: res.stockUnit });
            setCa01((c) => ({ ...c, heldItemCd: res.itemCd, updTms: res.updTms }));
            setMessage(infoMessage(res.msgId, res.msgText));
            return;
          }
          case '2': {
            const res = await pm01Api.register(form);
            setCa01((c) => ({ ...c, heldItemCd: res.itemCd, updTms: res.updTms }));
            setMessage(infoMessage(res.msgId, res.msgText));
            break;
          }
          case '3': {
            const res = await pm01Api.update(form, updTmsFor);
            setCa01((c) => ({ ...c, heldItemCd: res.itemCd, updTms: res.updTms }));
            setMessage(infoMessage(res.msgId, res.msgText));
            break;
          }
          case '4': {
            const res = await pm01Api.remove(form.itemCd, updTmsFor);
            setCa01((c) => ({ ...c, heldItemCd: '', updTms: '' }));
            setMessage(infoMessage(res.msgId, res.msgText));
            break;
          }
        }
        await loadList(ca01.topItemCd, ca01.pageNo);
      }),
    [run, funcCd, form, ca01, loadList],
  );

  const page = useCallback(
    (move: PageMove) =>
      run(async () => {
        await loadList(ca01.topItemCd, ca01.pageNo, move);
        setMessage(null);
      }),
    [run, loadList, ca01.topItemCd, ca01.pageNo],
  );

  const clear = useCallback(() => {
    setForm(EMPTY);
    setCa01((c) => ({ ...c, heldItemCd: '', updTms: '' }));
    setMessage(null);
    focus('itemCd');
  }, [focus]);

  const pfKeys = useMemo<PfKeyDef[]>(
    () => [
      { key: 'ENTER', label: '実行', onPress: enter, disabled: busy },
      { key: 'PF7', label: '前頁', onPress: () => page('prev'), disabled: busy },
      { key: 'PF8', label: '次頁', onPress: () => page('next'), disabled: busy },
      { key: 'CLEAR', label: 'クリア', onPress: clear, disabled: busy },
    ],
    [enter, page, clear, busy],
  );

  const set = (k: keyof ItemInput) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const rows = list?.rows ?? [];

  return (
    <ScreenFrame screenId="PM01" title="品目マスタ保守" message={message} pfKeys={pfKeys}>
      <div className="row" />
      <div className="row">
        <label className="lbl" htmlFor="funcCd">機能コード</label>
        <select id="funcCd" ref={register('funcCd', 1)} className="fld" value={funcCd} onChange={(e) => setFuncCd(e.target.value as FuncCd)} data-testid="funcCd">
          {(Object.keys(FUNC_LABELS) as FuncCd[]).map((k) => (
            <option key={k} value={k}>{k}:{FUNC_LABELS[k]}</option>
          ))}
        </select>
        <span className="guide">（1:照会 2:登録 3:更新 4:削除）</span>
      </div>
      <div className="row" />
      <div className="row">
        <label className="lbl" htmlFor="itemCd">品目コード</label>
        <input id="itemCd" ref={register('itemCd', 2)} className="fld w8" maxLength={8} inputMode="numeric" value={form.itemCd} onChange={set('itemCd')} data-testid="itemCd" />
        <span className="guide">（8桁・末尾チェックデジット）</span>
      </div>
      <div className="row">
        <label className="lbl" htmlFor="itemName">品目名</label>
        <input id="itemName" ref={register('itemName', 3)} className="fld w20" maxLength={20} value={form.itemName} onChange={set('itemName')} disabled={!attrEditable} data-testid="itemName" />
      </div>
      <div className="row">
        <label className="lbl" htmlFor="itemKbn">品目区分</label>
        <input id="itemKbn" ref={register('itemKbn', 4)} className="fld w1" maxLength={1} value={form.itemKbn} onChange={set('itemKbn')} disabled={!attrEditable} data-testid="itemKbn" />
        <span className="guide">（1:製品 2:部品 9:消耗品）</span>
      </div>
      <div className="row">
        <label className="lbl" htmlFor="stockUnit">在庫単位</label>
        <input id="stockUnit" ref={register('stockUnit', 5)} className="fld w4" maxLength={4} value={form.stockUnit} onChange={set('stockUnit')} disabled={!attrEditable} data-testid="stockUnit" />
      </div>
      <div className="row" />
      <div className="row list-head">
        <span className="col-cd">品目コード</span>
        <span className="col-name">品目名</span>
        <span className="col-kbn">区分</span>
        <span className="page-no" data-testid="page-no">頁 {list?.pageNo ?? ''}</span>
      </div>
      <div className="list" data-testid="item-list">
        {Array.from({ length: LIST_ROWS }, (_, i) => {
          const r = rows[i];
          return (
            <div
              key={i}
              className={`row list-row${r ? ' has-item' : ''}`}
              data-testid={r ? 'list-row' : undefined}
              onClick={r ? () => setForm((f) => ({ ...f, itemCd: r.itemCd })) : undefined}
            >
              <span className="col-cd">{r?.itemCd ?? ''}</span>
              <span className="col-name">{r?.itemName ?? ''}</span>
              <span className="col-kbn">{r ? `${r.itemKbn}:${KBN_LABELS[r.itemKbn] ?? ''}` : ''}</span>
            </div>
          );
        })}
      </div>
      <div className="row more">
        <span>{list?.hasPrev ? '▲前頁あり(PF7)' : ''}</span>
        <span>{list?.hasNext ? '▼次頁あり(PF8)' : ''}</span>
      </div>
      <div className="row" />
    </ScreenFrame>
  );
}
