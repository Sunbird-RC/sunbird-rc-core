import { useCallback, useEffect, useState } from 'react'
import { createMyOffer, getMe } from '../api'
import { ENTITIES, holderFields, issuerEntities } from '../fields'
import type {
  Crop,
  EntityKind,
  Farmer,
  Issuer,
  LandParcel,
  Offer,
  Qualification,
  SeedDistribution,
} from '../types'
import { displayHost, IssuerLogo, safeHttpUrl } from './IssuerLogo'
import { Alert, Badge, Loading } from './ui'
import { WalletHandoff } from './WalletHandoff'

type Me = {
  farmer: Farmer
  land: LandParcel[]
  crops: Crop[]
  seeds: SeedDistribution[]
  qualifications: Qualification[]
  issuedCount: number
  /** The authority holding this record, projected for a citizen's eyes. */
  issuer?: Issuer
}

/**
 * What a citizen sees: their own record, read-only, and a way to put their
 * credential in a wallet.
 *
 * Read-only is a deliberate design choice, not a missing feature. If a citizen
 * could edit the data, a credential asserting that data would be asserting
 * something its own subject controls — which defeats the point of an authority
 * issuing it. Corrections go through the issuing authority, and the copy says so.
 *
 * There is no farmer id anywhere in the requests this screen makes: the server
 * resolves the record from the session's token claim.
 */
