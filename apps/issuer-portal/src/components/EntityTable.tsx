import { useState } from 'react'
import { createRelated, deleteRelated, updateRelated } from '../api'
import { ENTITIES, blankRecord } from '../fields'
import type { EntityKind, Related } from '../types'
import { EntityForm } from './EntityForm'
import { Empty, Modal } from './ui'

/**
 * List + add/edit/delete for one of the three child entities. Generic over the
 * entity kind so land parcels, crop cycles and seed distributions all behave
 * the same way rather than drifting into three near-identical screens.
 */
export function EntityTable({
  kind,
  farmerId,
  rows,
  onChanged,
  onError,
}: {
  kind: EntityKind
  farmerId: string
  rows: Related[]
  /** Called after any successful mutation, with a message for the toast. */
  onChanged: (message: string) => void
  onError: (message: string) => void
}) {
  const meta = ENTITIES[kind]
  const cols = meta.fields.filter((f) => f.inTable)
  const [editing, setEditing] = useState<Related | 'new' | null>(null)
  const [deleting, setDeleting] = useState<string>()

  async function remove(osid: string) {
    setDeleting(osid)
    try {
      await deleteRelated(kind, osid)
      onChanged(`${meta.singular} deleted`)
    } catch (e) {
      onError((e as Error).message)
    } finally {
      setDeleting(undefined)
    }
  }

  const asRecord = (r: Related) => r as unknown as Record<string, unknown>

  return (
    <div className="card">
      <div className="card-head">
        <div className="grow">
          <h2 className="card-title">
            <span aria-hidden style={{ marginRight: 7 }}>
              {meta.icon}
            </span>
            {meta.plural}
          </h2>
          <p className="card-note">
            {rows.length} record{rows.length === 1 ? '' : 's'} for this farmer
          </p>
        </div>
        <button className="btn-ghost sm" onClick={() => setEditing('new')}>
          + Add {meta.singular}
        </button>
      </div>

      {rows.length === 0 ? (
        <Empty
          icon={meta.icon}
          title={`No ${meta.plural.toLowerCase()} yet`}
          body={`Add a ${meta.singular} so it can be included in an issued credential.`}
          action={
            <button className="btn-ghost" onClick={() => setEditing('new')}>
              + Add {meta.singular}
            </button>
          }
        />
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
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => {
                const rec = asRecord(r)
                const osid = String(rec.osid ?? '')
                return (
                  <tr key={osid || i}>
                    {cols.map((c) => {
                      const v = rec[c.name]
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
                    <td className="actions">
                      <button className="btn-ghost sm" onClick={() => setEditing(r)}>
                        Edit
                      </button>{' '}
                      <button
                        className="btn-ghost sm btn-danger"
                        disabled={!osid || deleting === osid}
                        onClick={() => remove(osid)}
                      >
                        {deleting === osid ? 'Deleting…' : 'Delete'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <Modal
          title={editing === 'new' ? `Add ${meta.singular}` : `Edit ${meta.singular}`}
          onClose={() => setEditing(null)}
        >
          <EntityForm
            fields={meta.fields}
            initial={editing === 'new' ? blankRecord(kind, farmerId) : asRecord(editing)}
            submitLabel={editing === 'new' ? `Add ${meta.singular}` : 'Save changes'}
            onCancel={() => setEditing(null)}
            onSubmit={async (payload) => {
              if (editing === 'new') {
                await createRelated(kind, payload as unknown as Related)
                onChanged(`${meta.singular} added`)
              } else {
                const osid = String(asRecord(editing).osid ?? '')
                await updateRelated(kind, osid, payload as unknown as Related)
                onChanged(`${meta.singular} updated`)
              }
              setEditing(null)
            }}
          />
        </Modal>
      )}
    </div>
  )
}
