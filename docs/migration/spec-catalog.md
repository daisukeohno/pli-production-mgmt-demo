# 画面仕様・業務ルールカタログ（移行の正）

対象: サンプル精機株式会社 生産管理システム（PL/I / CICS / DB2）→ Java（Spring Boot）+ モダン UI への 1:1 移行

## 0. 本書の位置づけ

- PL/I / CICS / DB2 の実機は使えないため、仕様の正は次の資産のみとする（運用マニュアルよりコードを優先）。
  `docs/design.md`、`src/include/PMCOMM.inc`、`src/include/PMMSG.inc`、`src/include/DCL_*.inc`、`src/pli/PMUTL01.pli`、`db2/ddl/*.sql`、`db2/data/*.csv`、`README.md`
- 各画面のメイン処理（PM01/PM02/PM03 本体）と BMS マップ本体（.bms）はリポジトリに存在しない（`README.md:L32-L35`）。
  そのため、本体ロジックに依存する項目は資産から読み取れる範囲で記述し、確定できない点は「推定」と明記して §8 の要判断事項に挙げる。
- 出典は `ファイル:L行` 形式で記す（行番号は main ブランチ 0c4054b 時点）。
- 本書の機械チェック（メッセージ文言の一字一句一致、出典行の存在、COMMAREA/BMS 項目の網羅）は `docs/migration/verify_spec_catalog.py` で行う。

凡例: **確定** = 資産に明記 / **推定** = 資産からの推論（後続チケットで扱いを確定させる）

---

## 1. 画面共通

| # | 項目 | 仕様 | 区分 | 出典 |
|---|---|---|---|---|
| C-1 | 画面構成 | 3 画面のみ: PM01 品目マスタ保守 / PM02 在庫照会・入出庫登録 / PM03 製造指示登録 | 確定 | `README.md:L12-L15` |
| C-2 | 実行方式 | CICS 疑似会話型（1 往復ごとにタスク終了、状態は COMMAREA で引き継ぐ）。新システムでは COMMAREA を画面状態 / API パラメータに置き換える（§6） | 確定 | `README.md:L10`、`src/include/PMCOMM.inc:L11-L36` |
| C-3 | 画面サイズ・行割り | 24x80。1 行目: `PM01 品目マスタ保守 YYYY/MM/DD`、23 行目: メッセージ、24 行目: PF キーガイド | 確定（PM01 のみ明記。PM02/PM03 も同じ割付と推定） | `docs/design.md:L8` |
| C-4 | ヘッダー日付 | 表示形式は `YYYY/MM/DD`（スラッシュ区切り）。共通ユーティリティ GET-SYS-DATE の返却値は `YYYY-MM-DD`（ハイフン区切り）なので表示時に区切りが異なる。日付は CICS ASKTIME = ホストのローカル時刻（新システムは Asia/Tokyo で取得） | 確定（タイムゾーンは推定） | `docs/design.md:L8`、`src/pli/PMUTL01.pli:L88-L98` |
| C-5 | メッセージ表示 | F-MSG（60 桁、OUT）に PMMSG の文言を表示。メッセージ ID は COMMAREA の CA-MSG-ID（CHAR(4)）で保持 | 確定 | `docs/design.md:L18`、`src/include/PMCOMM.inc:L35`、`src/include/PMMSG.inc:L10-L12` |
| C-6 | エラー項目カーソル | エラー時、CA-MSG-FLD-POS（BMS 項目順）の項目にカーソルを置く | 確定 | `src/include/PMCOMM.inc:L36` |
| C-7 | 品目区分コード値 | '1':製品 '2':部品 '9':消耗品 | 確定 | `db2/ddl/01_item_mst.sql:L13`、`src/include/DCL_ITEM_MST.inc:L19-L21` |
| C-8 | 論理削除済み品目 | DEL_FLG='1' の品目は削除済み。一覧に出さない。照会・PM02・PM03 からも「存在しない」扱い（M002）と推定 | 一覧は確定、他は推定 | `db2/ddl/01_item_mst.sql:L15`、`docs/design.md:L48`、`docs/design.md:L67` |

## 2. 画面別 項目一覧・機能・PF キー

### 2.1 PM01 品目マスタ保守

#### 項目一覧

| 項目名(BMS) | 内容 | 長さ | 入出力 | 対応 DB 列 / 型 | 出典 |
|---|---|---|---|---|---|
| F-FUNC | 機能コード 1:照会 2:登録 3:更新 4:削除 | 1 | IN | （COMMAREA CA-FUNC-CD） | `docs/design.md:L12`、`src/include/PMCOMM.inc:L14` |
| F-ITEMCD | 品目コード（8 桁、末尾チェックデジット） | 8 | IN | ITEM_MST.ITEM_CD CHAR(8) | `docs/design.md:L13`、`db2/ddl/01_item_mst.sql:L11` |
| F-ITEMNM | 品目名 | 20 | IN（機能 2,3 のみ） | ITEM_MST.ITEM_NAME CHAR(20) | `docs/design.md:L14`、`db2/ddl/01_item_mst.sql:L12` |
| F-KBN | 品目区分 1/2/9 | 1 | IN（機能 2,3 のみ） | ITEM_MST.ITEM_KBN CHAR(1) | `docs/design.md:L15`、`db2/ddl/01_item_mst.sql:L13` |
| F-UNIT | 単位 | 4 | IN（機能 2,3 のみ） | ITEM_MST.STOCK_UNIT CHAR(4) | `docs/design.md:L16`、`db2/ddl/01_item_mst.sql:L14` |
| F-LIST01〜F-LIST10 | 一覧行（品目コード+品目名+区分） | 各 33 | OUT | ITEM_CD / ITEM_NAME / ITEM_KBN | `docs/design.md:L17` |
| F-MSG | メッセージ表示 | 60 | OUT | — | `docs/design.md:L18` |

