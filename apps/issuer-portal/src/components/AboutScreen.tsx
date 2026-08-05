import { Alert } from './ui'

/**
 * Explains the two issuance paths in the terms staff actually need: who is
 * proven to be who, and by what. Kept in the product rather than in a README
 * because the distinction ("PIN proves the QR reached the right person" vs "the
 * login proves it") is the thing people get wrong.
 */
export function AboutScreen() {
  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1 className="page-title">How this works</h1>
          <p className="page-sub">
            Two ways a farmer's credential reaches their wallet, and what each one proves.
          </p>
        </div>
      </div>

      <Alert kind="info">
        In both paths the credential's contents come from the registry. Nobody types claims into a
        credential — that's what stops a wallet holding data about someone else.
      </Alert>

      <div className="card">
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">Path A · Staff hands over a QR and a PIN</h2>
            <p className="card-note">Use when the farmer is in front of you and has no login yet</p>
          </div>
        </div>
        <div className="card-body">
          <ol style={{ margin: 0, paddingLeft: 20, display: 'grid', gap: 9, fontSize: 14 }}>
            <li>You open the farmer, check their records, and choose a credential type.</li>
            <li>
              The claims are resolved from the registry and shown to you read-only. If a required
              value is missing, issuing is blocked until the record is fixed.
            </li>
            <li>
              You generate an offer. The screen shows a QR code and a <strong>one-time PIN</strong>.
            </li>
            <li>
              The farmer scans the QR and types the PIN into their wallet. The PIN is what proves the
              QR reached the intended person — a QR alone is a bearer token, so anyone who
              photographed it could otherwise collect the credential.
            </li>
            <li>The offer and PIN are single-use, and expire in five minutes.</li>
          </ol>
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">Path B · Farmer adds it themselves</h2>
            <p className="card-note">
              Use once their record is linked to a login — no staff involvement at all
            </p>
          </div>
        </div>
        <div className="card-body">
          <ol style={{ margin: 0, paddingLeft: 20, display: 'grid', gap: 9, fontSize: 14 }}>
            <li>
              You link their farmer record to a Keycloak account, on the farmer's Profile tab. This
              writes their farmer ID onto that account.
            </li>
            <li>
              In their wallet the farmer chooses to add a credential from this issuer, and signs in
              with <strong>the same account they'd use here</strong>.
            </li>
            <li>
              The issuer reads the farmer ID from their signed-in session, looks up that record, and
              issues from it.
            </li>
            <li>
              Because the record is chosen by <em>who signed in</em>, a farmer can only ever receive
              their own credential. There is no PIN, because the login already proved who they are.
            </li>
          </ol>
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">Why some credential types don't appear</h2>
          </div>
        </div>
        <div className="card-body">
          <p style={{ margin: 0, fontSize: 14, color: 'var(--muted)' }}>
            A type is only offered here if it supports the SD-JWT VC format <em>and</em> its issuing
            authority's DID can be resolved by a wallet. Several older schemas were authored by a DID
            that only resolves inside this deployment, or by one pointing at a host that no longer
            publishes a DID document. Issuing those would produce credentials that look fine here and
            fail in every wallet, so they're hidden rather than offered.
          </p>
        </div>
      </div>
    </>
  )
}
