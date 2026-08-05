import { loginUrl } from '../api'
import { IssuerArt } from './IssuerArt'
import { Alert } from './ui'

/**
 * Sign-in. Deliberately the only thing an unauthenticated visitor can see: the
 * portal writes to the registry and mints credentials, so there is no read-only
 * preview to fall back to.
 *
 * The button is a real link, not a fetch. The sign-in redirect has to happen as a
 * top-level navigation so the identity provider can set its own session cookie —
 * an XHR would be blocked and would fail silently.
 *
 * The copy names no product and no protocol. Whoever signs in here has been given
 * an account by their organisation; "you'll be taken to Keycloak" told them the
 * name of a component they have no reason to know, and "requires the issuer-staff
 * role" described a permission model rather than what to do about it.
 */
export function LoginScreen({ error }: { error?: string }) {
  return (
    <div className="login">
      <div className="login-hero">
        <IssuerArt />
        <div className="login-hero-inner">
          <div className="brand">
            <span className="brand-mark" aria-hidden>
              ◈
            </span>
            <div>
              <div className="brand-name">Sunbird RC</div>
              <div className="brand-sub">Issuer</div>
            </div>
          </div>

          <h1 className="login-title">
            Issue credentials from <span className="accent">records you already hold</span>
          </h1>
          <p className="login-copy">
            Every credential's contents come from your registry, never from anything typed in by
            hand — so what a holder receives is exactly what your records say.
          </p>

          <ul className="login-points">
            <li>
              <span className="tick" aria-hidden>
                ✓
              </span>
              Each authority manages its own holders and records
            </li>
            <li>
              <span className="tick" aria-hidden>
                ✓
              </span>
              Credentials signed with your own organisation's key
            </li>
            <li>
              <span className="tick" aria-hidden>
                ✓
              </span>
              Collected by any wallet, from a QR code or a link
            </li>
          </ul>
        </div>
      </div>

      <div className="login-panel">
        <div className="login-card">
          {/* The mark repeats here because the card is the whole screen on narrow
              viewports, where the hero collapses away above it. */}
          <span className="login-card-mark" aria-hidden>
            ◈
          </span>

          <h2>Sign in</h2>
          <p>Use the account your organisation gave you.</p>

          {error && <Alert kind="err">{error}</Alert>}

          <a className="btn block login-btn" href={loginUrl()}>
            Continue to sign in
          </a>

          <p className="login-foot">
            Staff accounts only. If you hold a credential, you don't need to sign in here — collect
            it in your wallet from the QR code or link your issuer gives you.
          </p>
        </div>
      </div>
    </div>
  )
}
