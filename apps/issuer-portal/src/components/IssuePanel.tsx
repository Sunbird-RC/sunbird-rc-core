import { useCallback, useEffect, useState } from 'react'
import { createOffer, listCredentialTypes, resolveClaims } from '../api'
import { issuerEntities } from '../fields'
import type { CredentialType, Farmer, Issuer, LandParcel, Offer } from '../types'
import { Alert, Empty, Loading, Spinner } from './ui'
import { WalletHandoff } from './WalletHandoff'

type Resolved = {
  claims: Record<string, unknown>
  sources: Record<string, string>
  missing: string[]
}

/**
 * Pick a credential type, review the claims the registry resolved, hand over a
 * QR and PIN.
 *
 * The claims are read-only by design. The whole point of sourcing them from the
 * registry is that nobody — staff included — can alter what a credential says on
 * its way to a wallet, so there is deliberately no edit affordance here. Staff
 * who spot a wrong value fix the record, and the claims re-resolve.
 */
type Delivery = 'authorization_code' | 'pre-authorized_code'

export function IssuePanel({
  farmer,
  issuer,
  land,
  onError,
}: {
  farmer: Farmer
  /** The issuing authority. Its bound credential type is what gets issued. */
  issuer: Issuer
  land: LandParcel[]
  onError: (message: string) => void
}) {
  // Signing in is the stronger option, but it only works once the holder's
  // record is linked to a Keycloak account — otherwise there is no identity for
  // the wallet to authenticate as. Default to whichever can actually complete.
  const canSignIn = Boolean(farmer.keycloakUsername)
  const [delivery, setDelivery] = useState<Delivery>(
    canSignIn ? 'authorization_code' : 'pre-authorized_code',
  )
  const [types, setTypes] = useState<CredentialType[]>()
  const [hidden, setHidden] = useState(0)
  const [loadError, setLoadError] = useState<string>()

  // The type is the ISSUER's, not a per-holder choice. An authority that could
  // issue any type on the server would make "which issuer am I acting for"
  // meaningless, and the signing DID comes from the schema, so a mismatch here
  // would produce a credential attributed to the wrong authority.
  const configId = issuer.credentialConfigId
  // Which parcel to flatten into the credential. A holder may have several, and a
  // credential describes one — so this is an explicit choice, not the first row.
  const [landRef, setLandRef] = useState<string>(land[0]?.landRecordRef ?? '')
  const usesLand = issuerEntities(issuer.recordEntities).includes('LandParcel')
  /** The issuer's own noun for the person, capitalised for use in a sentence. */
  const holder = issuer.holderLabel || 'Holder'

  const [resolved, setResolved] = useState<Resolved>()
  const [resolving, setResolving] = useState(false)

  const [offer, setOffer] = useState<Offer>()
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    let live = true
    listCredentialTypes()
      .then((r) => {
        if (!live) return
        setTypes(r.types)
        setHidden(r.hidden)
      })
      .catch((e: Error) => live && setLoadError(e.message))
    return () => {
      live = false
    }
  }, [])

  // Re-resolve whenever the type or the chosen parcel changes.
  useEffect(() => {
    if (!configId) {
      setResolved(undefined)
      return
    }
    let live = true
    setResolving(true)
    resolveClaims(farmer.farmerId, configId, landRef || undefined)
      .then((r) => live && setResolved(r))
      .catch((e: Error) => {
        if (!live) return
        setResolved(undefined)
        onError(e.message)
      })
      .finally(() => live && setResolving(false))
    return () => {
      live = false
    }
  }, [configId, landRef, farmer.farmerId, onError])

  const selected = types?.find((t) => t.configId === configId)
  const blocking = resolved?.missing ?? []

  const issue = useCallback(async () => {
    if (!configId) return
    setCreating(true)
    try {
      setOffer(await createOffer(farmer.farmerId, configId, landRef || undefined, delivery))
    } catch (e) {
      onError((e as Error).message)
    } finally {
      setCreating(false)
    }
  }, [configId, farmer.farmerId, landRef, delivery, onError])

  if (loadError) {
    return (
      <Alert kind="err">
        Couldn't load credential types. <span className="dim">{loadError}</span>
      </Alert>
    )
  }
  if (!types) return <Loading label="Loading credential types…" />

  if (!configId) {
    return (
      <Empty
        icon="◈"
        title="No credential type bound to this issuer"
        body={`${issuer.name} has no credential type yet, so there is nothing to issue. Bind one from the Issuers page, then come back.`}
      />
    )
  }

  if (!selected) {
    // The issuer names a configuration the schema registry no longer publishes.
    // Silently falling back to another type would attribute a credential to the
    // wrong authority, so this stops instead.
    return (
      <Alert kind="err">
        <strong>{issuer.name}</strong> is bound to credential type{' '}
        <span className="mono">{configId}</span>, which the schema registry does not currently
        publish as issuable
        {hidden > 0 && ` (${hidden} type(s) are hidden because their author DID is not
        wallet-resolvable)`}
        . Re-bind this issuer to an available type.
      </Alert>
    )
  }

  return (
    <div className="issue-grid">
      <div>
        <div className="card">
          <div className="card-head">
            <div className="grow">
              <h2 className="card-title">1 · What {issuer.name} issues</h2>
              <p className="card-note">
                Bound to this issuer — change it on the Issuers page rather than per holder.
              </p>
            </div>
          </div>
          <div className="card-body">
            <div className="bound-type">
              <span className="issuer-icon sm" aria-hidden>
                {issuer.icon || '◈'}
              </span>
              <div>
                <div className="tname">{selected.name}</div>
                <div className="tmeta">
                  {selected.attributes.length} attributes · SD-JWT VC · signed by{' '}
                  <span className="mono">{selected.issuer}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {usesLand && land.length > 1 && (
          <div className="card">
            <div className="card-head">
              <div className="grow">
                <h2 className="card-title">2 · Which land parcel?</h2>
                <p className="card-note">
                  This {(issuer.holderLabel || 'holder').toLowerCase()} holds {land.length} parcels;
                  a credential describes one.
                </p>
              </div>
            </div>
            <div className="card-body">
              <div className="typelist">
                {land.map((p) => (
                  <button
                    key={p.landRecordRef}
                    className={`typecard ${landRef === p.landRecordRef ? 'sel' : ''}`}
                    onClick={() => setLandRef(p.landRecordRef)}
                    aria-pressed={landRef === p.landRecordRef}
                  >
                    <span className="radio" aria-hidden />
                    <span>
                      <span className="tname">{p.landRecordRef}</span>
                      <span className="tmeta">
                        {p.landAreaAcres} acres
                        {p.farmLocation ? ` · ${p.farmLocation}` : ''}
                        {p.ownershipType ? ` · ${p.ownershipType}` : ''}
                      </span>
                    </span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {selected && (
          <div className="card">
            <div className="card-head">
              <div className="grow">
                <h2 className="card-title">
                  {usesLand && land.length > 1 ? '3' : '2'} · What will be issued
                </h2>
                <p className="card-note">
                  Resolved from the registry. Not editable — fix the record to change a value.
                </p>
              </div>
              {resolving && <Spinner />}
            </div>

            {!resolved ? (
              <Loading label="Resolving claims…" />
            ) : (
              <>
                {blocking.length > 0 && (
                  <div className="card-body" style={{ paddingBottom: 0 }}>
                    <Alert kind="err">
                      Missing required {blocking.length === 1 ? 'value' : 'values'}:{' '}
                      <strong>{blocking.join(', ')}</strong>. Add{' '}
                      {blocking.length === 1 ? 'it' : 'them'} to this{' '}
                      {(issuer.holderLabel || 'holder').toLowerCase()}'s records first — issuing
                      without {blocking.length === 1 ? 'it' : 'them'} fails at the credential
                      endpoint.
                    </Alert>
                  </div>
                )}
                <div className="tablewrap">
                  <table className="claimtable">
                    <thead>
                      <tr>
                        <th>Attribute</th>
                        <th>Value</th>
                        <th>From</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selected.attributes.map((a) => {
                        const v = resolved.claims[a]
                        const missing = blocking.includes(a)
                        const blank = v === undefined || v === null || v === ''
                        return (
                          <tr key={a}>
                            <td className="attr">
                              {a}
                              {selected.required.includes(a) && (
                                <span className="req" title="Required by the schema">
                                  {' '}
                                  *
                                </span>
                              )}
                            </td>
                            <td className={`val ${missing ? 'missing' : ''}`}>
                              {missing ? 'missing' : blank ? '—' : String(v)}
                            </td>
                            <td className="src">{resolved.sources[a] ?? '—'}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">Hand over</h2>
            <p className="card-note">
              {holder} scans this in their wallet
            </p>
          </div>
        </div>
        <div className="card-body">
          {!offer && (
            <div className="typelist" style={{ marginBottom: 16 }}>
              <button
                className={`typecard ${delivery === 'authorization_code' ? 'sel' : ''}`}
                onClick={() => setDelivery('authorization_code')}
                aria-pressed={delivery === 'authorization_code'}
                disabled={!canSignIn}
                title={canSignIn ? undefined : 'Link a wallet login on the Profile tab first'}
              >
                <span className="radio" aria-hidden />
                <span>
                  <span className="tname">{holder} signs in</span>
                  <span className="tmeta">
                    {canSignIn
                      ? 'The wallet opens a login page — no PIN to read out'
                      : 'Needs a linked wallet login (Profile tab)'}
                  </span>
                </span>
              </button>
              <button
                className={`typecard ${delivery === 'pre-authorized_code' ? 'sel' : ''}`}
                onClick={() => setDelivery('pre-authorized_code')}
                aria-pressed={delivery === 'pre-authorized_code'}
              >
                <span className="radio" aria-hidden />
                <span>
                  <span className="tname">QR code and PIN</span>
                  <span className="tmeta">
                    You read a 6-digit code to the {holder.toLowerCase()}
                  </span>
                </span>
              </button>
            </div>
          )}
          <WalletHandoff
            offer={offer}
            onCreate={issue}
            onReset={() => setOffer(undefined)}
            creating={creating}
            disabled={!selected || !resolved || blocking.length > 0}
            idleTitle="No offer yet"
            idleBody={
              delivery === 'authorization_code'
                ? `Generate a QR code. The ${holder.toLowerCase()} scans it and signs in with their own username and password.`
                : 'Generate an offer to get a QR code and a one-time PIN.'
            }
            createLabel="Generate offer"
            pinHint={`Read this to the ${holder.toLowerCase()}. Their wallet will ask for it — it's what proves the QR reached the right person.`}
          />
        </div>
      </div>
    </div>
  )
}
