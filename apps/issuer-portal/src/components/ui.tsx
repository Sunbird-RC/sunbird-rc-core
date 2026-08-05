import { useEffect, type ReactNode } from 'react'

/** Small primitives shared across screens. Kept in one file so the component
 *  directory stays about screens rather than about buttons. */

export function Spinner({ large = false }: { large?: boolean }) {
  return <span className={large ? 'spinner lg' : 'spinner'} aria-hidden />
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading" role="status">
      <Spinner /> {label}
    </div>
  )
}

export function Alert({
  kind,
  icon,
  children,
}: {
  kind: 'err' | 'warn' | 'info' | 'ok'
  icon?: string
  children: ReactNode
}) {
  return (
    <div className={`alert ${kind}`} role={kind === 'err' ? 'alert' : undefined}>
      <span className="ico" aria-hidden>
        {icon ?? { err: '⚠', warn: '⚠', info: 'ℹ', ok: '✓' }[kind]}
      </span>
      <div>{children}</div>
    </div>
  )
}

export function Badge({
  kind = 'neutral',
  dot = false,
  children,
}: {
  kind?: 'ok' | 'bad' | 'warn' | 'neutral' | 'brand'
  dot?: boolean
  children: ReactNode
}) {
  return (
    <span className={`badge ${kind}`}>
      {dot && <span className="dot" aria-hidden />}
      {children}
    </span>
  )
}

export function Field({
  label,
  required = false,
  help,
  error,
  children,
}: {
  label: string
  required?: boolean
  help?: string
  error?: string
  children: ReactNode
}) {
  return (
    <div className="field">
      <label>
        {label}
        {required && (
          <span className="req" title="Required">
            *
          </span>
        )}
      </label>
      {children}
      {error ? <span className="err">{error}</span> : help ? <span className="help">{help}</span> : null}
    </div>
  )
}

export function Empty({
  icon,
  title,
  body,
  action,
}: {
  icon: string
  title: string
  body: string
  action?: ReactNode
}) {
  return (
    <div className="empty">
      <span className="ico" aria-hidden>
        {icon}
      </span>
      <h3>{title}</h3>
      <p>{body}</p>
      {action}
    </div>
  )
}

/** Auto-dismissing status message. `key`ed by the caller so a repeat message re-shows. */
export function Toast({
  kind,
  message,
  onClose,
}: {
  kind: 'ok' | 'err'
  message: string
  onClose: () => void
}) {
  useEffect(() => {
    // Errors stay until dismissed — they usually need reading and often name a
    // field to fix. Successes are transient.
    if (kind === 'err') return
    const t = setTimeout(onClose, 4000)
    return () => clearTimeout(t)
  }, [kind, onClose])

  return (
    <div className={`toast ${kind}`} role="status">
      <span aria-hidden>{kind === 'ok' ? '✓' : '⚠'}</span>
      <span>{message}</span>
      <button onClick={onClose} aria-label="Dismiss">
        ✕
      </button>
    </div>
  )
}

export function Modal({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  // Escape closes, and the body cannot scroll behind the dialog.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prev
    }
  }, [onClose])

  return (
    <div
      className="modal-back"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="modal" role="dialog" aria-modal aria-label={title}>
        <div className="modal-head">
          <h3>{title}</h3>
          <span className="grow" />
          <button className="btn-ghost sm" onClick={onClose}>
            Close
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

/** Countdown ring, mirroring the offer TTL so a stale QR says so. */
export function Countdown({ total, left }: { total: number; left: number }) {
  const r = 9
  const c = 2 * Math.PI * r
  const frac = total > 0 ? Math.max(0, Math.min(1, left / total)) : 0
  const mins = Math.floor(left / 60)
  const secs = left % 60
  return (
    <span className="ttl">
      <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden>
        <circle cx="11" cy="11" r={r} fill="none" stroke="#e2e7e6" strokeWidth="2.5" />
        <circle
          cx="11"
          cy="11"
          r={r}
          fill="none"
          stroke="#a95236"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - frac)}
        />
      </svg>
      {left > 0 ? `Expires in ${mins}:${String(secs).padStart(2, '0')}` : 'Expired'}
    </span>
  )
}
