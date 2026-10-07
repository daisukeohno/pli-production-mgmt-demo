import { useEffect, useState } from 'react'

/** 日付をまたいでもヘッダーの日付が更新されるよう 1 分ごとに再評価する */
export function useToday(): Date {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 60_000)
    return () => window.clearInterval(id)
  }, [])
  return now
}
