export type Phase = 'idle' | 'creating' | 'waiting' | 'verified' | 'failed' | 'expired'

// `idle` is deliberately absent: a badge that says "Idle" tells the user
// nothing, so the pill renders only once there is real state to report.
const MAP: Partial<Record<Phase, { kind: string; text: string }>> = {
  creating: { kind: 'wait', text: 'Creating request' },
  waiting: { kind: 'wait', text: 'Waiting for wallet' },
  verified: { kind: 'ok', text: 'Verified' },
  failed: { kind: 'bad', text: 'Verification failed' },
  expired: { kind: 'bad', text: 'Request expired' },
}

export function StatusPill({ phase }: { phase: Phase }) {
  const state = MAP[phase]
  if (!state) return null
  const { kind, text } = state
  return (
    <span className={`pill ${kind}`} role="status" aria-live="polite">
      <i />
      {text}
    </span>
  )
}
