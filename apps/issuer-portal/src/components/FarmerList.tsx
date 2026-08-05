import { useEffect, useState } from 'react'
import { listFarmers } from '../api'
import { PAGE_SIZE } from '../config'
import type { Farmer, Issuer } from '../types'
import { displayHost, IssuerLogo, safeHttpUrl } from './IssuerLogo'
import { Badge, Empty, Loading, Spinner } from './ui'

/**
 * Searchable holder list, scoped to one issuer when given.
 *
 * Search is debounced and resets paging — without the reset, typing while on page
 * 3 shows an empty table for a query that has plenty of matches. Scoping is
 * applied SERVER-side: this component passes the issuer id and the BFF filters, so
 * the browser never receives another issuer's holders.
 */
export function FarmerList({
  issuer,
  issuers,
  onOpen,
  onNew,
  onCount,
  onLeaveIssuer,
  reloadKey,
}: {
  /** The issuer being worked in. Absent means every holder, across all issuers. */
  issuer?: Issuer
  /** All issuers, for the issuer column on the unscoped list. */
  issuers?: Issuer[]
  onOpen: (farmerId: string) => void
  onNew: () => void
  onCount: (n: number) => void
  /** Back to the gallery, so the issuer context can be changed. */
  onLeaveIssuer?: () => void
  /** Bumped by the parent after a mutation, to force a refetch. */
  reloadKey: number
}) {
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [offset, setOffset] = useState(0)
  const [rows, setRows] = useState<Farmer[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  const noun = issuer?.holderLabel ?? 'Holder'
  const plural = `${noun}s`
  const canIssue = !issuer || Boolean(issuer.credentialConfigId)

  useEffect(() => {
    const t = setTimeout(() => {
      setDebounced(search)
      setOffset(0)
    }, 250)
    return () => clearTimeout(t)
  }, [search])

  // Switching issuer resets paging too: page 3 of one issuer is rarely a valid
  // page of another.
  useEffect(() => {
    setOffset(0)
  }, [issuer?.issuerId])

  useEffect(() => {
    let live = true
    setLoading(true)
    setError(undefined)
    listFarmers(debounced, offset, PAGE_SIZE, issuer?.issuerId)
      .then((r) => {
        if (!live) return
        setRows(r.farmers)
        setTotal(r.total)
        onCount(r.total)
      })
      .catch((e: Error) => live && setError(e.message))
      .finally(() => live && setLoading(false))
    return () => {
      live = false
    }
  }, [debounced, offset, reloadKey, onCount, issuer?.issuerId])

  const from = total === 0 ? 0 : offset + 1
  const to = Math.min(offset + PAGE_SIZE, total)
  const issuerName = (id?: string) =>
    issuers?.find((i) => i.issuerId === id)?.name ?? (id || undefined)

  return (
    <>
      {issuer ? (
        <>
          {onLeaveIssuer && (
            <button className="crumb" onClick={onLeaveIssuer}>
              ← All issuers
            </button>
          )}
          {/* The authority's own header: logo at a readable size, what it issues,
              and the DID that will sign. The top bar keeps the same identity while
              scrolling; this is the page's title. */}
          <div className="ws-head" style={{ ['--accent' as string]: issuer.accent || '#a95236' }}>
            <IssuerLogo issuer={issuer} className="ws-logo" />
            <div className="ws-text">
              <h1 className="ws-name">{issuer.name}</h1>
              <div className="ws-meta">
                <span>
                  {total} {total === 1 ? noun.toLowerCase() : plural.toLowerCase()}
                </span>
                {issuer.credentialName && <span className="chip">{issuer.credentialName}</span>}
                {safeHttpUrl(issuer.url) && (
                  <a
                    className="chip"
                    href={safeHttpUrl(issuer.url)}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    ↗ {displayHost(issuer.url) ?? 'Website'}
                  </a>
                )}
                {issuer.did && (
                  <span className="chip ws-did" title={issuer.did}>
                    {issuer.did}
                  </span>
                )}
              </div>
            </div>
            <button className="btn" onClick={onNew}>
              + New {noun.toLowerCase()}
            </button>
          </div>
        </>
      ) : (
        <div className="page-head">
          <div className="grow">
            <h1 className="page-title">All holders</h1>
            <p className="page-sub">
              Every holder across every issuer. Use this to find a record whose issuer is unset or
              was deleted.
            </p>
          </div>
        </div>
      )}

      {issuer && !canIssue && (
        <div className="alert warn" role="alert">
          <span className="ico" aria-hidden>
            ⚠
          </span>
          <div>
            <strong>{issuer.name}</strong> has no credential type bound, so nothing can be issued
            from these records yet. Bind one from the Issuers page.
          </div>
        </div>
      )}

      <div className="card">
        <div className="card-head">
          <div className="search">
            <span className="mag" aria-hidden>
              ⌕
            </span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by ID, name or district"
              aria-label={`Search ${plural.toLowerCase()}`}
            />
          </div>
          {loading && <Spinner />}
        </div>

        {error ? (
          <div className="card-body">
            <div className="alert err" role="alert">
              <span className="ico" aria-hidden>
                ⚠
              </span>
              <div>
                Couldn't load {plural.toLowerCase()}. <span className="dim">{error}</span>
              </div>
            </div>
          </div>
        ) : loading && rows.length === 0 ? (
          <Loading label={`Loading ${plural.toLowerCase()}…`} />
        ) : rows.length === 0 ? (
          <Empty
            icon="👤"
            title={
              debounced ? `No matching ${plural.toLowerCase()}` : `No ${plural.toLowerCase()} yet`
            }
            body={
              debounced
                ? 'Nothing matched that search. Try an ID, a name, or a district.'
                : issuer
                  ? `Create the first ${noun.toLowerCase()} record, then issue a credential from it.`
                  : 'No holder records exist in the registry yet.'
            }
            action={
              debounced ? (
                <button className="btn-ghost" onClick={() => setSearch('')}>
                  Clear search
                </button>
              ) : issuer ? (
                <button className="btn" onClick={onNew}>
                  + New {noun.toLowerCase()}
                </button>
              ) : undefined
            }
          />
        ) : (
          <>
            <div className="tablewrap">
              <table>
                <thead>
                  <tr>
                    <th>{noun} ID</th>
                    <th>Name</th>
                    {!issuer && <th>Issuer</th>}
                    <th>District</th>
                    <th>Wallet login</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((f) => (
                    <tr
                      key={f.farmerId}
                      className="clickable"
                      onClick={() => onOpen(f.farmerId)}
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          onOpen(f.farmerId)
                        }
                      }}
                    >
                      <td className="mono">{f.farmerId}</td>
                      <td style={{ fontWeight: 600 }}>{f.name}</td>
                      {!issuer && (
                        <td className={f.issuerId ? '' : 'dim'}>
                          {f.issuerId ? (
                            issuerName(f.issuerId)
                          ) : (
                            <Badge kind="warn">Unassigned</Badge>
                          )}
                        </td>
                      )}
                      <td className={f.district ? '' : 'dim'}>{f.district || '—'}</td>
                      <td>
                        {f.keycloakUsername ? (
                          <Badge kind="ok" dot>
                            {f.keycloakUsername}
                          </Badge>
                        ) : (
                          <Badge kind="neutral">Not linked</Badge>
                        )}
                      </td>
                      <td className="actions">
                        <span className="btn-link">Open →</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pager">
              <span>
                Showing {from}–{to} of {total}
              </span>
              <div className="pager-btns">
                <button
                  className="btn-ghost sm"
                  disabled={offset === 0}
                  onClick={() => setOffset(Math.max(0, offset - PAGE_SIZE))}
                >
                  ← Previous
                </button>
                <button
                  className="btn-ghost sm"
                  disabled={to >= total}
                  onClick={() => setOffset(offset + PAGE_SIZE)}
                >
                  Next →
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </>
  )
}
