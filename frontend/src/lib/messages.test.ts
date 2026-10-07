import pmmsgSource from '../../../src/include/PMMSG.inc?raw'
import { MESSAGES } from './messages'

function parsePmmsg(src: string): Record<string, string> {
  const re = /INIT\('(M\d{3})','([^']*)'\)/g
  const out: Record<string, string> = {}
  for (const m of src.matchAll(re)) out[m[1]] = m[2].trimEnd()
  return out
}

describe('MESSAGES', () => {
  it('PMMSG.inc の全メッセージと一字一句同じ', () => {
    const legacy = parsePmmsg(pmmsgSource)
    expect(Object.keys(legacy)).toHaveLength(17)
    expect(MESSAGES).toEqual(legacy)
  })
})
