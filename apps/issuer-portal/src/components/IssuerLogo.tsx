import { useState } from 'react'
import type { Issuer } from '../types'

/**
 * Restricts a stored URL to something safe to put in an `href`.
 *
 * Issuer records are staff-editable and end up rendered as links, so this is a
 * real filter rather than tidying: `javascript:alert(1)` in the website field
 * would otherwise execute for the next person who clicks the card. Only http(s)
 * and same-origin paths survive; anything else is treated as absent.
 */
export function safeHttpUrl(raw?: string): string | undefined {
  const value = raw?.trim()
  if (!value) return undefined
  // A root-relative path is same-origin by construction. `//host` is NOT — it is
  // protocol-relative and points off-site, so it is excluded here.
  if (value.startsWith('/') && !value.startsWith('//')) return value
  try {
    const parsed = new URL(value)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed.href : undefined
  } catch {
    return undefined
  }
}

/** The hostname alone, so a card shows `agri.example.gov` rather than a full URL. */
export function displayHost(raw?: string): string | undefined {
  const safe = safeHttpUrl(raw)
  if (!safe || safe.startsWith('/')) return undefined
  try {
    return new URL(safe).host
  } catch {
    return undefined
  }
}

/**
 * An issuer's logo, falling back to its emoji icon.
 *
 * The fallback is on the IMAGE ERROR as well as on an absent URL: a logo hosted
 * elsewhere can stop resolving at any time, and an issuer card with a broken-image
 * glyph reads as a broken portal rather than a missing asset.
 */
export function IssuerLogo({
  issuer,
  size,
  /**
   * The tile shape to render into — `.tile-logo` in the gallery, `.ws-logo`
   * everywhere else. Each supplies its own size, so `size` is only needed to
   * override one.
   */
  className = 'ws-logo',
}: {
  issuer: Pick<Issuer, 'logoUrl' | 'icon' | 'name' | 'accent'>
  size?: number
  className?: string
}) {
  const [failed, setFailed] = useState(false)
  const src = safeHttpUrl(issuer.logoUrl)
  const showsImage = Boolean(src) && !failed

  return (
    <span
      // `has-img` drops the tinted plate: it exists to give the emoji fallback a
      // shape, and behind a real logo it reads as a badge inside a box.
      className={`${className}${showsImage ? ' has-img' : ''}`}
      style={size ? { width: size, height: size, fontSize: Math.round(size * 0.46) } : undefined}
      aria-hidden={!src || failed}
    >
      {showsImage ? (
        <img
          src={src}
          alt={`${issuer.name} logo`}
          onError={() => setFailed(true)}
          loading="lazy"
        />
      ) : (
        (issuer.icon || '◈')
      )}
    </span>
  )
}
