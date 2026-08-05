import type { CredentialType } from '../types'

/**
 * Which attributes to ask for, built from the chosen type's own schema rather
 * than a fixed list — so a newly created credential type is immediately usable
 * with no code change. Checkboxes (not presets) because the useful demo is
 * asking for *less* than everything, and that varies per credential.
 */
export function ClaimPicker({
  type,
  selected,
  onToggle,
  onSelectAll,
  onSelectNone,
}: {
  type: CredentialType
  selected: string[]
  onToggle: (attr: string) => void
  onSelectAll: () => void
  onSelectNone: () => void
}) {
  return (
    <>
      <div className="field-label">
        Attributes to request
        <span>
          {selected.length} of {type.attributes.length} selected
        </span>
      </div>

      <div className="claim-grid">
        {type.attributes.map((attr) => {
          const on = selected.includes(attr)
          return (
            <button
              key={attr}
              type="button"
              role="checkbox"
              aria-checked={on}
              data-on={on}
              className="claim-opt"
              onClick={() => onToggle(attr)}
              title={type.descriptions[attr] || attr}
            >
              <span className="box" aria-hidden>
                {on ? '✓' : ''}
              </span>
              <span className="claim-opt-body">
                <b>{attr}</b>
                {type.descriptions[attr] && <small>{type.descriptions[attr]}</small>}
              </span>
            </button>
          )
        })}
      </div>

      <div className="quick">
        <button className="btn-link" onClick={onSelectAll} type="button">
          Select all
        </button>
        <button className="btn-link" onClick={onSelectNone} type="button">
          Clear
        </button>
        {selected.length === 0 && <span className="quick-hint">Pick at least one attribute</span>}
      </div>
    </>
  )
}
