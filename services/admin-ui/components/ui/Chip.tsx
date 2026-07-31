export function Chip({
  label,
  count,
  active,
  onClick,
}: {
  label: string
  count?: number
  active?: boolean
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition ${
        active ? 'border-brick bg-brick/10 text-brick' : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
      }`}
    >
      <span>{label}</span>
      {count !== undefined && <span className="text-xs opacity-70">{count}</span>}
    </button>
  )
}
