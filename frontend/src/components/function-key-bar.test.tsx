import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { FunctionKeyBar } from './function-key-bar'

describe('FunctionKeyBar', () => {
  it('ボタンとファンクションキーの両方で実行でき、ブラウザ既定動作を抑止する', async () => {
    const onF7 = vi.fn()
    const onF8 = vi.fn()
    render(
      <FunctionKeyBar
        actions={[
          { key: 'F7', label: '前頁', onPress: onF7, disabled: true },
          { key: 'F8', label: '次頁', onPress: onF8 },
        ]}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: /次頁/ }))
    expect(onF8).toHaveBeenCalledTimes(1)

    const ev = new KeyboardEvent('keydown', { key: 'F8', cancelable: true })
    window.dispatchEvent(ev)
    expect(onF8).toHaveBeenCalledTimes(2)
    expect(ev.defaultPrevented).toBe(true)

    const disabledEv = new KeyboardEvent('keydown', { key: 'F7', cancelable: true })
    window.dispatchEvent(disabledEv)
    expect(onF7).not.toHaveBeenCalled()
    expect(disabledEv.defaultPrevented).toBe(true)

    const unbound = new KeyboardEvent('keydown', { key: 'F5', cancelable: true })
    window.dispatchEvent(unbound)
    expect(unbound.defaultPrevented).toBe(false)

    fireEvent.keyDown(window, { key: 'F8', ctrlKey: true })
    expect(onF8).toHaveBeenCalledTimes(2)
  })
})
