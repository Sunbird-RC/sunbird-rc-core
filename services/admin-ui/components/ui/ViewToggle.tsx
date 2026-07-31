export function ViewToggle({
  value,
  onChange,
}: {
  value: 'table' | 'cards'
  onChange: (v: 'table' | 'cards') => void
}) {
  return (
    <div className="inline-flex rounded-full border border-gray-200 p-0.5 text-xs font-medium">
      {(['table', 'cards'] as const).map((v) => (
        <button
          key={v}
          type="button"
          onClick={() => onChange(v)}
          className={`rounded-full px-3 py-1.5 capitalize transition ${
            value === v ? 'bg-ink text-ivory' : 'text-gray-500 hover:text-ink'
          }`}
        >
          {v}
        </button>
      ))}
    </div>
  )
}
