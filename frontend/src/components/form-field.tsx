import { cloneElement, isValidElement, type ReactElement } from 'react'
import { Label } from '@/components/ui/label'
import { messageText, type MsgId } from '@/lib/messages'
import { cn } from '@/lib/utils'

interface FormFieldProps {
  id: string
  label: string
  required?: boolean
  hint?: string
  error?: MsgId | null
  className?: string
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>
}

/** ラベル + 入力 + インラインエラー。エラー文言は PMMSG と同一。 */
export function FormField({ id, label, required, hint, error, className, children }: FormFieldProps) {
  const errorId = `${id}-error`
  const hintId = `${id}-hint`
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined
  const control = isValidElement(children)
    ? cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })
    : children

  return (
    <div className={cn('grid gap-1.5', className)}>
      <Label htmlFor={id} className="text-sm">
        {label}
        {required && <span className="text-destructive">*</span>}
      </Label>
      {control}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="text-xs font-medium text-destructive">
          {messageText(error)}
        </p>
      )}
    </div>
  )
}
