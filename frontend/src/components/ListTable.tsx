import type { ReactNode } from 'react';

export interface Column<T> {
  header: string;
  align?: 'left' | 'right';
  className?: string;
  render: (row: T) => ReactNode;
}

interface Props<T> {
  title: string;
  aside?: ReactNode;
  columns: Column<T>[];
  rows: T[];
  /** 表示行数（不足分は空行で埋める） */
  size: number;
  testId: string;
  /** 行の data-testid 接頭辞（01〜size の連番を付与） */
  rowTestIdPrefix: string;
}

/** 固定行数の一覧テーブル（ゼブラ表示） */
export function ListTable<T>({ title, aside, columns, rows, size, testId, rowTestIdPrefix }: Props<T>) {
  const padded: (T | null)[] = [...rows];
  while (padded.length < size) padded.push(null);

  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-3">
        <h2 className="text-sm font-bold text-slate-700">{title}</h2>
        {aside}
      </div>
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-xs text-slate-500">
          <tr>
            <th scope="col" className="w-12 px-4 py-2 text-right font-medium">
              No.
            </th>
            {columns.map((c) => (
              <th
                key={c.header}
                scope="col"
                className={`px-4 py-2 font-medium ${c.align === 'right' ? 'text-right' : 'text-left'} ${c.className ?? ''}`}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody data-testid={testId} className="divide-y divide-slate-100">
          {padded.map((r, i) => (
            <tr
              key={i}
              data-testid={`${rowTestIdPrefix}${String(i + 1).padStart(2, '0')}`}
              className="h-9 even:bg-slate-50"
            >
              <td className="px-4 text-right text-xs tabular-nums text-slate-400">{r ? i + 1 : ''}</td>
              {columns.map((c) => (
                <td
                  key={c.header}
                  className={`px-4 ${c.align === 'right' ? 'text-right tabular-nums' : ''} ${c.className ?? ''}`}
                >
                  {r ? c.render(r) : null}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
