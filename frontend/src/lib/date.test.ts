import { formatHeaderDate } from './date'

it('ヘッダー日付は YYYY/MM/DD', () => {
  expect(formatHeaderDate(new Date(2026, 0, 5))).toBe('2026/01/05')
  expect(formatHeaderDate(new Date(2026, 11, 31))).toBe('2026/12/31')
})