- 一覧の行レイアウト（33 桁の内訳。8+20+1=29 桁なので区切り 4 桁）は BMS 本体がないため不明。新 UI では「品目コード / 品目名 / 区分」の 3 列表として表示する（推定）。
- 一覧は DEL_FLG='0' のみ、ITEM_CD 昇順（CA01-TOP-ITEM-CD による頁送りから推定）。

#### 機能コード

| 機能 | 入力項目 | 処理 | 成功メッセージ | 主なエラー | 区分 | 出典 |
|---|---|---|---|---|---|---|
| 1 照会 | F-ITEMCD | ITEM_MST を読み F-ITEMNM / F-KBN / F-UNIT に表示。UPD_TMS を CA01-UPD-TMS へ退避 | （規定なし） | M002 | 確定（成功時メッセージは不明） | `docs/design.md:L12`、`docs/design.md:L49`、`src/include/PMCOMM.inc:L20` |
| 2 登録 | F-ITEMCD, F-ITEMNM, F-KBN, F-UNIT | チェックデジット検証 → 重複確認 → INSERT（DEL_FLG='0'、UPD_TMS/CRT_TMS=現在時刻） | M005 | M001, M003, M004 | 確定（INSERT 値は推定） | `docs/design.md:L47`、`docs/design.md:L64-L65`、`src/include/PMMSG.inc:L16-L18` |
| 3 更新 | F-ITEMCD, F-ITEMNM, F-KBN, F-UNIT | チェックデジット検証 → UPD_TMS 再比較（楽観排他） → UPDATE（UPD_TMS=現在時刻） | M006 | M001, M002, M004, M017 | 確定 | `docs/design.md:L47`、`docs/design.md:L49`、`src/include/PMMSG.inc:L19` |
| 4 削除 | F-ITEMCD | UPD_TMS 再比較（楽観排他） → DEL_FLG='1' に UPDATE（物理削除しない） | M007 | M002, M017 | 確定 | `docs/design.md:L48-L49`、`src/include/PMMSG.inc:L20` |

#### PF キー

| キー | 動作 | 区分 | 出典 |
|---|---|---|---|
| Enter | 機能コードに従って処理（上表） | 推定 | `docs/design.md:L12` |
| PF7 | 一覧 前頁（10 件/頁） | 確定 | `docs/design.md:L20` |
| PF8 | 一覧 次頁（10 件/頁） | 確定 | `docs/design.md:L20` |
| PF9 | 旧・一括出力。**デッドコード**（PF キー分岐にコメントアウトされた旧ロジックとして残る）。新システムでは実装しない。押下時の挙動は §8 D-09 | 確定 | `docs/design.md:L50` |

### 2.2 PM02 在庫照会・入出庫登録

#### 項目一覧

| 項目名(BMS) | 内容 | 長さ | 入出力 | 対応 DB 列 / 型 | 出典 |
|---|---|---|---|---|---|
| F-ITEMCD | 品目コード | 8 | IN | STOCK.ITEM_CD / ITEM_MST.ITEM_CD CHAR(8) | `docs/design.md:L25`、`db2/ddl/03_stock.sql:L10` |
| F-ITEMNM | 品目名（照会結果） | 20 | OUT | ITEM_MST.ITEM_NAME CHAR(20) | `docs/design.md:L26` |
| F-STOCKQTY | 現在庫数量 | 9 | OUT | STOCK.STOCK_QTY DEC(9,0) | `docs/design.md:L27`、`db2/ddl/03_stock.sql:L11` |
| F-IOKBN | 入出庫区分 1:入庫 2:出庫 | 1 | IN | （COMMAREA CA02-IO-KBN） | `docs/design.md:L28`、`src/include/PMCOMM.inc:L24` |
| F-IOQTY | 入出庫数量 | 9 | IN | — | `docs/design.md:L29` |
| F-MSG | メッセージ表示 | 60 | OUT | — | `docs/design.md:L30` |

#### 機能

| 機能 | 入力項目 | 処理 | 成功メッセージ | 主なエラー | 区分 | 出典 |
|---|---|---|---|---|---|---|
| 在庫照会 | F-ITEMCD | ITEM_MST と STOCK を読み F-ITEMNM / F-STOCKQTY を表示 | （規定なし） | M002 | 確定（成功時メッセージは不明） | `docs/design.md:L71` |
| 入庫（F-IOKBN=1） | F-ITEMCD, F-IOKBN, F-IOQTY | STOCK_QTY = STOCK_QTY + 入出庫数量 | M012 | M004, M008, M009, M010 | 確定（エラー条件は推定） | `docs/design.md:L72`、`src/include/PMMSG.inc:L25` |
| 出庫（F-IOKBN=2） | F-ITEMCD, F-IOKBN, F-IOQTY | 出庫後在庫 < 0 のとき、品目区分 '9'（消耗品）なら許容してマイナス在庫で更新、それ以外は M011 で更新しない | M012 | M011 ほか入庫と同じ | 確定 | `docs/design.md:L51`、`docs/design.md:L73-L75`、`db2/ddl/03_stock.sql:L11` |

