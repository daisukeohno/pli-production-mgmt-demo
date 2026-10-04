import {
  forwardRef,
  useRef,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type SelectHTMLAttributes,
} from 'react';

/** CA-MSG-FLD-POS 相当：項目へカーソルを置き、テキスト項目は全選択する */
export function focusField(el: HTMLElement | null | undefined) {
  el?.focus();
  if (el instanceof HTMLInputElement) el.select();
}

const CONTROL_BASE =
  'block rounded-md border bg-white px-3 py-2 text-sm text-slate-900 shadow-sm transition ' +
  'placeholder:text-slate-400 focus:outline-none focus:ring-2 ' +
  'read-only:cursor-default read-only:border-slate-200 read-only:bg-slate-100 read-only:text-slate-500 read-only:shadow-none ' +
  'disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-500 disabled:shadow-none';

const controlClass = (invalid: boolean | undefined, extra = '') =>
  `${CONTROL_BASE} ${
    invalid
      ? 'border-red-500 ring-2 ring-red-200 focus:border-red-500 focus:ring-red-300'
      : 'border-slate-300 focus:border-blue-500 focus:ring-blue-200'
  } ${extra}`;

type TextInputProps = InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean };

export const TextInput = forwardRef<HTMLInputElement, TextInputProps>(function TextInput(
  { invalid, className, ...rest },
  ref,
) {
  return (
    <input
      ref={ref}
      type="text"
      autoComplete="off"
      aria-invalid={invalid || undefined}
      className={controlClass(invalid, className)}
      {...rest}
    />
  );
});

type SelectInputProps = SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean };

export const SelectInput = forwardRef<HTMLSelectElement, SelectInputProps>(function SelectInput(
  { invalid, className, children, ...rest },
  ref,
) {
  return (
    <select
      ref={ref}
      aria-invalid={invalid || undefined}
      className={controlClass(invalid, `pr-8 ${className ?? ''}`)}
      {...rest}
    >
      {children}
    </select>
  );
});

export interface Option {
  value: string;
  label: string;
}

/** 選択肢に無い値（サーバ応答）もそのまま表示できるよう、必要に応じて選択肢を補う */
export function withCurrent(options: Option[], value: string): Option[] {
  return value === '' || options.some((o) => o.value === value) ? options : [...options, { value, label: value }];
}

interface SegmentedProps {
  id: string;
  labelledBy: string;
  options: Option[];
  value: string;
  onChange: (value: string) => void;
  invalid?: boolean;
  /** フォーカス移動先（選択中、未選択時は先頭のボタン） */
  focusRef?: (el: HTMLButtonElement | null) => void;
}

/** セグメントボタン（radiogroup）。矢印キーまたは値の数字キーで選択できる */
export function SegmentedControl({ id, labelledBy, options, value, onChange, invalid, focusRef }: SegmentedProps) {
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  const selectedIdx = options.findIndex((o) => o.value === value);
  const tabIdx = selectedIdx >= 0 ? selectedIdx : 0;

  const choose = (i: number) => {
    onChange(options[i].value);
    buttons.current[i]?.focus();
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const n = options.length;
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      e.preventDefault();
      choose(selectedIdx < 0 ? 0 : (selectedIdx + 1) % n);
    } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      e.preventDefault();
      choose(selectedIdx < 0 ? n - 1 : (selectedIdx - 1 + n) % n);
    } else {
      const i = options.findIndex((o) => o.value === e.key);
      if (i >= 0) {
        e.preventDefault();
        choose(i);
      }
    }
  };

  return (
    <div
      id={id}
      role="radiogroup"
      aria-labelledby={labelledBy}
      aria-invalid={invalid || undefined}
      onKeyDown={onKeyDown}
      className={`inline-flex rounded-lg border bg-slate-100 p-1 ${
        invalid ? 'border-red-500 ring-2 ring-red-200' : 'border-slate-200'
      }`}
    >
      {options.map((o, i) => {
        const checked = o.value === value;
        return (
          <button
            key={o.value}
            ref={(el) => {
              buttons.current[i] = el;
              if (i === tabIdx) focusRef?.(el);
            }}
            type="button"
            role="radio"
            aria-checked={checked}
            tabIndex={i === tabIdx ? 0 : -1}
            onClick={() => onChange(o.value)}
            className={`flex items-center gap-1.5 rounded-md px-4 py-1.5 text-sm font-medium transition focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
              checked ? 'bg-white text-blue-700 shadow-sm ring-1 ring-slate-200' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span className={`text-xs tabular-nums ${checked ? 'text-blue-400' : 'text-slate-400'}`}>{o.value}</span>{' '}
            {o.label}
          </button>
        );
      })}
    </div>
  );
}

interface FormRowProps {
  label: string;
  htmlFor?: string;
  labelId?: string;
  hint?: ReactNode;
  children: ReactNode;
}

/** ラベル付きフォーム行（FormGrid 内で使用） */
export function FormRow({ label, htmlFor, labelId, hint, children }: FormRowProps) {
  return (
    <>
      <label id={labelId} htmlFor={htmlFor} className="pt-2 text-sm font-medium text-slate-600 sm:text-right">
        {label}
      </label>
      <div className="flex flex-wrap items-center gap-3">
        {children}
        {hint != null && <span className="text-sm text-slate-500">{hint}</span>}
      </div>
    </>
  );
}

export function FormGrid({ children }: { children: ReactNode }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="grid grid-cols-1 items-start gap-x-6 gap-y-4 sm:grid-cols-[9rem_1fr]">{children}</div>
    </section>
  );
}
