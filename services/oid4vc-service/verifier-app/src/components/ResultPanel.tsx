import type { VerifierStatus } from '../api'

// Plain-language labels: the raw keys oid4vc-service returns are precise but
// meaningless to anyone who isn't reading the spec, and this list is exactly
// what makes a "verified" badge trustworthy — so it stays visible, not hidden.
const CHECK_LABELS: Record<string, string> = {
  holderSignature: 'Holder signature',
  credentialSignatures: 'Issuer signature',
  holderBinding: 'Holder binding',
  nonce: 'Nonce (freshness)',
  audience: 'Audience bound',
  revocation: 'Not revoked',
  dcql: 'Query satisfied',
}
const CHECK_ORDER = [
  'credentialSignatures',
  'holderSignature',
  'holderBinding',
  'nonce',
  'audience',
  'revocation',
  'dcql',
]

function prettyValue(value: unknown): { text: string; isBool: boolean } {
  if (typeof value === 'boolean') return { text: value ? 'Yes' : 'No', isBool: true }
  return { text: String(value), isBool: false }
}

/**
 * The final step: what the wallet shared, and which checks passed.
 *
 * Only ever rendered once a presentation has resolved, so it handles exactly two
 * cases — a request that could not be created, and a result. The idle, waiting
 * and expired states belong to earlier steps and are rendered there, so guards
 * for them here were unreachable.
 */
export function ResultPanel({
  requested,
  result,
  error,
  onReset,
}: {
  requested: string[]
  result: VerifierStatus | null
  error: string | null
  onReset: () => void
}) {
  if (error) {
    return (
      <>
        <Empty mark="!" text="This request could not be created." />
        <div className="alert">{error}</div>
      </>
    )
  }

  if (!result) return null

  // `claims` is keyed by the DCQL credential query id ('cred').
  const presented = (result.claims?.cred ?? Object.values(result.claims ?? {})[0] ?? {}) as Record<
    string,
    unknown
  >
  const checks = result.checks ?? {}

  const verified = result.verified === true
  const disclosed = requested.filter((k) =>
    Object.prototype.hasOwnProperty.call(presented, k),
  ).length

  return (
    <>
      <div className={`banner ${verified ? 'ok' : 'bad'}`}>
        <span className="banner-icon">{verified ? '✓' : '✕'}</span>
        <span className="banner-text">
          <strong>{verified ? 'Credential verified' : 'Verification failed'}</strong>
          <span>
            {verified
              ? `${disclosed} of ${requested.length} requested attribute${requested.length > 1 ? 's' : ''} disclosed · every check passed`
              : 'The presentation was rejected — see the reason below.'}
          </span>
        </span>
        <button className="btn-link" onClick={onReset}>
          Verify another
        </button>
      </div>

      <div className="claims">
        {requested.map((key, i) => {
          const has = Object.prototype.hasOwnProperty.call(presented, key)
          const { text, isBool } = has ? prettyValue(presented[key]) : { text: 'not disclosed', isBool: false }
          return (
            <div
              key={key}
              className={`claim${has && isBool ? ' bool' : ''}${has ? '' : ' absent'}`}
              style={{ animationDelay: `${i * 60}ms` }}
            >
              <span className="key">{key}</span>
              <span className="val">{text}</span>
            </div>
          )
        })}
      </div>

      {result.error && <div className="alert">{result.error}</div>}

      {Object.keys(checks).length > 0 && (
        <div className="checks">
          {CHECK_ORDER.filter((k) => k in checks).map((k) => {
            const ok = checks[k] === 'OK'
            return (
              <div className="check" key={k}>
                <span className={`tick ${ok ? 'ok' : 'bad'}`}>{ok ? '✓' : '✕'}</span>
                {CHECK_LABELS[k] ?? k}
              </div>
            )
          })}
        </div>
      )}

      {result.holderDid && (
        <div className="holder">
          <b>holder</b> — {result.holderDid}
        </div>
      )}

      <details>
        <summary>Raw verifier response</summary>
        <pre>{JSON.stringify(result, null, 2)}</pre>
      </details>
    </>
  )
}

function Empty({ mark, text }: { mark: string; text: string }) {
  return (
    <div className="empty">
      <div className="empty-mark">{mark}</div>
      <p>{text}</p>
    </div>
  )
}
