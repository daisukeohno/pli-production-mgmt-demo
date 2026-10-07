# frontend（生産管理システム 新UI）

React 19 + TypeScript + Vite + Tailwind CSS v4 + shadcn/ui。3270（BMS）画面 PM01〜PM03 を置き換える新 UI の骨格。

## コマンド

```bash
npm ci
npm run dev        # http://localhost:5173 （/api → http://localhost:8080 にプロキシ）
npm test           # vitest
npm run typecheck
npm run build      # tsc -b && vite build
```

- backend が未実装の間は **モック API**（`src/api/mock`）で応答する。ヘッダーに「モック API」バッジが出る。
- 実 backend に繋ぐときは `VITE_API_MOCK=false npm run dev`（プロキシ先は `VITE_BACKEND_URL` で変更可）。

## 画面と URL

| 画面ID | 画面 | URL | 旧 PF キー（docs/design.md） |
|---|---|---|---|
| PM01 | 品目マスタ保守 | `/items` | F7 前頁 / F8 次頁 |
| PM02 | 在庫照会・入出庫登録 | `/stock` | — |
| PM03 | 製造指示登録 | `/work-orders` | F4 不足部品一覧 |
| — | UI 部品見本（開発用） | `/dev/components` | F7 / F8 |

3 画面は現時点では空ページ（画面移行チケットで実装）。

## 旧画面からの置き換え方針

| 旧（BMS 24x80） | 新 UI | 実装 |
|---|---|---|
| 1 行目 画面ID・タイトル・日付 | ページ見出し（画面ID バッジ）+ ヘッダー日付 `YYYY/MM/DD` | `components/layout/app-layout.tsx`, `components/page-header.tsx` |
| 23 行目 メッセージ（CA-MSG-ID） | トースト + 項目下のインラインエラー。文言は PMMSG と一字一句同じ | `lib/messages.ts`, `lib/notify.ts`, `components/form-field.tsx` |
| カーソル位置（CA-MSG-FLD-POS） | エラー項目へフォーカス | 各画面で `validateForm` の結果 / API の `field` を使う |
| 24 行目 PF キーガイド | 画面下部のボタンバー。物理 F キーでも同じ操作（ブラウザ既定動作は抑止） | `components/function-key-bar.tsx`, `hooks/use-function-keys.ts` |
| 一覧 10 行（F-LIST01〜10） | テーブル + ページ送り（10 件/頁） | `components/data-table.tsx`, `components/pager.tsx`, `lib/constants.ts` |

## 共通部品

- `api/client.ts` … `apiRequest()`。業務エラーは `ApiError { status, msgId, field }`（旧 CA-MSG-ID / CA-MSG-FLD-POS 相当）。
- `lib/validation.ts` … `required`(M004) / `numeric`(M008) / `isoDate`(M016) と、BMS 項目順に評価して最初の 1 件だけ返す `validateForm()`。
  チェックデジット・存在チェック・在庫不足などの業務判定の正は backend。フロントの事前チェックで旧とメッセージの出方が変わらないよう、画面実装時は旧の判定順に合わせること。
- `components/ui/*` … shadcn/ui（`npx shadcn@latest add <name>` で追加）。

## テスト

- `lib/messages.test.ts` … `src/include/PMMSG.inc` を読み込み、全 17 メッセージが一字一句一致することを検証。
- `App.test.tsx` … 3 画面へのナビゲーション、ヘッダー日付、フォーム検証（M004/M016・フォーカス）、F7/F8 ページング。
- `components/function-key-bar.test.tsx`, `lib/validation.test.ts`, `api/mock/handlers.test.ts` ほか。
