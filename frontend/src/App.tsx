import { useState } from 'react';
import { Pm01 } from './screens/Pm01';
import { Pm02 } from './screens/Pm02';
import { Pm03 } from './screens/Pm03';

type ScreenId = 'PM01' | 'PM02' | 'PM03';

const SCREENS: { id: ScreenId; name: string }[] = [
  { id: 'PM01', name: '品目マスタ保守' },
  { id: 'PM02', name: '在庫照会・入出庫登録' },
  { id: 'PM03', name: '製造指示登録' },
];

export function App() {
  const [screenId, setScreenId] = useState<ScreenId>('PM01');
  return (
    <div className="flex min-h-screen">
      <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col border-r border-slate-200 bg-white">
        <div className="flex h-14 items-center border-b border-slate-200 px-5 text-xs font-bold tracking-wider text-slate-400">
          業務メニュー
        </div>
        <nav className="screen-select flex flex-col gap-1 p-3" aria-label="画面選択">
          {SCREENS.map((s) => {
            const active = screenId === s.id;
            return (
              <button
                key={s.id}
                type="button"
                aria-pressed={active}
                aria-current={active ? 'page' : undefined}
                onClick={() => setScreenId(s.id)}
                className={`flex items-center gap-3 rounded-lg border-l-4 px-3 py-2.5 text-left text-sm whitespace-nowrap transition focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
                  active
                    ? 'border-blue-600 bg-blue-50 font-bold text-blue-700'
                    : 'border-transparent text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <span
                  className={`rounded px-1.5 py-0.5 text-xs font-bold ${
                    active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  {s.id}
                </span>{' '}
                <span>{s.name}</span>
              </button>
            );
          })}
        </nav>
      </aside>
      <div className="min-w-0 flex-1">
        {/* key で画面切替時に再マウントし、各画面を初回起動から始める */}
        {screenId === 'PM01' && <Pm01 key="PM01" />}
        {screenId === 'PM02' && <Pm02 key="PM02" />}
        {screenId === 'PM03' && <Pm03 key="PM03" />}
      </div>
    </div>
  );
}
