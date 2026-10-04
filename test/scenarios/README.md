# test/scenarios

`docs/design.md` §4「Step 2〜4 シナリオ一覧」をテスト定義にした YAML シナリオです。
API を実 HTTP で叩く統合テストとして `backend` の `ScenarioTest` が全ファイルを実行します。

```
cd backend && mvn test            # 全シナリオ（pending はスキップ）
mvn test -Dtest=ScenarioTest      # シナリオのみ
```

| 画面 | 状態 |
|---|---|
| PM01 品目マスタ保守 | `status: active`（今回のスコープ。全件合格必須） |
| PM02 在庫照会・入出庫登録 | `status: active`（`/api/pm02` で実装済み。全件合格必須） |
| PM03 製造指示登録 | `status: pending`（定義のみ。画面実装時に active へ変更） |

## 形式

```yaml
id: PM01-04
title: ...
status: active | pending
steps:
  - name: ステップ名
    terminal: A            # 端末（＝HTTPセッション＝COMMAREA）。省略時 A
    request:
      method: POST         # 省略時 POST
      path: /api/pm01
      body: { aid: ENTER, func: '1', itemCd: '20000073' }
    expect:
      status: 200
      body:                # JSONPath によるチェック
        - { path: $.msgId, equals: M017 }
        - { path: $.list, size: 10 }
        - { path: '$.list[*].itemCd', notContains: '20000028' }
        - { path: $.sysDate, matches: '\d{4}/\d{2}/\d{2}' }
        - { path: $.fieldPos, isNull: true }
  - name: DB 検証
    db:
      sql: "SELECT DEL_FLG FROM SANSEKI.ITEM_MST WHERE ITEM_CD = '20000028'"
      expect:
        - { path: '$[0].DEL_FLG', equals: '1' }
```

各シナリオの開始前に DB は `db2/ddl` + `db2/data` の初期状態へ戻ります。

PM03 の API パス・項目名（`/api/pm03` など）は PM01 と同じ規約で置いた仮のものです（PM02 は実装で確定済み）。
各画面を実装するときに確定させてください。
