import { useCallback, useRef } from 'react';

type Focusable = HTMLInputElement | HTMLSelectElement;

/**
 * エラー項目へのカーソル位置付け（COMMAREA CA-MSG-FLD-POS 相当）。
 * 各入力項目を項目名と BMS 項目順の位置で登録し、ErrorResponse の field / fieldPos でフォーカスする。
 */
export function useFieldFocus() {
  const fields = useRef(new Map<string, { pos: number; el: Focusable | null }>());

  const register = useCallback(
    (name: string, pos: number) => (el: Focusable | null) => {
      fields.current.set(name, { pos, el });
    },
    [],
  );

  const focus = useCallback((field: string | null | undefined, fieldPos?: number) => {
    let target: Focusable | null | undefined = field ? fields.current.get(field)?.el : undefined;
    if (!target && fieldPos) {
      target = [...fields.current.values()].find((f) => f.pos === fieldPos)?.el;
    }
    if (target && !target.disabled) {
      target.focus();
      if (target instanceof HTMLInputElement) target.select();
    }
  }, []);

  return { register, focus };
}
