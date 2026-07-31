import { Button } from './Button'

export function ErrorBanner({
  title,
  body,
  onRetry,
}: {
  title: string
  body?: string
  onRetry?: () => void
}) {
  return (
    <div className="overflow-hidden rounded-md border border-danger shadow-md">
      <div className="flex items-center gap-2.5 border-b border-danger bg-danger-bg px-5 py-3.5">
        <div className="text-sm font-medium text-danger">{title}</div>
        {onRetry && (
          <Button variant="outline" size="sm" onClick={onRetry} className="ml-auto border-danger text-danger hover:bg-danger-bg">
            Retry
          </Button>
        )}
      </div>
      {body && (
        <pre className="whitespace-pre-wrap bg-gray-50 p-4 font-mono text-xs leading-relaxed text-gray-700">
          {body}
        </pre>
      )}
    </div>
  )
}
