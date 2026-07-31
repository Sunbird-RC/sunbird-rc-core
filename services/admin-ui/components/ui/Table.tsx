export type Column<T> = {
  key: string
  label: string
  render: (row: T) => React.ReactNode
  widthClass?: string
}

export function Table<T>({
  columns,
  rows,
  onRowClick,
  rowKey,
  title,
}: {
  columns: Column<T>[]
  rows: T[]
  onRowClick?: (row: T) => void
  rowKey: (row: T) => string
  title?: React.ReactNode
}) {
  const gridCols = columns.map((c) => c.widthClass ?? 'minmax(0,1fr)').join(' ')

  return (
    <div className="overflow-x-auto rounded-md bg-white shadow-md">
      {title && <div className="border-b border-gray-100 px-5 py-3.5 text-sm font-medium text-ink">{title}</div>}
      <div
        className="grid min-w-[560px] gap-3.5 border-b border-gray-100 bg-gray-50 px-5 py-2.5 text-xs font-medium uppercase tracking-wide text-gray-500"
        style={{ gridTemplateColumns: gridCols }}
      >
        {columns.map((c) => (
          <div key={c.key}>{c.label}</div>
        ))}
      </div>
      {rows.map((row) => (
        <div
          key={rowKey(row)}
          onClick={() => onRowClick?.(row)}
          className="grid min-w-[560px] items-center gap-3.5 border-b border-gray-50 px-5 py-3.5 transition hover:bg-gray-50 last:border-0"
          style={{ gridTemplateColumns: gridCols, cursor: onRowClick ? 'pointer' : undefined }}
        >
          {columns.map((c) => (
            <div key={c.key} className="min-w-0">
              {c.render(row)}
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
