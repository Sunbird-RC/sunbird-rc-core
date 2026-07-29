import { qrSrc } from '../api'
import { Countdown } from './Countdown'

/**
 * The QR lives in the right-hand stage rather than under the request controls:
 * scanning is the active step, so it belongs in the largest panel at eye level,
 * and the stage is otherwise empty while waiting. Keeping presentation and
 * issuance QRs in one component means both render at the same size and position.
 */
export function QrPanel({
  link,
  caption,
  footnote,
  ttl,
  onCopy,
  copied,
  chips,
}: {
  link: string
  caption: string
  footnote?: string
  ttl?: { left: number; total: number }
  onCopy?: () => void
  copied?: boolean
  /** Echo the requested attributes here: the QR replaces the request panel, so
   *  without this you cannot cross-check them against the wallet's consent
   *  screen while it is on the phone in front of you. */
  chips?: string[]
}) {
  return (
    <div className="stage-qr">
      <div className="qr-frame lg">
        <img src={qrSrc(link)} alt="Scan this with your wallet" width={272} height={272} />
      </div>
      <p className="qr-caption">{caption}</p>
      {chips && chips.length > 0 && (
        <div className="asked">
          <span>Requesting</span>
          {chips.map((c) => (
            <span className="chip" key={c}>
              {c}
            </span>
          ))}
        </div>
      )}
      {ttl && <Countdown left={ttl.left} total={ttl.total} />}
      {footnote && <div className="txn">{footnote}</div>}
      {onCopy && (
        <button className="btn-ghost narrow" onClick={onCopy}>
          {copied ? 'Copied' : 'Copy openid4vp:// link'}
        </button>
      )}
    </div>
  )
}
