# API 契約（バックエンド ⇔ フロントエンド）

対象: `backend/`（Spring Boot 3）と `frontend/`（React）。仕様の正は `docs/migration/spec-catalog.md`（以下「カタログ」）。
本書はカタログ §6（COMMAREA 各項目の扱い）を REST に落としたもの。画面ごとの業務 API は後続チケット（PM01 / PM02 / PM03）で本書のとおりに実装し、契約を変えるときは先に本書を更新する。

- 実装済み（本チケット）: §1〜§4 の共通規約、エラー応答、ヘルスチェック（§6）
- 後続チケットで実装: §5 の業務 API

## 1. 共通規約

| 項目 | 規約 | 根拠 |
|---|---|---|
| ベース URL | `/api`（開発時は Vite が `/api` を backend `:8080` へプロキシ） | `frontend/src/api/client.ts` |
| 形式 | JSON（UTF-8）。項目名は camelCase（DB 列 `ITEM_CD` → `itemCd`） | — |
| 状態 | ステートレス。CICS 疑似会話・COMMAREA はサーバー側に持たない。画面間で引き継ぐ値は画面状態か API パラメータ（カタログ §6） | カタログ C-2、§6 |
| CHAR(n) | 末尾の半角空白（パディング）を除いた文字列で返す。受け取った値はそのまま検証する（前後の空白を勝手に詰めない） | カタログ §5 |
| DEC(p,s) | JSON の数値。サーバー内部は BigDecimal（浮動小数を使わない）。小数部は列の桁数どおり（例 `qtyPer: 2.00`） | カタログ §5、R6 |
| 入力の数値項目 | BMS の入力項目と同じく **文字列** で受け取り、サーバーで検証する（数字以外 → M008）。例 `"ioQty": "10"` | カタログ §3 M008 |
| DATE | `YYYY-MM-DD` の文字列 | カタログ §5 |
| UPD_TMS | DB2 TIMESTAMP の文字表現 `YYYY-MM-DD-HH.MM.SS.ffffff`（26 桁、マイクロ秒まで）。照会応答の `updTms` を、更新・削除要求でそのまま送り返す（§4） | カタログ R3、`PMCOMM.inc` CA01-UPD-TMS |
| システム日付 | サーバーの `SystemDate`（注入可能な `Clock`、Asia/Tokyo）から取る。採番の年は暦年の西暦下 2 桁 | カタログ C-4、D-04、D-05 |

## 2. エラー応答

すべての API のエラーは同じ形で返す（CA-MSG-ID / CA-MSG-FLD-POS の置き換え）。3 項目とも必ず含める（値がなければ `null`）。

```json
{ "msgId": "M001", "message": "品目コードが不正です。チェックデジットを確認してください。", "field": "itemCd" }
```

| 項目 | 内容 |
|---|---|
| `msgId` | PMMSG のメッセージ ID（§3）。業務メッセージ以外のエラー（JSON 形式不正・未定義 API・システムエラー）は `null` |
| `message` | `msgId` の文言（PMMSG の文言を一字一句そのまま）。`msgId=null` のときは下表の固定文言 |
| `field` | エラー項目のリクエスト項目名（例 `itemCd`、`ioQty`、`dueDate`）。画面はこの項目にフォーカスを置く（CA-MSG-FLD-POS 相当）。項目に紐づかないエラーは `null` |

業務メッセージ以外のエラー:

| 状況 | HTTP | message |
|---|---|---|
| JSON 形式不正 | 400 | リクエストの形式が不正です。 |
| 未定義の API | 404 | 指定された API はありません。 |
| 未対応のメソッド | 405 | 指定された操作はできません。 |
| 想定外の例外 | 500 | システムエラーが発生しました。 |

画面固有の追加項目はエラー応答に足してよい（3 項目は必ず残す）。例: PM03 の M015 は `shortages`（§5.3）。

成功時のメッセージ（M005 / M006 / M007 / M012 / M013）は、更新系 API の応答に `msgId` と `message` を含める。照会成功時はメッセージなし（カタログ D-12：メッセージ欄は空白）。

## 3. メッセージ ID と HTTP ステータス

文言は `backend/src/main/java/jp/co/sanseki/pm/common/MessageCatalog.java`（PMMSG.inc と一字一句一致することを単体テストで確認）、フロントは `frontend/src/lib/messages.ts`。

