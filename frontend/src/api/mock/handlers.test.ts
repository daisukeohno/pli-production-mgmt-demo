import { fetchItem, fetchItems } from '../items'
import { ApiError } from '../client'

describe('mock API', () => {
  it('品目一覧は品目コード順・10 件/頁', async () => {
    const p1 = await fetchItems(1)
    expect(p1).toMatchObject({ page: 1, pageSize: 10, totalCount: 15, totalPages: 2 })
    expect(p1.items).toHaveLength(10)
    expect(p1.items[0]).toMatchObject({ itemCd: '10000014', itemName: '精密減速機ユニットA型', itemKbn: '1', stockUnit: '台' })
    const codes = p1.items.map((i) => i.itemCd)
    expect([...codes].sort()).toEqual(codes)

    const p2 = await fetchItems(2)
    expect(p2.items).toHaveLength(5)
    expect(p2.items.at(-1)?.itemCd).toBe('90000034')
  })

  it('存在しない品目は M002', async () => {
    await expect(fetchItem('99999999')).rejects.toMatchObject({ status: 404, msgId: 'M002', field: 'itemCd' })
    await expect(fetchItem('99999999')).rejects.toBeInstanceOf(ApiError)
  })
})