#### PF キー

PM02 固有の PF キーは資産に記載なし（Enter のみと推定）。

### 2.3 PM03 製造指示登録

#### 項目一覧

| 項目名(BMS) | 内容 | 長さ | 入出力 | 対応 DB 列 / 型 | 出典 |
|---|---|---|---|---|---|
| F-ITEMCD | 製品コード | 8 | IN | WORK_ORDER.ITEM_CD CHAR(8) | `docs/design.md:L35`、`db2/ddl/04_work_order.sql:L11` |
| F-ITEMNM | 製品名 | 20 | OUT | ITEM_MST.ITEM_NAME CHAR(20) | `docs/design.md:L36` |
| F-ORDERQTY | 指示数量 | 9 | IN | WORK_ORDER.ORDER_QTY DEC(9,0) | `docs/design.md:L37`、`db2/ddl/04_work_order.sql:L12` |
| F-DUEDATE | 完成予定日 YYYY-MM-DD | 10 | IN | WORK_ORDER.DUE_DATE DATE | `docs/design.md:L38`、`db2/ddl/04_work_order.sql:L13` |
| F-ORDERNO | 発行された製造指示番号 | 7 | OUT | WORK_ORDER.WORK_ORDER_NO CHAR(7) | `docs/design.md:L39`、`db2/ddl/04_work_order.sql:L10` |
| F-NGLIST01〜10 | 在庫不足部品一覧（PF4 で表示） | 各 30 | OUT | CA03-NG-LIST（品目コード / 必要数 / 在庫数） | `docs/design.md:L40`、`src/include/PMCOMM.inc:L30-L33` |
| F-MSG | メッセージ表示 | 60 | OUT | — | `docs/design.md:L41` |

- 不足一覧 1 行 30 桁の内訳（8+9+9=26 桁 + 区切り 4 桁）は BMS 本体がないため不明。新 UI では「部品コード / 必要数 / 在庫数」の 3 列表として表示する（推定）。

#### 機能

| 機能 | 入力項目 | 処理 | 成功メッセージ | 主なエラー | 区分 | 出典 |
|---|---|---|---|---|---|---|
| 製造指示登録 | F-ITEMCD, F-ORDERQTY, F-DUEDATE | 製品の存在確認 → BOM 取得 → 子部品ごとに必要数を計算し在庫と比較（R6） → 不足なしなら採番（R7）して WORK_ORDER に INSERT（STATUS='0'）し F-ORDERNO に表示 | M013 | M002, M004, M008, M014, M015, M016 | 確定（不足時に登録しないこと、STATUS 値は推定） | `docs/design.md:L52-L53`、`docs/design.md:L78-L83`、`db2/ddl/04_work_order.sql:L14` |

#### PF キー

| キー | 動作 | 区分 | 出典 |
|---|---|---|---|
| Enter | 製造指示登録 | 推定 | `docs/design.md:L78` |
| PF4 | 在庫不足部品一覧（CA03-NG-LIST）を F-NGLIST01〜10 に表示（M015 の後で使う） | 確定 | `docs/design.md:L40`、`src/include/PMMSG.inc:L28` |

### 2.4 PF キー まとめ

| キー | PM01 | PM02 | PM03 | 新システムでの扱い |
|---|---|---|---|---|
| PF4 | — | — | 在庫不足一覧表示 | PM03 の「不足一覧を表示」操作（M015 の応答に一覧データを含め、UI で表示切替） |
| PF7 | 一覧 前頁 | — | — | PM01 一覧の「前頁」ボタン |
| PF8 | 一覧 次頁 | — | — | PM01 一覧の「次頁」ボタン |
| PF9 | デッドコード（旧・一括出力） | — | — | **実装しない（廃止）** |

## 3. メッセージ一覧（M001〜M017）

文言は `src/include/PMMSG.inc` の INIT 値から CHAR(60) の末尾空白（パディング）を除いたもの。**一字一句このまま使う**（全角句点「。」、M010 の半角括弧 `(` `)`、M015 の半角 `23` `PF4`、M016 の半角 `YYYY-MM-DD` を含む）。

