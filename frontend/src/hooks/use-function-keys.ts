import { useEffect, useRef } from 'react'

export type FunctionKey = 'F1' | 'F2' | 'F3' | 'F4' | 'F5' | 'F6' | 'F7' | 'F8' | 'F9' | 'F10' | 'F11' | 'F12'

export interface FunctionKeyBinding {
  key: FunctionKey
  onPress: () => void
  disabled?: boolean
}

/**
 * 旧 PF キー（PF1〜PF12）を物理ファンクションキーに割り当てる。
 * 登録済みのキーはブラウザ既定動作（F5 再読込・F7 キャレットブラウズ等）を抑止する。
 */
export function useFunctionKeys(bindings: ReadonlyArray<FunctionKeyBinding>) {
  const ref = useRef(bindings)
  ref.current = bindings

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey || e.isComposing) return
      const binding = ref.current.find((b) => b.key === e.key)
      if (!binding) return
      e.preventDefault()
      if (!binding.disabled) binding.onPress()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])
}
