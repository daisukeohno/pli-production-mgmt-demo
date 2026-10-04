interface Props {
  message: string;
  onRestart: () => void;
}

/** PF3（終了）後の画面 */
export function EndedPanel({ message, onRestart }: Props) {
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <p className="mb-6 text-slate-700">{message}</p>
        <button
          type="button"
          onClick={onRestart}
          autoFocus
          className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
        >
          再開
        </button>
      </div>
    </div>
  );
}
