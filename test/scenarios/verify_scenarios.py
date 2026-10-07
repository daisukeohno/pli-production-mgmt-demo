#!/usr/bin/env python3
"""test/scenarios/**/*.yaml の機械チェック（要 PyYAML）。

形式
  1. 必須キー（トップレベル・steps・action・expect・db）と値の型
  2. メッセージ ID が PMMSG.inc の M001〜M017（または messages_ext.yaml の追加定義）に存在し、文言が一字一句一致
  3. given / db の表名・列名が db2/ddl の定義に存在する
  4. rules が spec-catalog.md §4 の R1〜R7、decisions が §8 の D-01〜D-21 に存在する
被覆
  5. design.md §4 の 17 シナリオ（PM01×6・PM02×5・PM03×6）が golden に 1 本ずつあり、題名が一致
  6. R1〜R7 がそれぞれ 1 本以上のシナリオで覆われている
  7. 境界ケースタグ（check-digit-zero / yield-fraction / year-rollover / list-10 / list-11）がある
期待値の再計算（db2/data の初期 CSV + given から手順を追って再計算し、記載値と突き合わせる）
  8. R1 チェックデジット（M001 / M005 / M006 の前提）
  9. R3 楽観排他（端末ごとの照会済み UPD_TMS と M006 / M007 / M017 の整合）
 10. PM01 一覧（10 件/頁、DEL_FLG='0'、ITEM_CD 昇順、PF7/PF8 活性）
 11. R5 入出庫後の在庫・M011 の前提
 12. R6 子部品必要数（CEIL、十進演算）と不足一覧（先頭 10 件）
 13. R7 採番（西暦下 2 桁・年替わりで 1 から・9999 超過は M018）
 14. db の期待行 = 手順適用後の状態と（初期 CSV + given）の差分
"""
import csv
import datetime as dt
import re
import sys
from decimal import Decimal, ROUND_CEILING
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
TABLES = ["ITEM_MST", "BOM", "STOCK", "WORK_ORDER", "SEQ_CTL"]
DB_TABLES = ["ITEM_MST", "STOCK", "WORK_ORDER", "SEQ_CTL"]
PK = {"ITEM_MST": ("ITEM_CD",), "BOM": ("PARENT_ITEM_CD", "CHILD_ITEM_CD"), "STOCK": ("ITEM_CD",),
      "WORK_ORDER": ("WORK_ORDER_NO",), "SEQ_CTL": ("SEQ_NAME",)}
TOP_KEYS = {"id": str, "category": str, "title": str, "screen": str, "rules": list, "decisions": list,
            "tags": list, "clock": str, "given": dict, "steps": list, "db": dict}
KEYS = {"PM01": {"START", "ENTER", "PF7", "PF8", "PF9"}, "PM02": {"ENTER"}, "PM03": {"ENTER", "PF4"}}
TITLES = {"PM01": "PM01 品目マスタ保守", "PM02": "PM02 在庫照会・入出庫登録", "PM03": "PM03 製造指示登録"}
EXPECT_KEYS = {"header", "fields", "msg", "list", "page", "controls", "ng_list"}
BOUNDARY_TAGS = ["check-digit-zero", "yield-fraction", "year-rollover", "list-10", "list-11"]
NOW = "@now"
PAGE = 10

errors = []


def err(where, text):
    errors.append(f"{where}: {text}")


# ---------- 参照データ ----------
def load_messages():
    msgs = {}
    for mid, text in re.findall(r"INIT\('(M\d{3})','([^']*)'\)", (ROOT / "src/include/PMMSG.inc").read_text(encoding="utf-8")):
        msgs[mid] = text.rstrip()
    base = set(msgs)
    for m in yaml.safe_load((HERE / "messages_ext.yaml").read_text(encoding="utf-8"))["messages"]:
        if m["id"] in base:
            err("messages_ext.yaml", f"{m['id']} は PMMSG.inc と重複")
        msgs[m["id"]] = m["text"]
    return base, msgs


