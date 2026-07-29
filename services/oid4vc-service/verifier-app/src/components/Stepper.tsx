/**
 * The `← Back` / `Step n of N` row above each step, mirroring the reference's
 * single-task header. Back is omitted on the first step rather than disabled, so
 * there is nothing dead to click.
 */
export function Stepper({
  step,
  total,
  onBack,
}: {
  step: number
  total: number
  onBack?: () => void
}) {
  return (
    <div className="steprow">
      {onBack ? (
        <button type="button" className="back" onClick={onBack}>
          <span aria-hidden>←</span> Back
        </button>
      ) : (
        <span />
      )}
      <span className="stepcount">
        Step {step} of {total}
      </span>
    </div>
  )
}
