export function EmptyState({
  title,
  body,
  action,
  dashed = true,
}: {
  title: string
  body?: string
  action?: React.ReactNode
  dashed?: boolean
}) {
  return (
    <div
      className={`rounded-md bg-white px-6 py-14 text-center ${dashed ? 'border border-dashed border-gray-200' : ''}`}
    >
      <div className="font-serif text-lg font-medium text-ink">{title}</div>
      {body && <div className="mt-1.5 text-sm text-gray-500">{body}</div>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}

// Distinguishes "flag off" (feature genuinely unavailable — 404 from a
// @ConditionalOnProperty-gated Java controller) from a plain empty result.
export function FlagDisabledState({ title, body }: { title: string; body: string }) {
  return (
    <div className="rounded-md border border-dashed border-warning bg-warning-bg px-7 py-14 text-center">
      <div className="font-serif text-lg font-medium text-warning-text">{title}</div>
      <div className="mx-auto mt-1.5 max-w-md text-sm text-warning-text/85">{body}</div>
    </div>
  )
}