def load_columns():
    cols = {}
    for f in sorted((ROOT / "db2/ddl").glob("*.sql")):
        text = f.read_text(encoding="utf-8")
        t = re.search(r"CREATE TABLE \w+\.(\w+)", text)
        if t:
            body = text[t.end():text.index(")\n", t.end())]
            cols[t.group(1)] = re.findall(r"^\s+([A-Z_]+)\s+(?:CHAR|DEC|INTEGER|DATE|TIMESTAMP)", body, re.M)
    return cols


def load_initial():
    db = {}
    for t in TABLES:
        with open(ROOT / f"db2/data/{t.lower()}.csv", encoding="utf-8") as f:
            db[t] = {tuple(r[k] for k in PK[t]): dict(r) for r in csv.DictReader(f)}
    return db


catalog = (ROOT / "docs/migration/spec-catalog.md").read_text(encoding="utf-8")
RULES = re.findall(r"^\| (R\d) \|", catalog, re.M)
DECISIONS = set(re.findall(r"^\| (D-\d\d) \|", catalog, re.M))
design = (ROOT / "docs/design.md").read_text(encoding="utf-8")
sec4 = design[design.index("## 4."):]
DESIGN = {}
for scr, body in re.findall(r"### (PM0\d)\n((?:\d+\..*\n)+)", sec4):
    for n, title in re.findall(r"^(\d+)\. (.+)$", body, re.M):
        DESIGN[f"{scr}-{n}"] = title.strip()
BASE_MSGS, MSGS = load_messages()
COLS = load_columns()
INITIAL = load_initial()


# ---------- 計算 ----------
def check_digit(cd7):
    s = sum(int(c) * w for c, w in zip(cd7, [3, 1, 3, 1, 3, 1, 3]))
    return (10 - s % 10) % 10


def valid_item_cd(cd):
    return bool(re.fullmatch(r"\d{8}", cd or "")) and check_digit(cd[:7]) == int(cd[7])


def required_qty(order_qty, qty_per, yield_rate):
    return int((Decimal(order_qty) * Decimal(qty_per) / (Decimal(yield_rate) / 100)).to_integral_value(ROUND_CEILING))


def s(v):
    return "" if v is None else str(v)


def norm(row):
    return {k: s(v) for k, v in row.items()}


# ---------- シナリオ ----------
class Sim:
    def __init__(self, sc, where):
        self.where = where
        self.db = {t: {k: dict(v) for k, v in rows.items()} for t, rows in INITIAL.items()}
        for t, ops in (sc.get("given") or {}).items():
            if t not in TABLES:
                err(where, f"given の表 {t} が不明")
                continue
            for op, items in ops.items():
                for it in items:
                    if op == "insert":
                        self.check_cols(t, it, "given")
                        self.db[t][tuple(s(it[k]) for k in PK[t])] = norm(it)
                    elif op == "update":
                        self.check_cols(t, {**it["key"], **it["set"]}, "given")
                        key = tuple(s(it["key"][k]) for k in PK[t])
                        if key not in self.db[t]:
                            err(where, f"given update の対象 {t}{key} が初期データにない")
                        else:
                            self.db[t][key].update(norm(it["set"]))
                    else:
                        err(where, f"given の操作 {op} は insert / update のみ")
        self.base = {t: {k: dict(v) for k, v in rows.items()} for t, rows in self.db.items()}
        self.held = {}        # 端末 → (ITEM_CD, version)（R3）
        self.version = {}     # ITEM_CD → 更新回数
        self.page = {}        # 端末 → 一覧頁
        self.ng = None        # 直前の不足一覧（R6）

    def check_cols(self, t, row, ctx):
        for c in row:
            if c not in COLS.get(t, []):
                err(self.where, f"{ctx} の {t}.{c} は DDL にない列")

    def item(self, cd, active=True):
        r = self.db["ITEM_MST"].get((cd,))
        return r if r and (not active or r["DEL_FLG"] == "0") else None

    def active_list(self):
        return [[r["ITEM_CD"], r["ITEM_NAME"], r["ITEM_KBN"]]
                for k, r in sorted(self.db["ITEM_MST"].items()) if r["DEL_FLG"] == "0"]


def check_fields(where, exp, actual):
    for k, v in (exp.get("fields") or {}).items():
        if k in actual and s(v) != s(actual[k]):
            err(where, f"{k} の期待値 {v!r} ≠ 再計算値 {actual[k]!r}")


