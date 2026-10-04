import { useState } from 'react';
import { Pm01 } from './screens/Pm01';
import { Pm02 } from './screens/Pm02';
import { Pm03 } from './screens/Pm03';

type ScreenId = 'PM01' | 'PM02' | 'PM03';

const SCREENS: { id: ScreenId; label: string }[] = [
  { id: 'PM01', label: 'PM01 品目マスタ保守' },
  { id: 'PM02', label: 'PM02 在庫照会・入出庫登録' },
  { id: 'PM03', label: 'PM03 製造指示登録' },
];

export function App() {
  const [screenId, setScreenId] = useState<ScreenId>('PM01');
  return (
    <div className="app">
      <nav className="screen-select" aria-label="画面選択" style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
        {SCREENS.map((s) => (
          <button
            key={s.id}
            type="button"
            aria-pressed={screenId === s.id}
            onClick={() => setScreenId(s.id)}
            style={screenId === s.id ? { fontWeight: 'bold', background: '#1f6feb', color: '#fff', borderColor: '#1f6feb' } : undefined}
          >
            {s.label}
          </button>
        ))}
      </nav>
      {/* key で画面切替時に再マウントし、各画面を初回起動から始める */}
      {screenId === 'PM01' && <Pm01 key="PM01" />}
      {screenId === 'PM02' && <Pm02 key="PM02" />}
      {screenId === 'PM03' && <Pm03 key="PM03" />}
    </div>
  );
}
