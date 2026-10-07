#!/usr/bin/env python3
"""spec-catalog.md の機械チェック（標準ライブラリのみ）。

1. 出典 `path:Lx` / `path:Lx-Ly` のファイルが存在し、行が範囲内かつ空行だけでないこと
2. メッセージ M001〜M017 の文言が PMMSG.inc と一字一句一致すること（CHAR(60) の末尾空白は除く）
3. PMCOMM.inc の COMMAREA 全項目が §6 に行として載っていること
4. design.md の BMS 項目が画面ごとに §2 に載っていること
5. 隠れ業務ルール R1〜R7 が §4 にあること
6. §2〜§8 の表の各データ行に出典があること
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "docs/migration/spec-catalog.md"
CITE_RE = re.compile(r"`([\w./-]+):L(\d+)(?:-L(\d+))?`")

errors = []


def section(text, start, end):
    s = text.index(start)
    e = text.index(end, s) if end else len(text)
    return text[s:e]


def table_rows(text):
    for line in text.splitlines():
        if line.startswith("|") and not re.match(r"^\|\s*-", line):
            yield line


def first_cell(row):
    return row.split("|")[1].strip()


catalog = CATALOG.read_text(encoding="utf-8")

# 1. citations
cites = CITE_RE.findall(catalog)
for path, a, b in cites:
    f = ROOT / path
    if not f.is_file():
        errors.append(f"出典ファイルなし: {path}")
        continue
    lines = f.read_text(encoding="utf-8").splitlines()
    lo, hi = int(a), int(b or a)
    if not (1 <= lo <= hi <= len(lines)):
        errors.append(f"出典行が範囲外: {path}:L{lo}-L{hi}（{len(lines)} 行）")
    elif not any(l.strip() for l in lines[lo - 1:hi]):
        errors.append(f"出典行が空行のみ: {path}:L{lo}-L{hi}")

# 2. messages
msg_src = {}
for line in (ROOT / "src/include/PMMSG.inc").read_text(encoding="utf-8").splitlines():
    m = re.search(r"INIT\('(M\d{3})','(.*)'\)", line)
    if m:
        msg_src[m.group(1)] = m.group(2).rstrip()
if len(msg_src) != 17:
    errors.append(f"PMMSG.inc のメッセージ数が 17 ではない: {len(msg_src)}")
msg_sec = section(catalog, "## 3. メッセージ一覧", "## 4.")
msg_cat = {}
for row in table_rows(msg_sec):
    cells = [c.strip() for c in row.split("|")[1:-1]]
    if re.fullmatch(r"M\d{3}", cells[0]):
        msg_cat[cells[0]] = cells
for mid, text in msg_src.items():
    if mid not in msg_cat:
        errors.append(f"メッセージ未掲載: {mid}")
    elif msg_cat[mid][1] != text:
        errors.append(f"文言不一致: {mid}\n  PMMSG : {text}\n  catalog: {msg_cat[mid][1]}")
    elif not msg_cat[mid][3]:
        errors.append(f"表示条件が空: {mid}")

# 3. COMMAREA
pmcomm = (ROOT / "src/include/PMCOMM.inc").read_text(encoding="utf-8")
ca_fields = re.findall(r"^\s*(?:/\*\s*DCL\s+|\d\s+)(CA[\w-]*?)(?:\(\d+\))?[\s,;]", pmcomm, re.M)
ca_sec = section(catalog, "## 6. COMMAREA", "## 7.")
ca_rows = {re.sub(r"\(.*?\)|（.*?）", "", first_cell(r)).strip() for r in table_rows(ca_sec)}
for name in ca_fields:
    if name not in ca_rows:
        errors.append(f"COMMAREA 項目未掲載: {name}")
for dead in ("CA01-LOCK-FLG", "CA-FIRST-TIME", "CA-LENGTH"):
    row = next((r for r in table_rows(ca_sec) if first_cell(r).startswith(dead)), "")
    if "廃止" not in row:
        errors.append(f"{dead} が廃止と明記されていない")

# 4. BMS fields per screen
design = (ROOT / "docs/design.md").read_text(encoding="utf-8")
screens = [("### PM01", "### PM02", "### 2.1 PM01", "### 2.2"),
           ("### PM02", "### PM03", "### 2.2 PM02", "### 2.3"),
           ("### PM03", "## 2.", "### 2.3 PM03", "### 2.4")]
for ds, de, cs, ce in screens:
    d_fields = [first_cell(r) for r in table_rows(section(design, ds, de)) if first_cell(r).startswith("F-")]
    c_fields = {first_cell(r) for r in table_rows(section(catalog, cs, ce))}
    for f in d_fields:
        if f not in c_fields:
            errors.append(f"{ds[4:]} BMS 項目未掲載: {f}")

# 5. hidden rules
rule_sec = section(catalog, "## 4. 隠れ業務ルール", "## 5.")
rule_ids = [first_cell(r) for r in table_rows(rule_sec) if re.fullmatch(r"R\d", first_cell(r))]
for i in range(1, 8):
    if f"R{i}" not in rule_ids:
        errors.append(f"隠れ業務ルール未掲載: R{i}")

# 6. every data row in §2〜§8 tables has a citation (except summary tables)
body = section(catalog, "## 2. 画面別", "## 9.")
exempt_headers = ("### 2.4",)
current = ""
for line in body.splitlines():
    if line.startswith("#"):
        current = line
    if current.startswith(exempt_headers):
        continue
    if line.startswith("|") and not re.match(r"^\|\s*-", line):
        header = line.startswith("| 項目名") or line.startswith("| 機能") or line.startswith("| キー") \
            or line.startswith("| ID") or line.startswith("| #") or line.startswith("| テーブル") \
            or line.startswith("| COMMAREA")
        if not header and not CITE_RE.search(line):
            errors.append(f"出典なしの行: {line[:60]}")

print(f"出典 {len(cites)} 件 / メッセージ {len(msg_cat)} 件 / COMMAREA {len(ca_fields)} 項目 / ルール {len(rule_ids)} 件")
if errors:
    print("FAIL")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("PASS")
