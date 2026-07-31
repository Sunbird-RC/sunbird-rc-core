import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { getGuidedState } from '@/lib/server/guided/steps'
import { getFlags } from '@/lib/server/flags'

const STATUS_LABEL: Record<string, { label: string; color: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }> = {
  done: { label: 'Done', color: 'success' },
  ready: { label: 'Ready', color: 'info' },
  blocked: { label: 'Blocked', color: 'warning' },
  unknown: { label: 'Unknown', color: 'neutral' },
  info: { label: 'Info', color: 'neutral' },
}

export default async function GettingStartedPage() {
  const flags = await getFlags()
  const steps = await getGuidedState(flags.oid4vcReachable)
  const doneCount = steps.filter((s) => s.status === 'done').length

  return (
    <>
      <Header
        title="Getting started"
        subtitle="Schema, publish, issue, verify, revoke — the real RC dependency chain, scratch to production"
        endpoint={`${doneCount} of ${steps.length} done`}
      />
      <ScreenBody>
        <div className="flex flex-col gap-3">
          {steps.map((step, i) => (
            <div key={step.id} className="flex items-start gap-3.5 rounded-md bg-white p-4 shadow-md">
              <div className="mt-0.5 flex h-6 w-6 flex-none items-center justify-center rounded-full bg-tint-93 text-xs font-medium text-obsidian">
                {i + 1}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <div className="font-medium text-ink">{step.title}</div>
                  <StatusBadge label={STATUS_LABEL[step.status].label} color={STATUS_LABEL[step.status].color} />
                </div>
                <div className="mt-1 text-sm text-gray-500">{step.detail}</div>
                <code className="mt-1.5 inline-block font-mono text-xs text-gray-400">{step.endpointHint}</code>
              </div>
            </div>
          ))}
        </div>
      </ScreenBody>
    </>
  )
}