def step_pm01(sim, st, w, term, mid, exp):
    key = st["action"]["key"]
    inp = {k: s(v) for k, v in (st["action"].get("input") or {}).items()}
    if key in ("START", "PF7", "PF8", "PF9"):
        p = {"START": 1, "PF7": sim.page.get(term, 1) - 1, "PF8": sim.page.get(term, 1) + 1,
             "PF9": sim.page.get(term, 1)}[key]
        rows = sim.active_list()
        last = max(1, -(-len(rows) // PAGE))
        if not 1 <= p <= last:
            err(w, f"{key} で頁 {p} は範囲外（全 {last} 頁。D-09 でボタン不可のはず）")
        sim.page[term] = p
        if "page" in exp and exp["page"] != p:
            err(w, f"page 期待 {exp['page']} ≠ 再計算 {p}")
        if "list" in exp and [[s(c) for c in r] for r in exp["list"]] != rows[(p - 1) * PAGE:p * PAGE]:
            err(w, f"一覧が再計算値と不一致: {rows[(p - 1) * PAGE:p * PAGE]}")
        ctl = exp.get("controls") or {}
        for k, v in {"PF7": p > 1, "PF8": p < last, "PF9": False}.items():
            if k in ctl and ctl[k] != v:
                err(w, f"controls.{k} 期待 {ctl[k]} ≠ 再計算 {v}")
        if mid is not None:
            err(w, f"{key} でメッセージ {mid} は出ない（D-09 / D-12）")
        return
    func, cd = inp.get("F-FUNC"), inp.get("F-ITEMCD", "")
    if mid == "M001" and valid_item_cd(cd):
        err(w, f"M001 期待だが {cd} はチェックデジット正常")
    if mid in ("M005", "M006") and not valid_item_cd(cd):
        err(w, f"{mid} 期待だが {cd} はチェックデジット不正")
    if mid == "M004" and all(inp.get(k) for k in ("F-ITEMCD", "F-ITEMNM", "F-KBN", "F-UNIT")[:1 if func in "14" else 4]):
        err(w, "M004 期待だが必須項目が揃っている")
    if mid == "M002" and sim.item(cd):
        err(w, f"M002 期待だが {cd} は有効な品目")
    if mid == "M003" and (cd,) not in sim.db["ITEM_MST"]:
        err(w, f"M003 期待だが {cd} は ITEM_MST にない")
    if mid == "M005":
        if (cd,) in sim.db["ITEM_MST"]:
            err(w, f"M005 期待だが {cd} は既存（M003 のはず）")
        sim.db["ITEM_MST"][(cd,)] = {"ITEM_CD": cd, "ITEM_NAME": inp["F-ITEMNM"], "ITEM_KBN": inp["F-KBN"],
                                     "STOCK_UNIT": inp["F-UNIT"], "DEL_FLG": "0", "UPD_TMS": NOW, "CRT_TMS": NOW}
    if func == "1" and mid is None:
        r = sim.item(cd)
        if not r:
            err(w, f"照会成功だが {cd} は有効な品目でない")
            return
        sim.held[term] = (cd, sim.version.get(cd, 0))
        check_fields(w, exp, {"F-ITEMCD": cd, "F-ITEMNM": r["ITEM_NAME"], "F-KBN": r["ITEM_KBN"], "F-UNIT": r["STOCK_UNIT"]})
    if func in ("3", "4") and mid in ("M006", "M007", "M017"):
        ok = sim.held.get(term) == (cd, sim.version.get(cd, 0)) and sim.item(cd) is not None
        if ok != (mid != "M017"):
            err(w, f"R3: 照会済み UPD_TMS {sim.held.get(term)} と DB 版 {(cd, sim.version.get(cd, 0))} から見て {mid} は不整合")
        if mid == "M006":
            sim.db["ITEM_MST"][(cd,)].update({"ITEM_NAME": inp["F-ITEMNM"], "ITEM_KBN": inp["F-KBN"],
                                              "STOCK_UNIT": inp["F-UNIT"], "UPD_TMS": NOW})
        elif mid == "M007":
            sim.db["ITEM_MST"][(cd,)].update({"DEL_FLG": "1", "UPD_TMS": NOW})
        if mid != "M017":
            sim.version[cd] = sim.version.get(cd, 0) + 1


def step_pm02(sim, st, w, term, mid, exp):
    inp = {k: s(v) for k, v in (st["action"].get("input") or {}).items()}
    cd, kbn, qty = inp.get("F-ITEMCD", ""), inp.get("F-IOKBN", ""), inp.get("F-IOQTY", "")
    item, stock = sim.item(cd), sim.db["STOCK"].get((cd,))
    if mid in (None, "M011", "M012") and not (item and stock):
        err(w, f"{mid} 期待だが {cd} は有効品目または STOCK 行がない（M002 のはず。D-19）")
        return
    if mid == "M002" and item and stock:
        err(w, f"M002 期待だが {cd} は品目・STOCK とも存在")
    if mid == "M008" and re.fullmatch(r"\d+", qty):
        err(w, "M008 期待だが数量は数字")
    if mid == "M010" and kbn in ("1", "2"):
        err(w, "M010 期待だが区分は 1/2")
    if mid in ("M011", "M012") and kbn:
        cur = int(stock["STOCK_QTY"])
        new = cur + int(qty) if kbn == "1" else cur - int(qty)
        short = new < 0 and item["ITEM_KBN"] != "9"
        if short != (mid == "M011"):
            err(w, f"R5: 在庫 {cur} → {new}、区分 {item['ITEM_KBN']} なので {'M011' if short else 'M012'} のはず")
        if mid == "M012":
            stock.update({"STOCK_QTY": str(new), "UPD_TMS": NOW})
    if mid in (None, "M012"):
        check_fields(w, exp, {"F-ITEMNM": item["ITEM_NAME"], "F-STOCKQTY": stock["STOCK_QTY"]})


def step_pm03(sim, st, w, term, mid, exp, clock):
    if st["action"]["key"] == "PF4":
        got = [[s(c) for c in r] for r in exp.get("ng_list") or []]
        if sim.ng is None:
            err(w, "PF4 の前に M015 がない")
        elif got != sim.ng[:10]:
            err(w, f"R6: 不足一覧が再計算値と不一致: {sim.ng[:10]}")
        return
    inp = {k: s(v) for k, v in (st["action"].get("input") or {}).items()}
    cd, qty, due = inp.get("F-ITEMCD", ""), inp.get("F-ORDERQTY", ""), inp.get("F-DUEDATE", "")
    sim.ng = None
    item = sim.item(cd)
    if mid == "M002" and item:
        err(w, f"M002 期待だが {cd} は有効な品目")
    if mid == "M016":
        try:
            if re.fullmatch(r"\d{4}-\d{2}-\d{2}", due):
                dt.date.fromisoformat(due)
                err(w, f"M016 期待だが {due} は正しい日付")
        except ValueError:
            pass
    if mid == "M008" and re.fullmatch(r"\d+", qty) and int(qty) > 0:
        err(w, "M008 期待だが数量は正の数字（D-13）")
    bom = [r for (p, _), r in sorted(sim.db["BOM"].items()) if p == cd]
    if mid == "M014" and bom:
        err(w, f"M014 期待だが {cd} に BOM がある")
    if mid not in ("M013", "M015", "M018"):
        return
    if not item or not bom:
        err(w, f"{mid} 期待だが {cd} は品目または BOM がない")
        return
    ng = []
    for r in bom:
        need = required_qty(qty, r["QTY_PER"], r["YIELD_RATE"])
        have = int(sim.db["STOCK"][(r["CHILD_ITEM_CD"],)]["STOCK_QTY"])
        if need > have:
            ng.append([r["CHILD_ITEM_CD"], str(need), str(have)])
    if bool(ng) != (mid == "M015"):
        err(w, f"R6: 不足部品 {ng} から見て {mid} は不整合")
    if mid == "M015":
        sim.ng = sorted(ng)
        return
    seq = sim.db["SEQ_CTL"][("WORK_ORDER",)]
    yy = f"{clock.year % 100:02d}"
    no = 1 if yy != seq["SEQ_YY"] else int(seq["SEQ_NO"]) + 1
    if (no > 9999) != (mid == "M018"):
        err(w, f"R7: 採番 {yy}/{no} から見て {mid} は不整合（D-06）")
    if mid == "M018":
        return
    order_no = f"W{yy}{no:04d}"
    check_fields(w, exp, {"F-ITEMNM": item["ITEM_NAME"], "F-ORDERNO": order_no})
    seq.update({"SEQ_YY": yy, "SEQ_NO": str(no)})
    sim.db["WORK_ORDER"][(order_no,)] = {"WORK_ORDER_NO": order_no, "ITEM_CD": cd, "ORDER_QTY": qty,
                                         "DUE_DATE": due, "STATUS": "0", "CRT_TMS": NOW}


def check_scenario(path, sc):
    where = str(path.relative_to(HERE))
    for k, t in TOP_KEYS.items():
        if not isinstance(sc.get(k), t):
            err(where, f"必須キー {k}（{t.__name__}）がない")
            return
    scr = sc["screen"]
    if scr not in KEYS:
        err(where, f"screen {scr} が不明")
        return
    if not sc["id"].startswith(scr) or path.parent.name != scr.lower():
        err(where, "id / 配置ディレクトリが screen と一致しない")
    if sc["category"] == "golden":
        ref = sc.get("design_ref")
        if ref not in DESIGN:
            err(where, f"design_ref {ref} が design.md §4 にない")
        elif DESIGN[ref] != sc["title"]:
            err(where, f"title が design.md §4 と不一致: {DESIGN[ref]}")
    elif sc["category"] not in ("boundary", "supplemental"):
        err(where, f"category {sc['category']} は golden / boundary / supplemental のみ")
    for r in sc["rules"]:
        if r not in RULES:
            err(where, f"rule {r} がカタログ §4 にない")
    for d in sc["decisions"]:
        if d not in DECISIONS:
            err(where, f"decision {d} がカタログ §8 にない")
    clock0 = dt.datetime.fromisoformat(sc["clock"])
    sim = Sim(sc, where)
    if not sc["steps"]:
        err(where, "steps が空")
    for i, st in enumerate(sc["steps"], 1):
        w = f"{where} step{i}"
        if not isinstance(st, dict) or not st.get("name") or not isinstance(st.get("action"), dict) \
                or not isinstance(st.get("expect"), dict) or not st["expect"]:
            err(w, "name / action / expect（空でない）が必要")
            continue
        key, exp = st["action"].get("key"), st["expect"]
        if key not in KEYS[scr]:
            err(w, f"key {key} は {scr} で使えない（{sorted(KEYS[scr])}）")
            continue
        if key == "ENTER" and not st["action"].get("input"):
            err(w, "ENTER には input が必要")
        if set(exp) - EXPECT_KEYS:
            err(w, f"expect の不明キー {sorted(set(exp) - EXPECT_KEYS)}")
        if key in ("ENTER", "START") and "msg" not in exp:
            err(w, "expect.msg が必要（成功時メッセージなしは null）")
        msg = exp.get("msg")
        mid = None
        if msg is not None:
            if not isinstance(msg, dict) or set(msg) != {"id", "text"}:
                err(w, "msg は null または {id, text}")
                continue
            mid = msg["id"]
            if mid not in MSGS:
                err(w, f"メッセージ ID {mid} が PMMSG.inc / messages_ext.yaml にない")
            elif msg["text"] != MSGS[mid]:
                err(w, f"{mid} の文言が不一致: {MSGS[mid]}")
        clock = dt.datetime.fromisoformat(st.get("clock", sc["clock"]))
        if clock < clock0:
            err(w, "step の clock が巻き戻っている")
        clock0 = clock
        hdr = exp.get("header")
        if hdr and (hdr.get("title") != TITLES[scr] or hdr.get("date") != clock.strftime("%Y/%m/%d")):
            err(w, f"header は {TITLES[scr]} / {clock:%Y/%m/%d} のはず")
        term = st.get("terminal", "A")
        if scr == "PM01":
            step_pm01(sim, st, w, term, mid, exp)
        elif scr == "PM02":
            step_pm02(sim, st, w, term, mid, exp)
        else:
            step_pm03(sim, st, w, term, mid, exp, clock)
    for t in DB_TABLES:
        exp = sc["db"].get(t)
        diff = {k: r for k, r in sim.db[t].items() if sim.base[t].get(k) != r}
        if exp == "unchanged":
            if diff:
                err(where, f"db.{t} は unchanged だが手順適用後に差分あり: {list(diff.values())}")
            continue
        if not isinstance(exp, dict) or not isinstance(exp.get("rows"), list) or not exp["rows"]:
            err(where, f"db.{t} は unchanged または rows（1 行以上）")
            continue
        got = {}
        for r in exp["rows"]:
            if set(r) != set(COLS[t]):
                err(where, f"db.{t} の行は全列 {COLS[t]} を持つ: {r}")
            got[tuple(s(r.get(k)) for k in PK[t])] = norm(r)
        if got != diff:
            err(where, f"db.{t} の期待行 ≠ 手順適用後の差分 {list(diff.values())}")


def main():
    files = sorted(p for p in HERE.glob("pm0*/*.yaml"))
    scenarios = []
    for p in files:
        try:
            sc = yaml.safe_load(p.read_text(encoding="utf-8"))
        except yaml.YAMLError as e:
            err(str(p.relative_to(HERE)), f"YAML 構文エラー: {e}")
            continue
        check_scenario(p, sc)
        scenarios.append(sc)
    ids = [sc.get("id") for sc in scenarios]
    for d in sorted({i for i in ids if ids.count(i) > 1}):
        err("全体", f"id 重複 {d}")
    golden = [sc.get("design_ref") for sc in scenarios if sc.get("category") == "golden"]
    for ref in DESIGN:
        if golden.count(ref) != 1:
            err("全体", f"design.md §4 {ref} の golden シナリオが {golden.count(ref)} 本（1 本のはず）")
    counts = {scr: sum(1 for r in DESIGN if r.startswith(scr)) for scr in KEYS}
    if counts != {"PM01": 6, "PM02": 5, "PM03": 6}:
        err("design.md", f"§4 のシナリオ数が想定と違う: {counts}")
    cover = {r: [sc["id"] for sc in scenarios if r in sc.get("rules", [])] for r in RULES}
    if RULES != [f"R{i}" for i in range(1, 8)]:
        err("spec-catalog.md", f"§4 のルールが R1〜R7 でない: {RULES}")
    tags = {t: [sc["id"] for sc in scenarios if t in sc.get("tags", [])] for t in BOUNDARY_TAGS}
    used = sorted({st["expect"]["msg"]["id"] for sc in scenarios for st in sc.get("steps", [])
                   if isinstance(st.get("expect"), dict) and isinstance(st["expect"].get("msg"), dict)})
    for r, who in cover.items():
        if not who:
            err("全体", f"ルール {r} を覆うシナリオがない")
    for t, who in tags.items():
        if not who:
            err("全体", f"境界ケース {t} のシナリオがない")

    by_cat = {c: sum(1 for sc in scenarios if sc.get("category") == c) for c in ("golden", "boundary", "supplemental")}
    print(f"シナリオ {len(scenarios)} 本（golden {by_cat['golden']} / boundary {by_cat['boundary']} / "
          f"supplemental {by_cat['supplemental']}）、手順 {sum(len(sc.get('steps', [])) for sc in scenarios)}")
    print(f"design.md §4: {len(DESIGN)} 本 {counts}")
    print("ルール被覆:")
    for r, who in cover.items():
        print(f"  {r}: {len(who)} 本 {', '.join(who)}")
    print("境界ケース:")
    for t, who in tags.items():
        print(f"  {t}: {', '.join(who)}")
    print(f"使用メッセージ: {', '.join(used)}（PMMSG.inc 外: {', '.join(m for m in used if m not in BASE_MSGS) or 'なし'}）")
    print(f"PMMSG.inc の未使用: {', '.join(sorted(BASE_MSGS - set(used))) or 'なし'}")
    if errors:
        print(f"\nFAIL: {len(errors)} 件")
        for e in errors:
            print("  - " + e)
        return 1
    print("\nPASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