| ID | 文言 | 使用画面 | 表示条件 | 区分 | 出典 |
|---|---|---|---|---|---|
| M001 | 品目コードが不正です。チェックデジットを確認してください。 | PM01 | 登録（2）・更新（3）で品目コードのチェックデジット検証が不正（R1） | 確定 | `src/include/PMMSG.inc:L14`、`docs/design.md:L47`、`docs/design.md:L65` |
| M002 | 該当する品目は登録されていません。 | PM01, PM02, PM03 | 入力した品目（製品）コードが ITEM_MST に存在しない（DEL_FLG='1' も含むと推定） | 確定（PM03）/ 推定（PM01, PM02） | `src/include/PMMSG.inc:L15`、`docs/design.md:L83` |
| M003 | 品目コードは既に使用されています。 | PM01 | 登録（2）で同じ ITEM_CD が既に存在（主キー重複。論理削除済みも含む） | 推定 | `src/include/PMMSG.inc:L16`、`db2/ddl/01_item_mst.sql:L18` |
| M004 | 必須項目が未入力です。 | PM01, PM02, PM03 | 機能ごとの必須入力項目が空白 | 推定 | `src/include/PMMSG.inc:L17` |
| M005 | 登録しました。 | PM01 | 品目登録成功 | 推定（文言からの対応付け） | `src/include/PMMSG.inc:L18` |
| M006 | 更新しました。 | PM01 | 品目更新成功 | 推定（同上） | `src/include/PMMSG.inc:L19` |
| M007 | 削除しました。 | PM01 | 品目削除（論理削除）成功 | 推定（同上） | `src/include/PMMSG.inc:L20` |
| M008 | 数値項目に数字以外が入力されています。 | PM02, PM03 | F-IOQTY / F-ORDERQTY に数字以外 | 推定 | `src/include/PMMSG.inc:L21` |
| M009 | 在庫数量が不正です。 | PM02 | 入出庫数量が 0 以下など不正値（2003 年の消耗品対応で追加） | 推定 | `src/include/PMMSG.inc:L22`、`src/include/PMMSG.inc:L6` |
| M010 | 入出庫区分は1(入庫)または2(出庫)を入力してください。 | PM02 | F-IOKBN が '1' / '2' 以外 | 確定（文言から） | `src/include/PMMSG.inc:L23`、`docs/design.md:L28` |
| M011 | 在庫が不足しています。出庫数量を確認してください。 | PM02 | 出庫で出庫後在庫がマイナス、かつ品目区分が '9' 以外（R5） | 確定 | `src/include/PMMSG.inc:L24`、`docs/design.md:L51`、`docs/design.md:L74` |
| M012 | 入出庫を登録しました。 | PM02 | 入庫・出庫の登録成功 | 推定（文言から） | `src/include/PMMSG.inc:L25` |
| M013 | 製造指示を登録しました。 | PM03 | 製造指示登録成功 | 推定（文言から） | `src/include/PMMSG.inc:L26` |
| M014 | 部品構成が登録されていません。 | PM03 | 製品コードに対応する BOM（PARENT_ITEM_CD）が 0 件 | 推定 | `src/include/PMMSG.inc:L27`、`db2/ddl/02_bom.sql:L10` |
| M015 | 在庫不足の部品があります。23行目にPF4で一覧を表示します。 | PM03 | 子部品必要数 > 在庫数の部品が 1 件以上（R6）。不足部品を CA03-NG-LIST に格納 | 確定 | `src/include/PMMSG.inc:L28`、`docs/design.md:L52`、`docs/design.md:L79` |
| M016 | 完成予定日の形式が不正です。YYYY-MM-DDで入力してください。 | PM03 | F-DUEDATE が YYYY-MM-DD 形式でない（存在しない日付も含むと推定） | 確定 | `src/include/PMMSG.inc:L29`、`docs/design.md:L82` |
| M017 | 他の端末で更新されています。再照会してください。 | PM01 | 更新（3）・削除（4）直前に再読込した UPD_TMS が CA01-UPD-TMS と不一致（R3） | 確定 | `src/include/PMMSG.inc:L30`、`docs/design.md:L49`、`README.md:L40-L41` |

- 成功時に照会結果だけを表示する場合（PM01 照会、PM02 照会）のメッセージは定義されていない（§8 D-12）。
- 機能コード不正（1〜4 以外）・PF キー不正に対応するメッセージは定義されていない（§8 D-09）。

## 4. 隠れ業務ルール（7 件）

