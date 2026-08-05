import { useCallback, useEffect, useState } from 'react'
import { deleteFarmer, getFarmer, updateFarmer } from '../api'
import { ENTITIES, holderFields, issuerEntities } from '../fields'
import type {
  Crop,
  EntityKind,
  Farmer,
  Issuer,
  LandParcel,
  Qualification,
  Related,
  SeedDistribution,
} from '../types'
import { EntityForm } from './EntityForm'
import { EntityTable } from './EntityTable'
import { IssuePanel } from './IssuePanel'
import { LinkLoginCard } from './LinkLoginCard'
import { Alert, Badge, Loading, Modal } from './ui'

/** Either the profile, the issuance panel, or one of the issuer's record types. */
type Tab = 'profile' | 'issue' | EntityKind

type Detail = {
  farmer: Farmer
  land: LandParcel[]
  crops: Crop[]
  seeds: SeedDistribution[]
  qualifications: Qualification[]
  issuedCount: number
}

/**
 * One holder: their profile, the record types their issuer actually uses, and
 * issuance.
 *
 * Tabs rather than one long page because issuance needs the whole width for the
 * claim table beside the QR, and because staff doing bulk data entry shouldn't
 * scroll past an issuance flow they aren't using.
 *
 * WHICH tabs appear comes from the issuer's `recordEntities`, not from code: the
 * Age issuer needs none, and showing a Land parcels tab there would invite staff
 * to enter data that its credential can never carry.
 */
