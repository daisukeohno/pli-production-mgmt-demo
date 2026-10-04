import { useState } from 'react';
import { Pm01 } from './screens/Pm01';
import { Pm02 } from './screens/Pm02';

type ScreenId = 'PM01' | 'PM02';

const SCREENS: { id: ScreenId; label: string }[] = [
  { id: 'PM01', label: 'PM01 品目マスタ保守' },
  { id: 'PM02', label: 'PM02 在庫照会・入出庫登録' },
];

export function App() {
  const [screenId, setScreenId] = useState<ScreenId>('PM01');
  return (
    <div className="app">
      <nav className="screen-select" aria-label="画面選択" style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
        {SCREENS.map((s) => (
          <button key={s.id} type="button" aria-pressed={screenId === s.id} onClick={() => setScreenId(s.id)}>
            {s.label}
          </button>
        ))}
      </nav>
      {screenId === 'PM01' ? <Pm01 /> : <Pm02 />}
    </div>
  );
}
