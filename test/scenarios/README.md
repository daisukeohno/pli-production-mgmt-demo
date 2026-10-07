# ゴールデンシナリオ（旧 PL/I/CICS の期待値）

PL/I/CICS は実行できないため、旧システムの「正解」を `docs/migration/spec-catalog.md`（とその出典）から手計算して固定したもの。
新システム（Spring Boot + モダン UI）の API 応答・画面値・DB 状態をこの期待値と突き合わせる。

| 区分 | 本数 | 内容 |
|---|---|---|
| golden | 17 | `docs/design.md` §4 のシナリオ（PM01×6・PM02×5・PM03×6）。`design_ref` が `PM0x-n` |
| boundary | 10 | 境界ケース（下表） |
| supplemental | 7 | カタログ §8 の決定事項（D-xx）を固定する補足ケース |

境界ケース: チェックデジット 0（PM01-B01）/ 一覧 10 件ちょうど（PM01-B02）・11 件（PM01-B03）/ 在庫ちょうど 0（PM02-B01）/
歩留率端数で在庫と同数（PM03-B01）・割り切れる場合（PM03-B02）/ 暦年 1〜3 月（PM03-B03）/ 連番 9999 超過（PM03-B04）・9999 で年替わり（PM03-B05）/ 不足部品 11 件（PM03-B06）。
年またぎ（PM03-04）と歩留率端数（PM03-03）は golden 側にもある。

## 形式

```yaml
id: PM03-01                # 画面-連番（golden）/ 画面-Bnn（boundary）/ 画面-Snn（supplemental）
category: golden           # golden | boundary | supplemental
design_ref: PM03-1         # golden のみ。design.md §4 の番号（title は §4 と同じ文言）
screen: PM03
rules: [R6, R7]            # カタログ §4 の隠れ業務ルール
decisions: [D-15]          # カタログ §8 の決定事項
tags: []                   # 境界ケースの種別
clock: '2026-10-07T10:00:00+09:00'   # システム日付（Asia/Tokyo）。step ごとに clock で上書き可（時刻は進む方向のみ）
given:                     # db2/data の初期 CSV からの差分（insert / update）。BOM も可
  STOCK:
    update:
      - {key: {ITEM_CD: '20000035'}, set: {STOCK_QTY: 105}}
steps:
  - name: ...
    terminal: A            # 省略時 A。PM01 の楽観排他で複数端末を表す
    action:
      key: ENTER           # START（初期表示）| ENTER | PF4 | PF7 | PF8 | PF9
      input: {F-ITEMCD: '10000014', F-ORDERQTY: '20', F-DUEDATE: '2026-12-01'}   # BMS 項目名
    expect:
      header: {title: PM03 製造指示登録, date: 2026/10/07}
      fields: {F-ORDERNO: W260003}     # 記載した項目のみ比較
      msg: {id: M013, text: 製造指示を登録しました。}   # 成功時メッセージ未定義（D-12）は null
      # list / page / controls（PM01 一覧）、ng_list（PM03 PF4 の不足一覧）
db:                        # 全手順後の期待状態。(初期 CSV + given) との差分行（全列）、差分なしは unchanged
  ITEM_MST: unchanged
  STOCK: unchanged
  WORK_ORDER:
    rows:
      - {WORK_ORDER_NO: W260003, ITEM_CD: '10000014', ORDER_QTY: 20, DUE_DATE: '2026-12-01', STATUS: '0', CRT_TMS: '@now'}
  SEQ_CTL:
    rows:
      - {SEQ_NAME: WORK_ORDER, SEQ_YY: '26', SEQ_NO: 3}
```

- `@now` は処理時点の現在時刻（値は比較せず、更新されたことだけを見る）。
- チェックデジット・CEIL 切り上げ・採番の計算過程は各ファイルのコメントに記載。
- `messages_ext.yaml`: PMMSG.inc（M001〜M017）にない追加メッセージ。D-06 の M018 は ID・文言とも**暫定（承認待ち）**。

## 検証

```bash
pip install pyyaml
python3 test/scenarios/verify_scenarios.py
```

形式（必須キー・メッセージ ID と文言が PMMSG.inc / messages_ext.yaml と一致・DDL の列名・R/D 番号）、
被覆（design.md §4 の 17 本・R1〜R7・境界ケース）に加えて、初期 CSV + given から手順を追って
R1 チェックデジット / R3 楽観排他 / 一覧頁 / R5 在庫 / R6 必要数と不足一覧 / R7 採番 / DB 差分を再計算し、記載した期待値と突き合わせる。