export function FarmerDetail({
  farmerId,
  issuer,
  onBack,
  onToast,
  onError,
  onMutated,
}: {
  farmerId: string
  /** The issuer being worked in. Absent on the unscoped "All holders" list. */
  issuer?: Issuer
  onBack: () => void
  onToast: (message: string) => void
  onError: (message: string) => void
  /** Tells the parent the list is stale (name changed, record deleted). */
  onMutated: () => void
}) {
  const [tab, setTab] = useState<Tab>('profile')
  const [detail, setDetail] = useState<Detail>()
  const [loadError, setLoadError] = useState<string>()
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let live = true
    setLoadError(undefined)
    getFarmer(farmerId)
      .then((d) => live && setDetail(d))
      .catch((e: Error) => live && setLoadError(e.message))
    return () => {
      live = false
    }
  }, [farmerId, reload])

  // A child mutation invalidates this holder AND the list behind it.
  const refresh = useCallback(
    (message: string) => {
      onToast(message)
      setReload((n) => n + 1)
      onMutated()
    },
    [onToast, onMutated],
  )

  const noun = issuer?.holderLabel ?? 'Holder'
  const backLabel = issuer ? `All ${noun.toLowerCase()}s` : 'All holders'

  if (loadError) {
    return (
      <>
        <button className="crumb" onClick={onBack}>
          ← {backLabel}
        </button>
        <Alert kind="err">
          Couldn't load this record. <span className="dim">{loadError}</span>
        </Alert>
      </>
    )
  }
  if (!detail) return <Loading label={`Loading ${noun.toLowerCase()}…`} />

  const { farmer, land, crops, seeds, qualifications, issuedCount } = detail
  const fields = holderFields(issuer?.holderLabel)

  const rowsFor = (kind: EntityKind): Related[] =>
    kind === 'LandParcel'
      ? land
      : kind === 'Crop'
        ? crops
        : kind === 'SeedDistribution'
          ? seeds
          : qualifications

  // The issuer decides which record types are relevant. With no issuer in
  // context — the "All holders" route — show every type that has records, so a
  // misfiled holder's data is still visible rather than silently hidden.
  const kinds: EntityKind[] = issuer
    ? issuerEntities(issuer.recordEntities)
    : (Object.keys(ENTITIES) as EntityKind[]).filter((k) => rowsFor(k).length > 0)

  const TABS: { key: Tab; label: string; n?: number }[] = [
    { key: 'profile', label: 'Profile' },
    ...kinds.map((k) => ({ key: k as Tab, label: ENTITIES[k].plural, n: rowsFor(k).length })),
    ...(issuer ? [{ key: 'issue' as Tab, label: 'Issue credential' }] : []),
  ]

  return (
    <>
      <button className="crumb" onClick={onBack}>
        ← {backLabel}
      </button>

      <div className="page-head">
        <div className="grow">
          <h1 className="page-title">{farmer.name}</h1>
          <p className="page-sub">
            <span className="mono">{farmer.farmerId}</span>
            {farmer.district ? ` · ${farmer.district}` : ''}
            {farmer.state ? `, ${farmer.state}` : ''}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {issuedCount > 0 && (
            <Badge kind="brand">
              {issuedCount} credential{issuedCount === 1 ? '' : 's'} issued
            </Badge>
          )}
          {farmer.keycloakUsername ? (
            <Badge kind="ok" dot>
              {farmer.keycloakUsername}
            </Badge>
          ) : (
            <Badge kind="neutral">No wallet login</Badge>
          )}
        </div>
      </div>

      {!issuer && !farmer.issuerId && (
        <Alert kind="warn">
          This record is not assigned to any issuer, so it appears under no issuer's holder list and
          cannot be issued from. Edit it and set an issuer.
        </Alert>
      )}

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}
            aria-current={tab === t.key ? 'page' : undefined}
          >
            {t.label}
            {t.n !== undefined && <span className="n">{t.n}</span>}
          </button>
        ))}
      </div>

      {tab === 'profile' && (
        <>
          <div className="card">
            <div className="card-head">
              <div className="grow">
                <h2 className="card-title">Profile</h2>
                <p className="card-note">These values feed the credential's identity claims.</p>
              </div>
              <button className="btn-ghost sm" onClick={() => setEditing(true)}>
                Edit
              </button>
            </div>
            <div className="tablewrap">
              <table>
                <tbody>
                  {fields.map((f) => {
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
                  <tr>
                    <td style={{ fontWeight: 600 }}>Issuer</td>
                    <td className={farmer.issuerId ? '' : 'dim'}>
                      {issuer?.name ?? farmer.issuerId ?? 'Unassigned'}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <LinkLoginCard farmer={farmer} holderLabel={noun} onChanged={refresh} onError={onError} />

          <div className="card">
            <div className="card-head">
              <div className="grow">
                <h2 className="card-title">Danger zone</h2>
                <p className="card-note">
                  Deleting the record does not revoke credentials already issued to a wallet.
                </p>
              </div>
              <button className="btn-ghost sm btn-danger" onClick={() => setConfirmDelete(true)}>
                Delete {noun.toLowerCase()}
              </button>
            </div>
          </div>
        </>
      )}

      {kinds.map(
        (kind) =>
          tab === kind && (
            <EntityTable
              key={kind}
              kind={kind}
              farmerId={farmer.farmerId}
              rows={rowsFor(kind)}
              onChanged={refresh}
              onError={onError}
            />
          ),
      )}

      {tab === 'issue' && issuer && (
        <IssuePanel farmer={farmer} issuer={issuer} land={land} onError={onError} />
      )}

      {editing && (
        <Modal title={`Edit ${noun.toLowerCase()}`} onClose={() => setEditing(false)}>
          <EntityForm
            fields={fields}
            initial={farmer as unknown as Record<string, unknown>}
            submitLabel="Save changes"
            lockedFields={['farmerId']}
            onCancel={() => setEditing(false)}
            onSubmit={async (payload) => {
              // The issuer assignment is preserved across an edit: it is not one
              // of the form's fields, and dropping it would silently orphan the
              // record from the issuer whose list staff are standing in.
              await updateFarmer(farmer.farmerId, {
                ...(payload as unknown as Farmer),
                issuerId: farmer.issuerId ?? issuer?.issuerId,
              })
              setEditing(false)
              refresh(`${noun} updated`)
            }}
          />
        </Modal>
      )}

      {confirmDelete && (
        <Modal title={`Delete this ${noun.toLowerCase()}?`} onClose={() => setConfirmDelete(false)}>
          <Alert kind="warn">
            This removes <strong>{farmer.name}</strong> ({farmer.farmerId}) and cannot be undone.
            Their supporting records stay in the registry and will be orphaned.
          </Alert>
          <p className="card-note">
            Credentials already issued to a wallet remain valid — deleting a record is not
            revocation.
          </p>
          <div className="form-actions">
            <button className="btn-ghost" onClick={() => setConfirmDelete(false)}>
              Cancel
            </button>
            <button
              className="btn btn-danger"
              onClick={async () => {
                try {
                  await deleteFarmer(farmer.farmerId)
                  onToast(`${noun} deleted`)
                  onMutated()
                  onBack()
                } catch (e) {
                  onError((e as Error).message)
                  setConfirmDelete(false)
                }
              }}
            >
              Delete {noun.toLowerCase()}
            </button>
          </div>
        </Modal>
      )}
    </>
  )
}
