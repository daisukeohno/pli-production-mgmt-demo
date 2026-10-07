import { toast } from 'sonner'
import { isSuccessMessage, messageText, type MsgId } from './messages'

/** 旧 23 行目のメッセージ行の代わりにトーストで表示する。 */
export function notifyMessage(id: MsgId) {
  const text = messageText(id)
  const opts = { id: 'screen-message', description: id }
  if (isSuccessMessage(id)) {
    toast.success(text, opts)
  } else {
    toast.error(text, opts)
  }
}
