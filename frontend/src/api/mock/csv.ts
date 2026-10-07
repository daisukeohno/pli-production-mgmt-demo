/** db2/data の初期データ CSV（カンマ区切り・引用符なし）を行オブジェクトに変換する */
export function parseCsv(text: string): Record<string, string>[] {
  const lines = text.replace(/\r\n/g, '\n').split('\n').filter((l) => l.trim() !== '')
  const [header, ...rows] = lines
  const cols = header.split(',')
  return rows.map((line) => {
    const vals = line.split(',')
    return Object.fromEntries(cols.map((c, i) => [c, vals[i] ?? '']))
  })
}
