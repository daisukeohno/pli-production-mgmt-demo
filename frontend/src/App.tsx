import { useState } from 'react';
import { Pm01 } from './screens/Pm01';
import { Pm03 } from './screens/Pm03';

const SCREENS = [
  { id: 'PM01', label: 'PM01 品目マスタ保守' },
  { id: 'PM03', label: 'PM03 製造指示登録' },
] as const;

export function App() {
  const [screen, setScreen] = useState<(typeof SCREENS)[number]['id']>('PM01');
  return (
    <div>
      <nav className="screen-switch">
        {SCREENS.map((s) => (
          <button
            key={s.id}
            type="button"
            disabled={s.id === screen}
            onClick={() => setScreen(s.id)}
          >
            {s.label}
          </button>
        ))}
      </nav>
      {/* key で画面切替時に再マウントし、各画面を初回起動から始める */}
      {screen === 'PM01' ? <Pm01 key="PM01" /> : <Pm03 key="PM03" />}
    </div>
  );
}