| ID | 種別 | HTTP | 文言 |
|---|---|---|---|
| M001 | 入力エラー | 400 | 品目コードが不正です。チェックデジットを確認してください。 |
| M002 | 該当なし | 404 | 該当する品目は登録されていません。 |
| M003 | 重複 | 409 | 品目コードは既に使用されています。 |
| M004 | 入力エラー | 400 | 必須項目が未入力です。 |
| M005 | 成功 | 201 | 登録しました。 |
| M006 | 成功 | 200 | 更新しました。 |
| M007 | 成功 | 200 | 削除しました。 |
| M008 | 入力エラー | 400 | 数値項目に数字以外が入力されています。 |
| M009 | 入力エラー | 400 | 在庫数量が不正です。 |
| M010 | 入力エラー | 400 | 入出庫区分は1(入庫)または2(出庫)を入力してください。 |
| M011 | 業務エラー | 422 | 在庫が不足しています。出庫数量を確認してください。 |
| M012 | 成功 | 200 | 入出庫を登録しました。 |
| M013 | 成功 | 201 | 製造指示を登録しました。 |
| M014 | 業務エラー | 422 | 部品構成が登録されていません。 |
| M015 | 業務エラー | 422 | 在庫不足の部品があります。23行目にPF4で一覧を表示します。 |
| M016 | 入力エラー | 400 | 完成予定日の形式が不正です。YYYY-MM-DDで入力してください。 |
| M017 | 排他エラー | 409 | 他の端末で更新されています。再照会してください。 |
| M018 | 業務エラー | 422 | 製造指示番号の連番が上限(9999)を超えました。 |

- M018 は新システムで追加（カタログ §8 D-06）。PMMSG.inc には存在しない。frontend の `messages.ts` への追加は PM03 のチケットで行う。
- 入力チェックの評価順は カタログ D-21 の暫定案（必須 → 形式 → 存在・重複 → 業務）。最初に見つかった 1 件だけ返す（旧画面のメッセージ行は 1 行）。

## 4. 楽観的排他制御（UPD_TMS）

1. 照会 API が `updTms`（26 桁文字列）を返す。画面はこれを状態として持つ（旧 CA01-UPD-TMS）。
2. 更新・削除 API は `updTms` を受け取り、更新直前に読み直した ITEM_MST.UPD_TMS とマイクロ秒まで比較する（`OptimisticLock.verify`）。
3. 不一致・未指定・形式不正は M017（409、`field: null`）。照会せずに更新した場合や、照会と別の品目コードを指定した場合も不一致として扱う（カタログ D-11）。
4. 一致すれば更新し、UPD_TMS を現在時刻（マイクロ秒に切り捨て）にする。応答で新しい `updTms` を返す。

旧方式の CA01-LOCK-FLG は使わない。STOCK には楽観排他がない（カタログ D-19：`STOCK_QTY = STOCK_QTY ± 数量` の 1 文で更新）。

## 5. 業務 API（後続チケットで実装）

### 5.1 PM01 品目マスタ保守

| 操作 | 旧 | メソッド・パス | リクエスト | 成功応答 | 主なエラー |
|---|---|---|---|---|---|
| 一覧 | 初期表示 / PF7 / PF8 | `GET /api/items?topItemCd=&direction=` | `topItemCd`: 現在頁の先頭品目コード（省略時は先頭頁）、`direction`: `next` / `prev`（省略時は `topItemCd` から） | 200 `{ items: Item[], topItemCd, hasPrev, hasNext }`（DEL_FLG='0'、ITEM_CD 昇順、10 件） | — |
| 照会 | 機能 1 | `GET /api/items/{itemCd}` | — | 200 `Item` | M002 |
| 登録 | 機能 2 | `POST /api/items` | `{ itemCd, itemName, itemKbn, stockUnit }` | 201 `{ msgId: "M005", message, item: Item }` | M004, M001, M003 |
| 更新 | 機能 3 | `PUT /api/items/{itemCd}` | `{ itemName, itemKbn, stockUnit, updTms }` | 200 `{ msgId: "M006", message, item: Item }` | M004, M001, M002, M017 |
| 削除 | 機能 4 | `DELETE /api/items/{itemCd}?updTms=` | — | 200 `{ msgId: "M007", message }` | M002, M017 |

`Item = { itemCd, itemName, itemKbn, stockUnit, updTms }`

