import type { ReactNode } from 'react'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { cn } from '@/lib/utils'

export interface Column<T> {
  key: string
  header: string
  cell: (row: T) => ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: ReadonlyArray<Column<T>>
  rows: ReadonlyArray<T>
  rowKey: (row: T) => string
  loading?: boolean
  emptyText?: string
  onRowClick?: (row: T) => void
  selectedKey?: string | null
}

export function DataTable<T>({ columns, rows, rowKey, loading, emptyText = 'データがありません', onRowClick, selectedKey }: DataTableProps<T>) {
  return (
    <div className="overflow-hidden rounded-lg border bg-card">
      <Table>
        <TableHeader className="bg-muted/60">
          <TableRow>
            {columns.map((c) => (
              <TableHead key={c.key} className={cn('h-10 text-xs font-semibold text-muted-foreground', c.className)}>
                {c.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody aria-busy={loading || undefined}>
          {rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                {loading ? '読み込み中…' : emptyText}
              </TableCell>
            </TableRow>
          ) : (
            rows.map((row) => {
              const k = rowKey(row)
              return (
                <TableRow
                  key={k}
                  data-state={selectedKey === k ? 'selected' : undefined}
                  className={cn(onRowClick && 'cursor-pointer', loading && 'opacity-60')}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                >
                  {columns.map((c) => (
                    <TableCell key={c.key} className={c.className}>
                      {c.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              )
            })
          )}
        </TableBody>
      </Table>
    </div>
  )
}