| # | ルール | 新システムでの仕様 | 出典 |
|---|---|---|---|
| R1 | 品目コード末尾チェックデジット（モジュラス10・ウェイト3-1） | 品目コード 8 桁のうち先頭 7 桁の各桁に左から 3,1,3,1,3,1,3 を掛けて合計し、チェックデジット = (10 − (合計 MOD 10)) MOD 10。8 桁目と一致すれば正常、不一致は M001。PM01 の登録・更新で呼ぶ。旧モジュラス11方式（ウェイト 7〜1、MOD 11）は 2003 年に廃止済みでコメントのみ残っており、**実装しない**。初期データ 15 品目はすべてこの方式で正しい（例: 1000001**4** → 合計 6、(10−6) MOD 10 = 4） | `src/pli/PMUTL01.pli:L53-L82`、`src/pli/PMUTL01.pli:L9-L10`、`src/pli/PMUTL01.pli:L33-L43`、`docs/design.md:L47`、`README.md:L39`、`db2/data/item_mst.csv:L2-L16` |
| R2 | 削除は論理削除 | 機能 4 は ITEM_MST.DEL_FLG を '1' に UPDATE する（DELETE 文は使わない）。削除後は一覧に出ない。DEL_FLG の既定値は '0' | `docs/design.md:L48`、`docs/design.md:L67`、`db2/ddl/01_item_mst.sql:L15`、`db2/ddl/01_item_mst.sql:L7` |
| R3 | UPD_TMS による楽観的排他制御 → M017 | 照会時に ITEM_MST.UPD_TMS を CA01-UPD-TMS（CHAR(26) = DB2 TIMESTAMP の `YYYY-MM-DD-HH.MM.SS.ffffff` マイクロ秒まで）に退避。更新・削除の直前に再読込して比較し、不一致なら M017 で更新しない。一致すれば UPD_TMS を現在時刻で更新する。新システムでは照会 API の応答に updTms をマイクロ秒精度で返し、更新・削除 API のパラメータで受け取って比較する。旧方式の CA01-LOCK-FLG は使わない | `docs/design.md:L49`、`src/include/PMCOMM.inc:L8-L9`、`src/include/PMCOMM.inc:L20-L21`、`db2/ddl/01_item_mst.sql:L16`、`src/include/DCL_ITEM_MST.inc:L15`、`README.md:L40-L41` |
| R4 | PF9（旧・一括出力）はデッドコード | PM01 の PF キー分岐にコメントアウトされた旧ロジックとして残るだけで動作しない。新システムでは PF9 相当の機能（一括出力）を **実装しない** | `docs/design.md:L50` |
| R5 | 品目区分 '9'（消耗品）のみマイナス在庫を許容 | PM02 出庫で「現在庫 − 出庫数量 < 0」のとき、ITEM_KBN='9' なら許容して更新（STOCK_QTY がマイナスになる）、'1'/'2' は M011 で更新しない。在庫ちょうど 0 になる出庫はどの区分でも可 | `docs/design.md:L51`、`docs/design.md:L74-L75`、`db2/ddl/03_stock.sql:L11`、`src/include/DCL_ITEM_MST.inc:L21` |
| R6 | 子部品必要数 = CEIL(指示数量 × 員数 ÷ 歩留まり率) | 歩留まり率 YIELD_RATE は ％ 値（例 95.00）なので、必要数 = CEIL(ORDER_QTY × QTY_PER ÷ (YIELD_RATE ÷ 100))。端数は切り上げ（CEIL）。計算は BigDecimal で行い浮動小数を使わない。必要数 > STOCK_QTY の部品を不足とし、CA03-NG-LIST（部品コード / 必要数 / 在庫数、最大 10 件）に格納して M015。例: 製品 10000014 を 50 台 → 部品 20000035 は 50×2.00÷0.95 = 105.26… → 106、在庫 60 → 不足 | `docs/design.md:L52`、`docs/design.md:L79-L80`、`db2/ddl/02_bom.sql:L12-L13`、`db2/ddl/02_bom.sql:L5`、`src/include/PMCOMM.inc:L29-L33`、`db2/data/bom.csv:L3`、`db2/data/stock.csv:L6` |
| R7 | 製造指示番号 = 'W' + 西暦下 2 桁 + 4 桁連番（SEQ_CTL を UPDATE してから SELECT）、年度またぎ | SEQ_CTL（SEQ_NAME='WORK_ORDER'）を UPDATE して連番を 1 進めてから、同一トランザクション内で SELECT して番号を得る（UPDATE で行ロックを先に取るので同時登録でも重複しない）。番号は 'W' + SEQ_YY + SEQ_NO の 4 桁ゼロ埋め（例 W260003）。システム日付の西暦下 2 桁が SEQ_YY と異なる場合（年またぎ）は SEQ_YY を新しい年に更新し SEQ_NO を 1 から振り直す（推定。§8 D-05）。初期データの次番号は W260003 | `docs/design.md:L53`、`docs/design.md:L81`、`db2/ddl/05_seq_ctl.sql:L9-L11`、`db2/ddl/04_work_order.sql:L10`、`src/include/DCL_SEQ_CTL.inc:L10-L12`、`db2/data/seq_ctl.csv:L2`、`db2/data/work_order.csv:L2-L3` |

## 5. DB テーブルと型の対応

| テーブル | 主な列（型） | 備考 | 出典 |
|---|---|---|---|
| ITEM_MST | ITEM_CD CHAR(8) PK / ITEM_NAME CHAR(20) / ITEM_KBN CHAR(1) / STOCK_UNIT CHAR(4) / DEL_FLG CHAR(1) 既定 '0' / UPD_TMS, CRT_TMS TIMESTAMP | INDEX (ITEM_KBN, ITEM_CD) | `db2/ddl/01_item_mst.sql:L9-L23`、`src/include/DCL_ITEM_MST.inc:L9-L16` |
| BOM | PARENT_ITEM_CD, CHILD_ITEM_CD CHAR(8) 複合 PK / QTY_PER DEC(7,2) / YIELD_RATE DEC(5,2) / UPD_TMS | 1 階層のみ（製品-部品）。両コードとも ITEM_MST への FK | `db2/ddl/02_bom.sql:L2`、`db2/ddl/02_bom.sql:L8-L25`、`src/include/DCL_BOM.inc:L9-L14` |
| STOCK | ITEM_CD CHAR(8) PK / STOCK_QTY DEC(9,0) / UPD_TMS | 消耗品のみマイナス可。ITEM_MST への FK | `db2/ddl/03_stock.sql:L8-L19`、`src/include/DCL_STOCK.inc:L9-L12` |
| WORK_ORDER | WORK_ORDER_NO CHAR(7) PK / ITEM_CD CHAR(8) / ORDER_QTY DEC(9,0) / DUE_DATE DATE / STATUS CHAR(1) 既定 '0'（0:登録済 1:取消済）/ CRT_TMS | ホスト変数は DUE-DATE CHAR(10)（YYYY-MM-DD） | `db2/ddl/04_work_order.sql:L8-L22`、`src/include/DCL_WORK_ORDER.inc:L9-L15` |
| SEQ_CTL | SEQ_NAME CHAR(10) PK / SEQ_YY CHAR(2) / SEQ_NO INTEGER | 採番制御 | `db2/ddl/05_seq_ctl.sql:L7-L14`、`src/include/DCL_SEQ_CTL.inc:L9-L12` |

