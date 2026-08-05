import { useEffect, useState } from 'react'
import { getOfferStatus, qrSrc } from '../api'
import { OFFER_TTL_SECONDS, POLL_INTERVAL_MS } from '../config'
import type { Offer } from '../types'
import { Alert, Badge, Countdown, Empty, Spinner } from './ui'

/**
 * The QR + PIN + deep-link handover, shared by the staff issue flow and the
 * citizen's own "add to wallet".
 *
 * Extracted so the two cannot drift: the expiry countdown, the pickup polling and
 * the rule that the PIN is read aloud rather than embedded in the QR are all
 * security-relevant presentation, and having them in one place means a fix
 * applies to both callers.
 */
export function WalletHandoff({
  offer,
  onCreate,
  onReset,
  creating,
  disabled,
  idleTitle,
  idleBody,
  createLabel,
  /**
   * Wording for the PIN, when the offer carries one. Optional: an
   * authorization_code offer has no transaction code, and the PIN block is not
   * rendered at all in that case.
   */
  pinHint,
}: {
  offer?: Offer
  onCreate: () => void
  onReset: () => void
  creating: boolean
  disabled: boolean
  idleTitle: string
  idleBody: string
  createLabel: string
  pinHint?: string
}) {
  const [left, setLeft] = useState(OFFER_TTL_SECONDS)
  const [collected, setCollected] = useState(false)

  useEffect(() => {
    if (!offer) return
    setCollected(false)
    setLeft(OFFER_TTL_SECONDS)
    const t = setInterval(() => setLeft((n) => (n <= 0 ? 0 : n - 1)), 1000)
    return () => clearInterval(t)
  }, [offer])

  useEffect(() => {
    if (!offer || collected || left <= 0) return
    const t = setInterval(() => {
      getOfferStatus(offer.offerId)
        .then((s) => {
          if (s.status === 'collected') setCollected(true)
        })
        .catch(() => {
          /* transient; the countdown is the real deadline */
        })
    }, POLL_INTERVAL_MS)
    return () => clearInterval(t)
  }, [offer, collected, left])

  if (!offer) {
    return (
      <div className="qr-side">
        <Empty icon="⬚" title={idleTitle} body={idleBody} />
        <button className="btn block" disabled={disabled || creating} onClick={onCreate}>
          {creating ? (
            <>
              <Spinner /> Creating…
            </>
          ) : (
            createLabel
          )}
        </button>
      </div>
    )
  }

  if (collected) {
    return (
      <div className="qr-side">
        <Badge kind="ok" dot>
          Collected
        </Badge>
        <Alert kind="ok">
          The wallet has collected this credential. The offer and its PIN are now spent.
        </Alert>
        <button className="btn-ghost block" onClick={onReset}>
          Start again
        </button>
      </div>
    )
  }

  if (left <= 0) {
    return (
      <div className="qr-side">
        <Badge kind="warn">Expired</Badge>
        <Alert kind="warn">
          This expired before it was scanned. Offers are single-use and short-lived on purpose —
          generate a fresh one.
        </Alert>
        <button className="btn block" onClick={onCreate}>
          {createLabel}
        </button>
      </div>
    )
  }

  return (
    <div className="qr-side">
      <div className="qr-frame">
        <img src={qrSrc(offer.qrData)} alt="Credential offer QR code" />
      </div>

      {offer.txCode && (
        <>
          <span className="pin-label">One-time PIN</span>
          <div className="pin" aria-label={`PIN ${offer.txCode.split('').join(' ')}`}>
            {offer.txCode.split('').map((d, i) => (
              <span key={i}>{d}</span>
            ))}
          </div>
          <p className="card-note" style={{ margin: 0 }}>
            {pinHint}
          </p>
        </>
      )}

      <Countdown total={OFFER_TTL_SECONDS} left={left} />

      <a className="btn-ghost sm" href={offer.qrData} style={{ textDecoration: 'none' }}>
        Open in a wallet on this device
      </a>
      <button className="btn-link" onClick={onReset}>
        Cancel
      </button>
    </div>
  )
}
