/** TTL ring mirroring oid4vc-service's VP_TXN_TTL, so an unscanned QR says so. */
export function Countdown({ left, total }: { left: number; total: number }) {
  const r = 15
  const circumference = 2 * Math.PI * r
  const progress = Math.max(0, Math.min(1, left / total))
  const low = left <= 60
  const mm = Math.floor(left / 60)
  const ss = String(left % 60).padStart(2, '0')

  return (
    <div className={`ttl${low ? ' low' : ''}`}>
      <svg width="38" height="38" viewBox="0 0 38 38" aria-hidden>
        <circle className="ring-bg" cx="19" cy="19" r={r} fill="none" strokeWidth="3" />
        <circle
          className="ring-fg"
          cx="19"
          cy="19"
          r={r}
          fill="none"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - progress)}
        />
      </svg>
      <div className="ttl-text">
        {mm}:{ss}
        <small>until this request expires</small>
      </div>
    </div>
  )
}