- 新システムでは DEC(p,s) は BigDecimal、CHAR(n) は末尾空白を除いた String として扱う（CHAR 比較は DB2 では末尾空白を無視する）。
- 初期データは `db2/data/*.csv`（UTF-8）。TIMESTAMP は `YYYY-MM-DD-HH.MM.SS.ffffff` 形式。

## 6. COMMAREA 項目ごとの新システムでの扱い

扱いの区分: **画面状態** = フロントエンドの state で保持 / **API パラメータ** = リクエスト・レスポンスで受け渡す / **廃止** = 新システムでは持たない

| COMMAREA 項目 | 型 | 旧用途 | 新システムでの扱い | 出典 |
|---|---|---|---|---|
| CA-HEADER | 構造 | 共通ヘッダー | 下位項目に従う | `src/include/PMCOMM.inc:L12` |
| CA-SCRN-ID | CHAR(4) | 直前に表示した画面 ID | 画面状態（ルーティング / 現在の画面）。サーバー側では保持しない | `src/include/PMCOMM.inc:L13` |
| CA-FUNC-CD | CHAR(1) | 機能コード 1〜4 | API パラメータ（PM01 は機能ごとに API を分ける: 照会 GET / 登録 POST / 更新 PUT / 削除 DELETE）＋ 画面状態（選択中の機能） | `src/include/PMCOMM.inc:L14` |
| CA-FIRST-TIME | CHAR(1) | 疑似会話の初回起動判定 | **廃止**（画面の初期表示処理で代替） | `src/include/PMCOMM.inc:L15` |
| CA-PM01 | 構造 | PM01 用エリア | 下位項目に従う | `src/include/PMCOMM.inc:L16` |
| CA01-ITEM-CD | CHAR(8) | 選択中の品目コード | 画面状態 ＋ API パラメータ（更新・削除対象のキー） | `src/include/PMCOMM.inc:L17` |
| CA01-PAGE-NO | FIXED BIN(15) | 現在の表示頁 | 画面状態（頁番号の表示用） | `src/include/PMCOMM.inc:L18` |
| CA01-TOP-ITEM-CD | CHAR(8) | 当該頁の先頭品目コード（PF7/PF8 用） | API パラメータ（一覧 API の頁送りキー。ITEM_CD を基準に前頁/次頁 10 件を返すキーセット方式を再現） | `src/include/PMCOMM.inc:L19` |
| CA01-UPD-TMS | CHAR(26) | 照会時に読んだ UPD_TMS（楽観排他） | API パラメータ（照会応答で返し、更新・削除要求で受け取る。マイクロ秒精度） | `src/include/PMCOMM.inc:L20` |
| CA01-LOCK-FLG | CHAR(1) | 旧方式の排他フラグ。現在未使用（デッドエリア） | **廃止** | `src/include/PMCOMM.inc:L21`、`src/include/PMCOMM.inc:L8-L9` |
| CA-PM02 | 構造 | PM02 用エリア | 下位項目に従う | `src/include/PMCOMM.inc:L22` |
| CA02-ITEM-CD | CHAR(8) | 照会中の品目コード | 画面状態 ＋ API パラメータ | `src/include/PMCOMM.inc:L23` |
| CA02-IO-KBN | CHAR(1) | 入出庫区分 '1'/'2' | API パラメータ | `src/include/PMCOMM.inc:L24` |
| CA-PM03 | 構造 | PM03 用エリア | 下位項目に従う | `src/include/PMCOMM.inc:L25` |
| CA03-ITEM-CD | CHAR(8) | 製造する製品コード | API パラメータ | `src/include/PMCOMM.inc:L26` |
| CA03-ORDER-QTY | FIXED DEC(9,0) | 指示数量 | API パラメータ | `src/include/PMCOMM.inc:L27` |
| CA03-DUE-DATE | CHAR(10) | 完成予定日 YYYY-MM-DD | API パラメータ（文字列で受け取り、サーバーで形式検証 → M016） | `src/include/PMCOMM.inc:L28` |
| CA03-NG-CNT | FIXED BIN(15) | 在庫不足部品の件数 | API パラメータ（M015 応答の不足一覧の件数。配列長で表現） | `src/include/PMCOMM.inc:L29` |
| CA03-NG-LIST(10) | 構造配列 | 在庫不足部品一覧（最大 10 件） | API パラメータ（M015 応答に不足一覧を含める）＋ 画面状態（PF4 相当の表示切替） | `src/include/PMCOMM.inc:L30` |
| CA03-NG-ITEM-CD | CHAR(8) | 不足部品コード | API パラメータ（不足一覧の要素） | `src/include/PMCOMM.inc:L31` |
| CA03-NG-NEED-QTY | FIXED DEC(9,0) | 必要数 | API パラメータ（同上） | `src/include/PMCOMM.inc:L32` |
| CA03-NG-STOCK-QTY | FIXED DEC(9,0) | 在庫数 | API パラメータ（同上） | `src/include/PMCOMM.inc:L33` |
| CA-MSG | 構造 | メッセージエリア | 下位項目に従う | `src/include/PMCOMM.inc:L34` |
| CA-MSG-ID | CHAR(4) | PMMSG のメッセージ ID | API パラメータ（全 API の応答に messageId と文言を含める） | `src/include/PMCOMM.inc:L35` |
| CA-MSG-FLD-POS | FIXED BIN(15) | エラー項目のカーソル位置 | API パラメータ（エラー項目名を返す）＋ 画面状態（該当項目にフォーカス） | `src/include/PMCOMM.inc:L36` |
| CA-LENGTH（コメントアウト） | FIXED BIN(31) | 旧 COMMAREA 長定義 | **廃止** | `src/include/PMCOMM.inc:L38-L39` |

