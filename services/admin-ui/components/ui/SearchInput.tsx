'use client'

export function SearchInput({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (v: string) => void
  placeholder?: string
}) {
  return (
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="h-10 w-full rounded-sm border border-gray-200 bg-white px-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-wave focus:outline-none"
    />
  )
}
