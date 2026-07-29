import type { CredentialType } from '../types'

/**
 * Which credential to verify. Cards rather than a segmented control because the
 * list is discovered at runtime and can grow, and each option needs a second
 * line (issuer + attribute count) to be distinguishable.
 */
export function TypePicker({
  types,
  selected,
  onSelect,
}: {
  types: CredentialType[]
  selected: CredentialType | null
  onSelect: (t: CredentialType) => void
}) {
  return (
    <div className="type-list" role="radiogroup" aria-label="Credential type">
      {types.map((t) => (
        <button
          key={t.id}
          type="button"
          role="radio"
          aria-checked={selected?.id === t.id}
          data-on={selected?.id === t.id}
          className="type-card"
          onClick={() => onSelect(t)}
        >
          <span className="radio" aria-hidden />
          <span className="type-body">
            <b>{t.name}</b>
            <small>
              {t.attributes.length} attribute{t.attributes.length === 1 ? '' : 's'} · issued by{' '}
              {t.issuer.split(':').pop()?.slice(0, 8)}…
            </small>
          </span>
        </button>
      ))}
    </div>
  )
}