## 7. 運用マニュアルとの食い違い（コードを正とする）

| # | 項目 | コード上の仕様（正） | 運用マニュアル（旧記述・採用しない） | 出典 |
|---|---|---|---|---|
| G-1 | 品目名の桁数 | 20 桁（ITEM_NAME CHAR(20)、F-ITEMNM 20） | 24 桁 | `docs/design.md:L57`、`db2/ddl/01_item_mst.sql:L12`、`src/include/DCL_ITEM_MST.inc:L11`、`docs/design.md:L14` |
| G-2 | 一覧の表示件数 | 10 件/頁（F-LIST01〜10） | 20 件/頁 | `docs/design.md:L58`、`docs/design.md:L20`、`docs/design.md:L17` |
| G-3 | チェックデジット方式 | モジュラス10・ウェイト3-1（R1） | 古い記載がある場合あり（旧モジュラス11と推定） | `README.md:L39`、`src/pli/PMUTL01.pli:L9-L10` |

## 8. 要判断事項（新旧共通の不具合候補・仕様不明点）

方針: 旧挙動はクセも含めて再現する。下表は **直さずに一覧化** したもの。各項目の扱い（「新旧直す」/「旧挙動のまま移行」/「移行後に回す」）の判断をお願いします。「暫定案」は判断が出るまで後続チケットが使う前提。

