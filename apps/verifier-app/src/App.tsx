import { useCallback, useEffect, useRef, useState } from 'react'
import {
  createOffer,
  createRequest,
  getStatus,
  listCredentialTypes,
  type VerifierStatus,
} from './api'
import { POLL_INTERVAL_MS, TXN_TTL_SECONDS } from './config'
import type { CredentialType } from './types'
import { ClaimPicker } from './components/ClaimPicker'
import { CredentialArt } from './components/CredentialArt'
import { QrPanel } from './components/QrPanel'
import { ResultPanel } from './components/ResultPanel'
import { StatusPill, type Phase } from './components/StatusPill'
import { Stepper } from './components/Stepper'
import { TypePicker } from './components/TypePicker'

const TOTAL_STEPS = 4

export default function App() {
  const [types, setTypes] = useState<CredentialType[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [type, setType] = useState<CredentialType | null>(null)
  const [claims, setClaims] = useState<string[]>([])

  const [step, setStep] = useState(1)
  const [phase, setPhase] = useState<Phase>('idle')
  const [qr, setQr] = useState<{ link: string; txn: string } | null>(null)
  const [left, setLeft] = useState(TXN_TTL_SECONDS)
  const [result, setResult] = useState<VerifierStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [offerQr, setOfferQr] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const pollRef = useRef<number | null>(null)
  const tickRef = useRef<number | null>(null)

  const stopTimers = useCallback(() => {
    if (pollRef.current) window.clearInterval(pollRef.current)
    if (tickRef.current) window.clearInterval(tickRef.current)
    pollRef.current = null
    tickRef.current = null
  }, [])

  useEffect(() => stopTimers, [stopTimers])

  // Types come from live schema configs, so a credential created after this app
  // was built is selectable without a rebuild.
  useEffect(() => {
    listCredentialTypes()
      .then(({ types: found }) => setTypes(found))
      .catch((err) => {
        setTypes([])
        setLoadError(err instanceof Error ? err.message : String(err))
      })
  }, [])

  const chooseType = useCallback((t: CredentialType) => {
    setType(t)
    // Smallest meaningful ask by default — requesting less is the whole point.
    setClaims(t.attributes.slice(0, 1))
    setStep(2)
  }, [])

  const toggleClaim = useCallback((attr: string) => {
    setClaims((prev) => (prev.includes(attr) ? prev.filter((a) => a !== attr) : [...prev, attr]))
  }, [])

  const generate = useCallback(async () => {
    if (!type || claims.length === 0) return
    stopTimers()
    setPhase('creating')
    setResult(null)
    setError(null)
    setQr(null)
    setOfferQr(null)

    try {
      const req = await createRequest(type.vct, claims)
      setQr({ link: req.qr_data, txn: req.transaction_id })
      setLeft(TXN_TTL_SECONDS)
      setPhase('waiting')
      setStep(3)

      tickRef.current = window.setInterval(() => {
        setLeft((prev) => {
          if (prev <= 1) {
            stopTimers()
            setPhase('expired')
            return 0
          }
          return prev - 1
        })
      }, 1000)

      pollRef.current = window.setInterval(async () => {
        try {
          const status = await getStatus(req.transaction_id)
          if (status.status === 'pending') return
          stopTimers()
          setResult(status)
          setPhase(status.verified ? 'verified' : 'failed')
          setStep(4)
        } catch {
          // 404 means the transaction TTL lapsed; the countdown reports that.
        }
      }, POLL_INTERVAL_MS)
    } catch (err) {
      setPhase('failed')
      setError(err instanceof Error ? err.message : String(err))
      setStep(4)
    }
  }, [type, claims, stopTimers])

  const issue = useCallback(async () => {
    if (!type) return
    stopTimers()
    setPhase('idle')
    setQr(null)
    setResult(null)
    setError(null)
    try {
      const offer = await createOffer(type)
      setOfferQr(offer.qr_data)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    }
  }, [type, stopTimers])

  const copyLink = useCallback(async () => {
    if (!qr) return
    try {
      await navigator.clipboard.writeText(qr.link)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1400)
    } catch {
      setCopied(false)
    }
  }, [qr])

  /** Going back abandons any live request — an issued QR is bound to its claims. */
  const goBack = useCallback(
    (to: number) => {
      stopTimers()
      setPhase('idle')
      setResult(null)
      setError(null)
      setQr(null)
      setOfferQr(null)
      setStep(to)
    },
    [stopTimers],
  )

  const busy = phase === 'creating'

  const HEADINGS: Record<number, { title: string; sub: string }> = {
    1: {
      title: 'Which credential?',
      sub: 'Choose what the person should present from their wallet.',
    },
    2: {
      title: 'What to request',
      sub: `Only these attributes are shared — the wallet cannot be made to reveal more.`,
    },
    3: {
      title: 'Scan with a wallet',
      sub: 'This updates by itself as soon as the holder approves on their phone.',
    },
    4: {
      title: 'Verification result',
      sub: 'What the wallet chose to share, and the checks run on it.',
    },
  }

  return (
    <div className="split">
      <aside className="hero">
        <CredentialArt />
        <div className="hero-inner">
          <h1 className="hero-title">
            Verify a credential
            <br />
            in <span className="accent">seconds.</span>
          </h1>
          <p className="hero-copy">
            Ask for exactly the attributes you need. The holder approves on their own phone, and
            nothing more than that is shared — or stored.
          </p>
        </div>
      </aside>

      <main className="panel">
        <div className="panel-inner">
          <div className="panel-top">
            <div className="brand">
              <span className="brand-mark">◈</span>
              <span className="brand-name">Sunbird RC</span>
            </div>
            <StatusPill phase={phase} />
          </div>

          <Stepper
            step={step}
            total={TOTAL_STEPS}
            onBack={step > 1 ? () => goBack(step - 1) : undefined}
          />

          <h3 className="step-title">{HEADINGS[step].title}</h3>
          <p className="step-sub">{HEADINGS[step].sub}</p>

          {offerQr && (
            <div className="offer-note">
              <QrPanel
                link={offerQr}
                caption={`Scan to receive a sample ${type?.name ?? 'credential'}`}
                footnote="issuance offer · valid 10 minutes"
              />
              <button className="btn-quiet" onClick={() => setOfferQr(null)}>
                Done — hide this
              </button>
            </div>
          )}

          {step === 1 &&
            (types === null ? (
              <p className="hint">Loading credential types…</p>
            ) : loadError ? (
              <div className="alert">
                Could not load credential types: {loadError}
                <br />
                <br />
                If this app runs on a different origin than the API, <code>/credential-schema</code>{' '}
                sends no CORS headers and the browser blocks it. Run the dev server without{' '}
                <code>?base=</code> so requests are proxied same-origin.
              </div>
            ) : types.length === 0 ? (
              <p className="hint">
                No verifiable credential types found. A type needs the <code>vc+sd-jwt</code> format
                and an issuer DID this host can resolve.
              </p>
            ) : (
              <TypePicker types={types} selected={type} onSelect={chooseType} />
            ))}

          {step === 2 && type && (
            <>
              <ClaimPicker
                type={type}
                selected={claims}
                onToggle={toggleClaim}
                onSelectAll={() => setClaims(type.attributes)}
                onSelectNone={() => setClaims([])}
              />
              <button className="btn" onClick={generate} disabled={busy || claims.length === 0}>
                {busy ? 'Generating…' : 'Request credentials'}
              </button>
              <button className="btn-quiet" onClick={issue}>
                Wallet empty? Issue a sample {type.name}
              </button>
            </>
          )}

          {step === 3 &&
            (phase === 'expired' ? (
              <div className="expired">
                <p>This code expired before it was scanned. Codes stay valid for five minutes.</p>
                <button className="btn" onClick={generate} disabled={busy}>
                  {busy ? 'Generating…' : 'Generate a new QR code'}
                </button>
              </div>
            ) : qr ? (
              <QrPanel
                link={qr.link}
                caption="Scan with the wallet, then approve the request"
                footnote={qr.txn}
                ttl={{ left, total: TXN_TTL_SECONDS }}
                onCopy={copyLink}
                copied={copied}
                chips={claims}
              />
            ) : (
              <p className="hint">Go back a step and request credentials to get a QR code.</p>
            ))}

          {step === 4 && (
            <ResultPanel
              requested={claims}
              result={result}
              error={error}
              onReset={() => goBack(2)}
            />
          )}

          <p className="panel-foot">
            Nothing is stored by this app. Each request is single-use and expires in five minutes.
          </p>
        </div>
      </main>
    </div>
  )
}
