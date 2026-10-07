import { isoDate, numeric, required, validateForm, type FormRules } from './validation'

describe('validation rules', () => {
  it('required → M004', () => {
    expect(required('')).toBe('M004')
    expect(required('  ')).toBe('M004')
    expect(required('1')).toBeNull()
  })

  it('numeric → M008（空欄は対象外）', () => {
    expect(numeric('')).toBeNull()
    expect(numeric('0123')).toBeNull()
    expect(numeric('12a')).toBe('M008')
    expect(numeric('-1')).toBe('M008')
    expect(numeric('１２')).toBe('M008')
  })

  it('isoDate → M016', () => {
    expect(isoDate('2026-11-15')).toBeNull()
    expect(isoDate('2028-02-29')).toBeNull()
    expect(isoDate('2026/11/15')).toBe('M016')
    expect(isoDate('2026-02-30')).toBe('M016')
    expect(isoDate('20261115')).toBe('M016')
  })
})

describe('validateForm', () => {
  type F = 'a' | 'b'
  const rules: FormRules<F> = [
    ['a', [required, numeric]],
    ['b', [required]],
  ]

  it('項目順で最初のエラー 1 件だけを返す', () => {
    expect(validateForm({ a: '', b: '' }, rules)).toEqual({ field: 'a', msgId: 'M004' })
    expect(validateForm({ a: 'x', b: '' }, rules)).toEqual({ field: 'a', msgId: 'M008' })
    expect(validateForm({ a: '1', b: '' }, rules)).toEqual({ field: 'b', msgId: 'M004' })
    expect(validateForm({ a: '1', b: 'y' }, rules)).toBeNull()
  })
})
