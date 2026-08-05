import type { ReactNode } from 'react'
import { logoutUrl } from '../api'
import type { Issuer, Session } from '../types'
import { IssuerLogo } from './IssuerLogo'

export type NavKey = 'issuers' | 'holders' | 'about' | 'me'

function initials(name: string) {
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?'
}

/**
 * Persistent shell: top bar with identity, sidebar for navigation, scrolling
 * content. A back-office tool, unlike the verifier console's one-step-at-a-time
 * split view — staff move between holders and issuance repeatedly and shouldn't
 * lose their place.
 *
 * The sidebar is derived from the ROLE and from the issuer currently open. A
 * citizen must have no route to the holder list — offering a link to something the
 * server will refuse is worse than not offering it, and the list is not theirs to
 * see. The server enforces both independently.
 */
export function AppShell({
  session,
  nav,
  onNav,
  /** The issuer being worked in, if any. Adds its own section to the sidebar. */
  issuer,
  holderCount,
  citizen = false,
  children,
}: {
  session: Session
  nav: NavKey
  onNav: (k: NavKey) => void
  issuer?: Issuer
  holderCount?: number
  citizen?: boolean
  children: ReactNode
}) {
  const name = session.fullName || session.username || 'Signed in'
  const isStaff = (session.roles ?? []).includes('issuer-staff')

  const staffNav: { key: NavKey; icon: string; label: string; count?: number }[] = [
    { key: 'issuers', icon: '◈', label: 'Issuers' },
    // Every holder across every issuer. Present so records with no issuer — or
    // one that was deleted — remain reachable and fixable rather than orphaned.
    { key: 'holders', icon: '👥', label: 'All holders' },
    { key: 'about', icon: 'ℹ', label: 'How this works' },
  ]
  const citizenNav: { key: NavKey; icon: string; label: string }[] = [
    { key: 'me', icon: '👤', label: 'My details' },
  ]
  const navItems = isStaff ? staffNav : citizen ? citizenNav : []

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden>
            ◈
          </span>
          <div>
            <div className="brand-name">Sunbird RC</div>
            <div className="brand-sub">Issuer</div>
          </div>
        </div>

        {/* Which authority you are acting for, always visible. Issuing the right
            credential from the wrong issuer is a mistake staff cannot see in the
            claim table, so it belongs in the chrome rather than on one screen. */}
        {isStaff && issuer && (
          <div className="topbar-issuer" style={{ ['--accent' as string]: issuer.accent || '#4a7c59' }}>
            <IssuerLogo issuer={issuer} size={26} />
            <div>
              <div className="ti-name">{issuer.name}</div>
              <div className="ti-sub">
                {issuer.credentialName || 'No credential type bound'}
              </div>
            </div>
          </div>
        )}

        <span className="topbar-spacer" />

        <div className="who">
          <span className="avatar" aria-hidden>
            {initials(name)}
          </span>
          <div>
            <div className="who-name">{name}</div>
            <div className="who-role">
              {isStaff ? 'Issuer staff' : citizen ? `Holder · ${session.farmerId}` : 'No access'}
            </div>
          </div>
        </div>
        <a className="btn-ghost sm" href={logoutUrl()} style={{ textDecoration: 'none' }}>
          Sign out
        </a>
      </header>

      {session.mock && (
        <div className="mockbar">
          <span aria-hidden>⚠</span>
          Mock mode — sample data, no registry or Keycloak. Set PORTAL_MOCK=0 to use real services.
        </div>
      )}

      <div className="body">
        <nav className="sidebar" aria-label="Sections">
          {/* "Registry" is the staff console's subject; a holder is looking at
              their own record, and calling that a registry is jargon. */}
          <span className="nav-label">{isStaff ? 'Registry' : 'Your record'}</span>
          {navItems.map((n) => (
            <button
              key={n.key}
              className={`nav-item ${nav === n.key ? 'active' : ''}`}
              onClick={() => onNav(n.key)}
              aria-current={nav === n.key ? 'page' : undefined}
            >
              <span className="ico" aria-hidden>
                {n.icon}
              </span>
              {n.label}
              {n.key === 'holders' && holderCount !== undefined && (
                <span className="nav-count">{holderCount}</span>
              )}
            </button>
          ))}

          {isStaff && issuer && (
            <>
              <span className="nav-label" style={{ marginTop: 18 }}>
                {issuer.name}
              </span>
              <div className="nav-issuer">
                <span className="mono">{issuer.issuerId}</span>
                {issuer.did && (
                  <span className="nav-did" title={issuer.did}>
                    {issuer.did}
                  </span>
                )}
              </div>
            </>
          )}
        </nav>

        <main className="content">
          <div className="content-inner">{children}</div>
        </main>
      </div>
    </div>
  )
}
