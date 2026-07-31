'use client'

import { useEffect, useRef, useState } from 'react'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { useToast } from '@/components/providers/ToastProvider'
import type { InspectorEntry } from '@/lib/server/inspector'

type EnrichedEntry = InspectorEntry & { publicUrl: string; curl: string }

function statusColor(status: number): 'success' | 'danger' | 'neutral' {
  if (status === 0) return 'danger'
  if (status >= 200 && status < 300) return 'success'
  if (status >= 400) return 'danger'
  return 'neutral'
}

// The design's default right-drawer panel: last HTTP call's method, URL,
// request/response bodies, status. Polls a cursor (?since=seq) only while
// this panel is actually visible, so 12 idle screens don't hammer the
// server. Failures (status 0 = transport error, or 4xx/5xx) are recorded
// same as successes — that's most of this panel's value.
export function InspectorPanel() {
  const [entries, setEntries] = useState<EnrichedEntry[]>([])
  const [expanded, setExpanded] = useState<number | null>(null)
  const [showPublicUrl, setShowPublicUrl] = useState(false)
  const seqRef = useRef(0)
  const toast = useToast()

  useEffect(() => {
    let cancelled = false
    let timer: ReturnType<typeof setTimeout>

    const poll = async () => {
      if (document.visibilityState === 'visible') {
        try {
          const res = await fetch(`/admin/api/inspector?since=${seqRef.current}`)
          const fresh: EnrichedEntry[] = await res.json()
          if (!cancelled && fresh.length > 0) {
            seqRef.current = fresh[fresh.length - 1].seq
            setEntries((prev) => [...fresh, ...prev].slice(0, 30))
            setExpanded((e) => e ?? fresh[fresh.length - 1].seq)
          }
        } catch {
          // Polling failure shouldn't crash the panel — just try again next tick.
        }
      }
      if (!cancelled) timer = setTimeout(poll, 2000)
    }
    poll()
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [])

  if (entries.length === 0) {
    return (
      <div className="px-6 py-14 text-center">
        <div className="font-serif text-sm font-medium text-ink">Request inspector</div>
        <div className="mt-1.5 text-sm text-gray-500">
          Every call this console makes to a backend service shows up here — method, URL, request and response
          bodies, and status.
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2 p-4">
      <label className="flex items-center gap-2 text-xs text-gray-500">
        <input type="checkbox" checked={showPublicUrl} onChange={(e) => setShowPublicUrl(e.target.checked)} />
        Show runnable (localhost) URLs
      </label>
      {entries.map((e) => (
        <div key={e.seq} className="rounded-sm border border-gray-100">
          <button
            type="button"
            onClick={() => setExpanded((cur) => (cur === e.seq ? null : e.seq))}
            className="flex w-full items-center gap-2 px-3 py-2 text-left"
          >
            <span className="font-mono text-xs font-medium text-charcoal">{e.method}</span>
            <StatusBadge label={e.status === 0 ? 'FAILED' : String(e.status)} color={statusColor(e.status)} />
            <span className="min-w-0 flex-1 truncate font-mono text-xs text-gray-500">
              {showPublicUrl ? e.publicUrl : e.url}
            </span>
          </button>
          {expanded === e.seq && (
            <div className="flex flex-col gap-2 border-t border-gray-100 px-3 py-2 text-xs">
              {e.error && <div className="text-danger">{e.error}</div>}
              {e.requestBody && (
                <div>
                  <div className="mb-1 font-medium uppercase tracking-wide text-gray-500">Request</div>
                  <pre className="max-h-32 overflow-auto whitespace-pre-wrap break-all rounded-sm bg-gray-50 p-2 font-mono text-[11px]">
                    {e.requestBody}
                  </pre>
                </div>
              )}
              {e.responseBody && (
                <div>
                  <div className="mb-1 font-medium uppercase tracking-wide text-gray-500">Response</div>
                  <pre className="max-h-32 overflow-auto whitespace-pre-wrap break-all rounded-sm bg-gray-50 p-2 font-mono text-[11px]">
                    {e.responseBody}
                  </pre>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="text-gray-400">{e.ms}ms</span>
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(e.curl)
                    toast('Copied as cURL.', 'success')
                  }}
                  className="rounded-sm border border-gray-200 px-2 py-1 text-xs font-medium text-ink hover:bg-gray-50"
                >
                  Copy as cURL
                </button>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