| # | 種別 | 内容 | 暫定案 | 出典 |
|---|---|---|---|---|
| D-01 | 不具合候補（PL/I 言語仕様上の懸念・実機未確認） | `P-CHK-DGT = CHAR(WK-CHK, 1)` は、PL/I の数値→文字変換（FIXED BIN(15) は先頭空白付きで右詰めの文字列になる）の後に 1 桁へ切り詰めるため、先頭の空白 1 文字になる可能性がある。その場合、検証（'V'）は常に 'N' になり登録できない。初期データとシナリオ（チェックデジット正常で登録できる）からは、実運用では意図どおり動いていると考えられる | 意図どおりの算術（R1）で実装する | `src/pli/PMUTL01.pli:L73`、`docs/design.md:L64` |
| D-02 | 仕様不明 | 品目コードに数字以外（英字・空白）が含まれる場合、`WK-DIGITS(I) = SUBSTR(...)` で CONVERSION 条件（異常終了）が起きる可能性がある。事前の数字チェックやメッセージの定義はない | M001 を返す（異常終了は再現しない） | `src/pli/PMUTL01.pli:L61-L63` |
| D-03 | 不具合候補（新システムへの影響なし） | `ABSTIME` が FIXED BIN(31) で宣言されているが、CICS ASKTIME の ABSTIME は 15 桁のパック 10 進数。新システムはシステム日付（Asia/Tokyo）を使うので影響しない | 対応不要（記録のみ） | `src/pli/PMUTL01.pli:L30`、`src/pli/PMUTL01.pli:L89-L90` |
| D-04 | 資料間の食い違い | 製造指示番号の年部分が、design.md と SEQ_CTL では「西暦下2桁」、WORK_ORDER の DDL コメントでは「年度下2桁」。4 月始まりの年度だと 1〜3 月の採番が変わる | 西暦（暦年）下 2 桁を採用 | `docs/design.md:L53`、`db2/ddl/05_seq_ctl.sql:L10`、`db2/ddl/04_work_order.sql:L10` |
| D-05 | 仕様不明 | 年またぎで SEQ_NO をリセットする条件と順序は記載なし（シナリオ名のみ）。採番の基準日（システム日付か完成予定日か）も記載なし | システム日付の西暦下 2 桁 ≠ SEQ_YY なら SEQ_YY を更新して SEQ_NO=1、それ以外は SEQ_NO+1 | `docs/design.md:L53`、`docs/design.md:L81` |
| D-06 | 仕様不明 | 連番 4 桁が 9999 を超えたときの扱いは定義されていない（WORK_ORDER_NO は CHAR(7)） | 範囲外としてエラー（メッセージ未定義のため要決定） | `db2/ddl/04_work_order.sql:L10`、`db2/ddl/05_seq_ctl.sql:L11` |
| D-07 | 仕様不明 | 在庫不足の部品が 11 件以上ある場合、CA03-NG-LIST は 10 件までしか持てない。超過分の扱いは記載なし | 部品コード昇順で先頭 10 件のみ表示（旧の器に合わせる） | `src/include/PMCOMM.inc:L29-L33`、`docs/design.md:L40` |
| D-08 | 仕様不明 | YIELD_RATE が 0 の場合はゼロ除算（ZERODIVIDE）。入力チェックは記載なし（初期データは 93.00〜100.00） | 現データでは起きないので記録のみ | `db2/ddl/02_bom.sql:L13`、`db2/data/bom.csv:L2-L10` |
| D-09 | 仕様不明 | 機能コード 1〜4 以外、PF9 などの未定義キー、PF7（1 頁目）/ PF8（最終頁）で端に達したときのメッセージは定義されていない | 新 UI では選択肢・ボタンの活性制御で入力不可にする（メッセージは出さない） | `docs/design.md:L12`、`docs/design.md:L20`、`docs/design.md:L50` |
| D-10 | クセ | 論理削除した品目コードは主キーに残るので、同じコードで再登録すると M003（再利用不可） | 旧挙動のまま再現 | `db2/ddl/01_item_mst.sql:L15`、`db2/ddl/01_item_mst.sql:L18` |
| D-11 | クセ | 照会せずに更新・削除した場合（CA01-UPD-TMS が空）や、照会と別の品目コードを入れた場合の扱いは記載なし | M017 とする（比較不一致として扱う） | `docs/design.md:L49`、`src/include/PMCOMM.inc:L17`、`src/include/PMCOMM.inc:L20` |
| D-12 | 仕様不明 | 照会（PM01 機能 1、PM02 照会）成功時のメッセージは定義されていない | メッセージ欄は空白 | `src/include/PMMSG.inc:L14-L30` |
| D-13 | 仕様不明 | PM03 の指示数量が 0 以下・完成予定日が過去日のときのメッセージは定義されていない（M009 は「在庫数量」の文言） | 指示数量 0 以下は M008、過去日はチェックしない | `src/include/PMMSG.inc:L21-L22`、`src/include/PMMSG.inc:L29` |
| D-14 | 仕様不明 | PM03 で品目区分が製品（'1'）以外のコードを入れた場合のチェックは記載なし | 区分チェックはせず、BOM がなければ M014 | `docs/design.md:L35`、`db2/ddl/02_bom.sql:L10` |
| D-15 | 仕様不明 | M015 のとき製造指示を登録するか（不足でも登録するか）は明記なし。製造指示登録で在庫を引き当て（減算）するかも記載なし | 不足時は登録しない。在庫は減算しない | `docs/design.md:L52`、`docs/design.md:L78-L79` |
| D-16 | 不具合候補 | 消耗品のマイナス在庫は符号が付くため、9 桁の在庫が負になると F-STOCKQTY（9 桁）に収まらない場合がある | 新 UI は桁制限なしで表示 | `docs/design.md:L27`、`db2/ddl/03_stock.sql:L11` |
| D-17 | 不具合候補（実機の文字コード依存・未確認） | メッセージは CHAR(60)。EBCDIC 混在（全角 2 バイト + シフトコード）で数えると M010・M015・M016 は 60 バイトを超え、実機では末尾が切れて表示されている可能性がある | 文言は PMMSG の全文を表示 | `src/include/PMMSG.inc:L12`、`src/include/PMMSG.inc:L23`、`src/include/PMMSG.inc:L28-L29` |
| D-18 | 文言のクセ | M015 は「23行目にPF4で一覧を表示します」だが、23 行目はメッセージ行で、一覧の表示先は F-NGLIST01〜10 | 文言は変えずにそのまま使う | `src/include/PMMSG.inc:L28`、`docs/design.md:L8`、`docs/design.md:L40` |
| D-19 | 仕様不明 | PM02 で STOCK 行がない品目（ITEM_MST にはある）の扱いは記載なし（初期データは全品目に STOCK 行あり）。STOCK には楽観排他がなく、同時出庫の扱いも記載なし | STOCK 行なしは M002。更新は `STOCK_QTY = STOCK_QTY ± 数量` の 1 文で行う | `db2/ddl/03_stock.sql:L8-L15`、`db2/data/stock.csv:L2-L16` |
| D-20 | 資料の誤記（挙動への影響なし） | design.md の「更新・更新は楽観的排他制御」は「更新・削除」の誤記（README の記載と一致させて解釈）。PMMSG の変更履歴の「M0009〜M0017」は 4 桁表記だが実際の ID は M009〜M017 | 実際の ID（M001〜M017）と「更新・削除」で解釈 | `docs/design.md:L49`、`README.md:L40`、`src/include/PMMSG.inc:L6-L8` |
| D-21 | 仕様不明 | 入力チェックの評価順（例: PM01 登録で M004 → M001 → M003 のどれを先に出すか）は資産から確定できない | 必須（M004）→ 形式（M008 / M010 / M016 / M001）→ 存在・重複（M002 / M003 / M014）→ 業務（M011 / M015 / M017）の順 | `src/include/PMMSG.inc:L14-L30` |

## 9. 後続チケットへの引継ぎ

- ゴールデンシナリオ（17 本）は `docs/design.md:L60-L83` を起点に、本書の §3 / §4 の期待値で定義する。
- §8 の暫定案は、判断が出たら本書を更新してから実装に反映する。
