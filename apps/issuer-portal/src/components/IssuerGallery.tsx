import { useEffect, useMemo, useState } from 'react'
import { createIssuer, deleteIssuer, listCredentialTypes, listIssuers, updateIssuer } from '../api'
import { ISSUER_FIELDS } from '../fields'
import type { CredentialType, Issuer } from '../types'
import { EntityForm } from './EntityForm'
import { displayHost, IssuerLogo, safeHttpUrl } from './IssuerLogo'
import { Alert, Empty, Loading, Modal, Spinner } from './ui'

/**
 * The issuer gallery — the portal's entry point.
 *
 * Staff choose WHICH AUTHORITY they are acting for before they touch a holder
 * record, because that choice decides what the credential says and, through the
 * bound schema's author DID, who cryptographically signs it. Landing on a flat
 * list of every holder — the previous shape — quietly assumed one issuer.
 *
 * Presented as logo-first tiles rather than the table rows used everywhere else:
 * this is the one screen where the task is recognising an organisation rather than
 * reading data, and picking the wrong authority is a mistake nothing downstream
 * surfaces.
 */
export function IssuerGallery({
  onOpen,
  onToast,
  onError,
}: {
  onOpen: (issuer: Issuer) => void
  onToast: (message: string) => void
  onError: (message: string) => void
}) {
  const [issuers, setIssuers] = useState<Issuer[]>()
  const [unassigned, setUnassigned] = useState(0)
  const [loadError, setLoadError] = useState<string>()
  const [reload, setReload] = useState(0)
  const [creating, setCreating] = useState(false)
  const [binding, setBinding] = useState<Issuer>()
  const [types, setTypes] = useState<CredentialType[]>()
  const [search, setSearch] = useState('')

  useEffect(() => {
    let live = true
    setLoadError(undefined)
    listIssuers()
      .then((r) => {
        if (!live) return
        setIssuers(r.issuers)
        setUnassigned(r.unassigned)
      })
      .catch((e: Error) => live && setLoadError(e.message))
    return () => {
      live = false
    }
  }, [reload])

  // Fetched once, up front: the binding dialog needs them and so does the
  // "cannot issue yet" state on every tile.
  useEffect(() => {
    let live = true
    listCredentialTypes()
      .then((r) => live && setTypes(r.types))
      .catch(() => live && setTypes([]))
    return () => {
      live = false
    }
  }, [])

  const refresh = (message: string) => {
    onToast(message)
    setReload((n) => n + 1)
  }

  // Filtered in the browser, not the server: the issuer list is small by nature
  // (one row per authority), so a round trip per keystroke would add latency for
  // nothing.
  const shown = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q || !issuers) return issuers ?? []
    return issuers.filter((i) =>
      [i.name, i.category, i.credentialName, i.holderLabel, i.issuerId]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q)),
    )
  }, [issuers, search])

  const totalHolders = (issuers ?? []).reduce((n, i) => n + (i.holderCount ?? 0), 0)

  if (loadError) {
    return (
      <>
        <div className="page-head">
          <div className="grow">
            <h1 className="page-title">Issuers</h1>
          </div>
        </div>
        <Alert kind="err">
          Couldn't load issuers. <span className="dim">{loadError}</span>
        </Alert>
        <p className="card-note">
          If this deployment has just been upgraded, the registry may not have the{' '}
          <span className="mono">Issuer</span> entity yet — it is added by the schema files in{' '}
          <span className="mono">registry-schemas/</span> and picked up when the registry restarts.
        </p>
      </>
    )
  }
  if (!issuers) return <Loading label="Loading issuers…" />

  return (
    <>
      <div className="hero">
        <div className="hero-inner">
          <h1 className="hero-title">
              Issue credentials <span className="accent">straight from the record</span>
          </h1>
          <p className="hero-sub">
            Each issuer is its own authority, with its own credential and its own holders. Pick the
            one you are acting for — everything else follows from that choice.
          </p>

          <div className="hero-row">
            <div className="search-lg">
              <span className="mag" aria-hidden>
                ⌕
              </span>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search issuers by name, category or credential"
                aria-label="Search issuers"
              />
            </div>
            <button className="btn" onClick={() => setCreating(true)}>
              + New issuer
            </button>
          </div>

          <div className="hero-stats">
            <span>
              <b>{issuers.length}</b> issuer{issuers.length === 1 ? '' : 's'}
            </span>
            <span>
              <b>{totalHolders}</b> holder{totalHolders === 1 ? '' : 's'}
            </span>
            <span>
              <b>{types?.length ?? '—'}</b> credential type
              {types?.length === 1 ? '' : 's'} published
            </span>
          </div>
        </div>
      </div>

      {issuers.length === 0 ? (
        <Empty
          icon="◈"
          title="No issuers yet"
          body="Create the first issuing authority, bind a credential type to it, then add the holders it issues to."
          action={
            <button className="btn" onClick={() => setCreating(true)}>
              + New issuer
            </button>
          }
        />
      ) : shown.length === 0 ? (
        <Empty
          icon="⌕"
          title="No matching issuers"
          body={`Nothing matched “${search.trim()}”.`}
          action={
            <button className="btn-ghost" onClick={() => setSearch('')}>
              Clear search
            </button>
          }
        />
      ) : (
        <div className="tiles">
          {shown.map((i) => {
            const bound = Boolean(i.credentialConfigId)
            const site = safeHttpUrl(i.url)
            return (
              <div
                key={i.issuerId}
                className={`tile ${bound ? '' : 'unbound'}`}
                style={{ ['--accent' as string]: i.accent || '#a95236' }}
              >
                {/* Outside the main button: nesting interactive elements inside a
                    button is invalid, and these are secondary to opening it. */}
                <div className={`tile-tools ${bound ? '' : 'pinned'}`}>
                  {site && (
                    <a
                      className="tile-tool"
                      href={site}
                      target="_blank"
                      /* Without noopener the opened page gets a handle on this
                         window, and this URL is staff-editable. */
                      rel="noopener noreferrer"
                      title={`${i.name} — ${displayHost(i.url) ?? 'website'}`}
                      aria-label={`${i.name} website`}
                    >
                      ↗
                    </a>
                  )}
                  <button
                    className="tile-tool"
                    onClick={() => setBinding(i)}
                    title={bound ? 'Credential type and settings' : 'Bind a credential type'}
                    aria-label={`Settings for ${i.name}`}
                  >
                    {bound ? '⚙' : '!'}
                  </button>
                </div>

                <button
                  className="tile-open"
                  onClick={() => onOpen(i)}
                  aria-label={`Open ${i.name}`}
                >
                  <IssuerLogo issuer={i} size={68} className="tile-logo" />
                  <span className="tile-name" title={i.name}>
                    {i.name}
                  </span>
                  {i.category && <span className="tile-cat">{i.category}</span>}
                  <span className="tile-cred" title={i.credentialName}>
                    {bound ? i.credentialName : 'No credential yet'}
                  </span>
                </button>

                <div className="tile-foot">
                  <span className="tile-count">
                    <b>{i.holderCount ?? '—'}</b>
                    {(i.holderLabel || 'holder').toLowerCase()}
                    {i.holderCount === 1 ? '' : 's'}
                  </span>
                  <span className={`tile-state ${bound ? 'ready' : ''}`}>
                    {bound ? 'Ready to issue' : 'Set up needed'}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {unassigned > 0 && (
        <Alert kind="warn">
          <strong>
            {unassigned} holder{unassigned === 1 ? '' : 's'}
          </strong>{' '}
          {unassigned === 1 ? 'is' : 'are'} not assigned to any issuer, so no issuer's list shows{' '}
          {unassigned === 1 ? 'it' : 'them'}. Open <strong>All holders</strong> in the sidebar and set
          an issuer.
        </Alert>
      )}

      {creating && (
        <Modal title="New issuer" onClose={() => setCreating(false)}>
          <p className="card-note" style={{ marginTop: 0 }}>
            The issuer ID is generated from the name. You can bind a credential type straight after
            creating it — an issuer with none can hold records but cannot issue.
          </p>
          <EntityForm
            fields={ISSUER_FIELDS}
            initial={{ category: 'Other', icon: '◈' }}
            submitLabel="Create issuer"
            onCancel={() => setCreating(false)}
            onSubmit={async (payload) => {
              const created = await createIssuer(payload as Partial<Issuer>)
              setCreating(false)
              refresh(`Issuer ${created.name} created`)
              // Straight into binding: an issuer with no credential type cannot do
              // the one thing it exists for, and this is the moment staff have the
              // context to choose.
              setBinding(created)
            }}
          />
        </Modal>
      )}

      {binding && (
        <BindCredentialModal
          issuer={binding}
          types={types}
          onClose={() => setBinding(undefined)}
          onBound={(message) => {
            setBinding(undefined)
            refresh(message)
          }}
          onError={onError}
          onDeleted={(message) => {
            setBinding(undefined)
            refresh(message)
          }}
        />
      )}
    </>
  )
}

/**
 * Binds a credential type to an issuer, and offers deletion.
 *
 * The type is chosen from what the schema registry actually publishes rather than
 * typed: a configuration id that does not exist fails only at issuance, by which
 * point a holder is standing there with a wallet open. The signing DID shown for
 * each option is the schema's `author` — the key that will actually sign, which is
 * why it belongs in front of whoever is choosing.
 */
function BindCredentialModal({
  issuer,
  types,
  onClose,
  onBound,
  onDeleted,
  onError,
}: {
  issuer: Issuer
  types?: CredentialType[]
  onClose: () => void
  onBound: (message: string) => void
  onDeleted: (message: string) => void
  onError: (message: string) => void
}) {
  const [configId, setConfigId] = useState(issuer.credentialConfigId ?? '')
  const [busy, setBusy] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)

  async function save() {
    const chosen = types?.find((t) => t.configId === configId)
    setBusy(true)
    try {
      await updateIssuer(issuer.issuerId, {
        credentialConfigId: configId,
        // Denormalised so the gallery renders without a schema call per tile.
        credentialName: chosen?.name ?? '',
        // Recorded for display: the DID that will sign this issuer's credentials
        // is the schema author's, so staff can see it without reading metadata.
        did: chosen?.issuer ?? issuer.did,
      })
      onBound(configId ? `${issuer.name} now issues ${chosen?.name}` : 'Credential type cleared')
    } catch (e) {
      onError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={issuer.name} onClose={onClose}>
      <div className="ws-head" style={{ ['--accent' as string]: issuer.accent || '#a95236' }}>
        <IssuerLogo issuer={issuer} size={52} className="ws-logo" />
        <div className="ws-text">
          <div className="ws-meta">
            <span className="chip mono">{issuer.issuerId}</span>
            {issuer.holderLabel && <span className="chip">Holders: {issuer.holderLabel}</span>}
          </div>
          {issuer.did && (
            <div className="ws-meta">
              <span className="chip ws-did" title={issuer.did}>
                {issuer.did}
              </span>
            </div>
          )}
        </div>
      </div>

      <h3 style={{ margin: '0 0 4px', fontSize: 15 }}>Credential type</h3>
      <p className="card-note" style={{ marginTop: 0, marginBottom: 14 }}>
        What this authority issues. Discovered live from the schema registry — only SD-JWT types with
        a wallet-resolvable author DID are offered.
      </p>

      {!types ? (
        <Loading label="Loading credential types…" />
      ) : types.length === 0 ? (
        <Alert kind="warn">
          No issuable credential types were found. A type must be published in the schema registry
          with the <span className="mono">vc+sd-jwt</span> format and an author DID a wallet can
          resolve.
        </Alert>
      ) : (
        <>
          <div className="typelist">
            {types.map((t) => (
              <button
                key={t.configId}
                className={`typecard ${configId === t.configId ? 'sel' : ''}`}
                onClick={() => setConfigId(t.configId)}
                aria-pressed={configId === t.configId}
              >
                <span className="radio" aria-hidden />
                <span>
                  <span className="tname">{t.name}</span>
                  <span className="tmeta">
                    {t.attributes.length} attributes · signed by{' '}
                    <span className="mono">{t.issuer}</span>
                  </span>
                </span>
              </button>
            ))}
          </div>
          <div className="form-actions">
            <button className="btn-ghost" onClick={onClose} disabled={busy}>
              Cancel
            </button>
            <button className="btn" onClick={save} disabled={busy || !configId}>
              {busy ? (
                <>
                  <Spinner /> Saving…
                </>
              ) : (
                'Bind credential type'
              )}
            </button>
          </div>
        </>
      )}

      <div className="card" style={{ marginTop: 22 }}>
        <div className="card-head">
          <div className="grow">
            <h2 className="card-title">Danger zone</h2>
            <p className="card-note">
              An issuer can only be deleted once it has no holders. Credentials it has already issued
              stay valid — deleting an issuer is not revocation.
            </p>
          </div>
          {!confirmDelete && (
            <button
              className="btn-ghost sm btn-danger"
              onClick={() => setConfirmDelete(true)}
              disabled={busy}
            >
              Delete
            </button>
          )}
        </div>
        {confirmDelete && (
          <div className="card-body">
            <Alert kind="warn">
              Delete <strong>{issuer.name}</strong>? This cannot be undone.
            </Alert>
            <div className="form-actions">
              <button className="btn-ghost" onClick={() => setConfirmDelete(false)} disabled={busy}>
                Keep it
              </button>
              <button
                className="btn btn-danger"
                disabled={busy}
                onClick={async () => {
                  setBusy(true)
                  try {
                    await deleteIssuer(issuer.issuerId)
                    onDeleted(`Issuer ${issuer.name} deleted`)
                  } catch (e) {
                    onError((e as Error).message)
                    setConfirmDelete(false)
                  } finally {
                    setBusy(false)
                  }
                }}
              >
                Delete issuer
              </button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
