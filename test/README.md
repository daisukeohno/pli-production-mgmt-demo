# PM01 シナリオテスト

`test/scenarios/{画面ID}/*.json` が完了条件のシナリオ定義です。各ファイルは REST API に対する HTTP ステップと期待値を持ち、
`backend` の JUnit ランナー（`jp.co.sanseki.pm.scenario.ScenarioRunner`）が実 HTTP で実行します。

| ファイル | シナリオ |
|---|---|
| `pm01/01_item_inquiry.json` | 品目照会（正常系） |
| `pm01/02_item_register_ok.json` | 品目登録（チェックデジット正常） |
| `pm01/03_item_register_bad_check_digit.json` | 品目登録（チェックデジット不正 → M001） |
| `pm01/04_item_update_optimistic_lock.json` | 品目更新（他端末更新で UPD_TMS 不一致 → M017） |
| `pm01/05_item_logical_delete.json` | 品目削除（論理削除後、一覧に出ないこと） |
| `pm01/06_paging_pf7_pf8.json` | 前頁/次頁ページング（10件/頁、11件以上で双方向移動） |

各シナリオは先頭で `POST /api/test-support/reset` を呼び、DB を `db2/data/*.csv` の初期状態に戻してから実行します（シナリオ間の順序依存なし）。

## 前提

- Java 17、Maven 3.9 以上
- Docker（PostgreSQL 16 コンテナ / Testcontainers 用）
- Node.js 20 以上（画面確認時のみ）

## A. ワンコマンド実行（Testcontainers）

DB 起動・初期データ投入・アプリ起動・シナリオ実行をすべて `mvn test` 内で行います。

```bash
cd backend
mvn test                       # 単体テスト + PM01 全シナリオ
mvn test -Dtest=Pm01ScenarioTest   # シナリオのみ
```

結果は `backend/target/scenario-report/pm01.md` にも出力されます。

## B. 起動中のアプリに対して実行

### 1. DB 起動

```bash
cd backend
docker compose up -d           # PostgreSQL 16（pmdb / pm / pm、localhost:5432）
```

### 2. 初期データ投入・アプリ起動

アプリ起動時に Flyway が `db2/ddl` 相当のテーブル作成（`V1__legacy_ddl.sql`）と
`db2/data/*.csv` の投入（`V2__Seed_legacy_csv`）を行います。シナリオ実行時はテスト用 API を有効にする `scenario` プロファイルで起動します。

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=scenario     # http://localhost:8080
```

接続先は環境変数 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` で変更できます。

### 3. シナリオ実行

```bash
cd backend
mvn test -Dtest=LiveScenarioTest -Dscenario.baseUrl=http://localhost:8080
```

`-Dscenario.screen=pm01` で対象画面ディレクトリを指定できます（既定 `pm01`）。

### 4. 画面確認（任意）

```bash
cd frontend
npm install
npm run dev                    # http://localhost:5173（/api は localhost:8080 へプロキシ）
```

ENTER=実行、F7=前頁（PF7）、F8=次頁（PF8）、Esc=クリア。

## シナリオ JSON の書式

```jsonc
{
  "id": "PM01-04",
  "title": "...",
  "steps": [
    {
      "name": "照会（ENTER）",
      "request": { "method": "GET", "path": "/api/pm01/items/20000011" },
      "expect": {
        "status": 200,
        "body": { "msgId": null, "updTms": "~re:^\\d{4}-..." },
        "assert": [ { "path": "rows[*].itemCd", "notContains": "20000011" } ]
      },
      "capture": { "tmsA": "updTms" }           // 後続ステップで ${tmsA} として参照
    }
  ]
}
```

- `expect.body` は部分一致（記載したキーのみ比較）。`~re:` 接頭辞は正規表現一致
- `expect.assert` は `equals` / `notEquals` / `contains` / `notContains` / `size` を指定可能