- 一覧の頁送りは CA01-TOP-ITEM-CD を基準にしたキーセット方式（カタログ §6）。頁番号（CA01-PAGE-NO）は画面状態で持つ。端（1 頁目の前頁・最終頁の次頁）は `hasPrev` / `hasNext` でボタンを非活性にする（カタログ D-09）。
- frontend PR #6 のモック `GET /api/items?page=` は暫定契約。PM01 のチケットで本書の形に合わせる。
- 論理削除済みの品目は照会・更新・削除で M002、登録は主キー重複として M003（カタログ C-8、D-10）。

### 5.2 PM02 在庫照会・入出庫登録

| 操作 | メソッド・パス | リクエスト | 成功応答 | 主なエラー |
|---|---|---|---|---|
| 在庫照会 | `GET /api/stocks/{itemCd}` | — | 200 `{ itemCd, itemName, itemKbn, stockQty }` | M002 |
| 入出庫登録 | `POST /api/stocks/{itemCd}/movements` | `{ ioKbn: "1" \| "2", ioQty: "数字文字列" }` | 200 `{ msgId: "M012", message, itemCd, itemName, stockQty }`（更新後在庫） | M004, M008, M009, M010, M002, M011 |

### 5.3 PM03 製造指示登録

| 操作 | メソッド・パス | リクエスト | 成功応答 | 主なエラー |
|---|---|---|---|---|
| 製品照会（製品名表示） | `GET /api/items/{itemCd}` | — | 200 `Item` | M002 |
| 製造指示登録 | `POST /api/work-orders` | `{ itemCd, orderQty: "数字文字列", dueDate: "YYYY-MM-DD" }` | 201 `{ msgId: "M013", message, workOrderNo, itemCd, itemName }` | M004, M008, M016, M002, M014, M015, M018 |

M015 のエラー応答は不足部品一覧を含める（旧 CA03-NG-CNT / CA03-NG-LIST。PF4 の一覧表示は画面側で切り替える）:

```json
{
  "msgId": "M015", "message": "在庫不足の部品があります。23行目にPF4で一覧を表示します。", "field": null,
  "shortages": [ { "itemCd": "20000035", "needQty": 106, "stockQty": 60 } ]
}
```

- `shortages` は部品コード昇順で最大 10 件（カタログ D-07）。件数は配列長（CA03-NG-CNT）。
- 不足時は登録しない・在庫は減算しない（カタログ D-15）。
- 採番は SEQ_CTL を UPDATE してから SELECT。システム日付の西暦下 2 桁 ≠ SEQ_YY なら SEQ_YY を更新して SEQ_NO=1、連番が 9999 を超える場合は M018（カタログ R7、D-05、D-06）。

## 6. ヘルスチェック

`GET /actuator/health`（Spring Boot Actuator）。`db`（DB 接続）と `sanseki`（SANSEKI スキーマ 5 テーブルの件数）を返す。

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "H2", "validationQuery": "isValid()" } },
    "sanseki": { "status": "UP", "details": { "schema": "SANSEKI",
      "rowCounts": { "ITEM_MST": 15, "BOM": 9, "STOCK": 15, "WORK_ORDER": 2, "SEQ_CTL": 1 } } }
  }
}
```

## 7. COMMAREA 項目との対応（カタログ §6 の要約）

| COMMAREA 項目 | API での扱い |
|---|---|
| CA-SCRN-ID / CA-FIRST-TIME / CA01-LOCK-FLG / CA-LENGTH | 持たない（画面状態・初期表示処理で代替、または廃止） |
| CA-FUNC-CD | PM01 は機能ごとにメソッドを分ける（GET / POST / PUT / DELETE） |
| CA01-ITEM-CD / CA02-ITEM-CD / CA03-ITEM-CD | パスまたはリクエストの `itemCd` |
| CA01-PAGE-NO | 画面状態 |
| CA01-TOP-ITEM-CD | 一覧 API の `topItemCd` |
| CA01-UPD-TMS | 照会応答・更新要求の `updTms`（§4） |
| CA02-IO-KBN | `ioKbn` |
| CA03-ORDER-QTY / CA03-DUE-DATE | `orderQty` / `dueDate` |
| CA03-NG-CNT / CA03-NG-LIST | M015 エラー応答の `shortages` |
| CA-MSG-ID / CA-MSG-FLD-POS | エラー応答の `msgId` / `field`（§2） |