export function MyDetails({ onError }: { onError: (message: string) => void }) {
  const [me, setMe] = useState<Me>()
  const [loadError, setLoadError] = useState<string>()
  const [parcelRef, setParcelRef] = useState<string>()
  const [offer, setOffer] = useState<Offer>()
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    let live = true
    getMe()
      .then((d) => {
        if (!live) return
        setMe(d)
        setParcelRef(d.land[0]?.landRecordRef)
      })
      .catch((e: Error) => live && setLoadError(e.message))
    return () => {
      live = false
    }
  }, [])

  // Deliberately NO credential type here. Which credential this holder can be
  // issued is decided by their own issuer, on the server — a type chosen in the
  // browser would let a holder of one authority ask for another authority's
  // credential, and with several issuers on one deployment that is a real
  // request, not a hypothetical one.
  const create = useCallback(async () => {
    setCreating(true)
    try {
      setOffer(await createMyOffer(parcelRef))
    } catch (e) {
      onError((e as Error).message)
    } finally {
      setCreating(false)
    }
  }, [parcelRef, onError])

  if (loadError) {
    return (
      <>
        <div className="page-head">
          <div className="grow">
            <h1 className="page-title">My details</h1>
          </div>
        </div>
        <Alert kind="warn">{loadError}</Alert>
      </>
    )
  }
  if (!me) return <Loading label="Loading your record…" />

  const { farmer, land, crops, seeds, qualifications, issuer } = me

  const rowsFor = (kind: EntityKind): Record<string, unknown>[] =>
    (kind === 'LandParcel'
      ? land
      : kind === 'Crop'
        ? crops
        : kind === 'SeedDistribution'
          ? seeds
          : qualifications) as unknown as Record<string, unknown>[]

  // Only the record types this holder's issuer actually uses. A citizen of the
  // age issuer has no land, and an empty "Land parcels" card would read as data
  // that had gone missing.
  const kinds: EntityKind[] = issuer
    ? issuerEntities(issuer.recordEntities)
    : (Object.keys(ENTITIES) as EntityKind[]).filter((k) => rowsFor(k).length > 0)

  const section = (kind: EntityKind, rows: Record<string, unknown>[]) => {
    const meta = ENTITIES[kind]
    const cols = meta.fields.filter((f) => f.inTable)
    return (
      <div className="card" key={kind}>
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">
              <span aria-hidden style={{ marginRight: 7 }}>
                {meta.icon}
              </span>
              {meta.plural}
            </h2>
            <p className="card-note">
              {rows.length} record{rows.length === 1 ? '' : 's'}
            </p>
          </div>
        </div>
        {rows.length === 0 ? (
          <div className="card-body">
            <p className="hint" style={{ margin: 0 }}>
              Nothing recorded yet.
            </p>
          </div>
        ) : (
          <div className="tablewrap">
            <table>
              <thead>
                <tr>
                  {cols.map((c) => (
                    <th key={c.name} className={c.numeric ? 'num' : undefined}>
                      {c.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={String(r.osid ?? i)}>
                    {cols.map((c) => {
                      const v = r[c.name]
                      const blank = v === undefined || v === null || v === ''
                      return (
                        <td
                          key={c.name}
                          className={[c.numeric ? 'num' : '', blank ? 'dim' : ''].join(' ').trim()}
                        >
                          {blank ? '—' : String(v)}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    )
  }

  return (
    <>
      <div className="page-head">
        <div className="grow">
          <h1 className="page-title">My details</h1>
          <p className="page-sub">
            Your record as held by {issuer?.name ?? 'the issuing authority'}. This is what your
            credential will say.
          </p>
        </div>
        <Badge kind="brand">{farmer.farmerId}</Badge>
      </div>

      <div className="issue-grid">
        <div>
          <div className="card">
            <div className="card-head">
              <div className="grow">
                <h2 className="card-title">Profile</h2>
                <p className="card-note">Read-only</p>
              </div>
            </div>
            <div className="tablewrap">
              <table>
                <tbody>
                  {holderFields(issuer?.holderLabel).map((f) => {
                    const v = (farmer as unknown as Record<string, unknown>)[f.name]
                    const blank = v === undefined || v === null || v === ''
                    return (
                      <tr key={f.name}>
                        <td style={{ fontWeight: 600, width: '38%' }}>{f.label}</td>
                        <td className={blank ? 'dim' : f.name === 'farmerId' ? 'mono' : ''}>
                          {blank ? '—' : String(v)}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {kinds.map((kind) => section(kind, rowsFor(kind)))}

          <Alert kind="info">
            Something wrong here? These records are maintained by {issuer?.name ?? 'the issuing authority'} — contact them to have it corrected. Changing it yourself would defeat the purpose of a credential
            they vouch for.
          </Alert>
        </div>

        <div className="card">
          <div className="card-head">
            <div className="grow">
              <h2 className="card-title">Add to a wallet</h2>
              <p className="card-note">{issuer?.credentialName ?? 'Your credential'}</p>
              {issuer && (
                <div className="issued-by">
                  <IssuerLogo issuer={issuer} size={26} />
                  <span>
                    Issued by <strong>{issuer.name}</strong>
                    {safeHttpUrl(issuer.url) && (
                      <>
                        {' · '}
                        <a
                          href={safeHttpUrl(issuer.url)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {displayHost(issuer.url) ?? 'Website'}
                        </a>
                      </>
                    )}
                  </span>
                </div>
              )}
            </div>
          </div>
          <div className="card-body">
            {land.length > 1 && (
              <div className="field" style={{ marginBottom: 14 }}>
                <label>Which land parcel?</label>
                <select value={parcelRef ?? ''} onChange={(e) => setParcelRef(e.target.value)}>
                  {land.map((p) => (
                    <option key={p.landRecordRef} value={p.landRecordRef}>
                      {p.landRecordRef} · {p.landAreaAcres} acres
                    </option>
                  ))}
                </select>
                <span className="help">Your credential describes one parcel.</span>
              </div>
            )}

            {!issuer?.credentialName ? (
              <Alert kind="warn">
                {issuer
                  ? `${issuer.name} has not published a credential type yet, so there is nothing to add to a wallet.`
                  : 'Your record is not assigned to an issuing authority yet, so there is nothing to add to a wallet.'}
              </Alert>
            ) : (
              <WalletHandoff
                offer={offer}
                onCreate={create}
                onReset={() => setOffer(undefined)}
                creating={creating}
                disabled={false}
                idleTitle="Not requested yet"
                idleBody="Generate a QR code, scan it with your wallet, and sign in with the same username and password you used here."
                createLabel="Add my credential to a wallet"
                /* No pinHint: this path issues an authorization_code offer, so
                   there is no transaction code and the PIN block never renders. */
              />
            )}
          </div>
        </div>
      </div>
    </>
  )
}
