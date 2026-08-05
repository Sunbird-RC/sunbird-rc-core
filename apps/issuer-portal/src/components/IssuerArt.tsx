/**
 * Login-panel artwork: a registry record on the left feeding a signed credential
 * on the right, with a key glyph between them.
 *
 * The two-panel composition is the point — this console's whole job is turning a
 * held record into a signed credential, so the artwork shows a row of data lines
 * becoming a sealed card. Same warm palette and dashed-connector motif as the
 * verifier console's artwork, so the two read as one product family.
 *
 * Decorative, so hidden from assistive technology; low opacity keeps the
 * headline over it comfortably above AA.
 */
export function IssuerArt() {
  return (
    <svg className="hero-art" viewBox="0 0 120 120" preserveAspectRatio="xMidYMid slice" aria-hidden>
      <defs>
        <radialGradient id="ia-glowA">
          <stop offset="0%" stopColor="#ffdb73" stopOpacity="0.55" />
          <stop offset="100%" stopColor="#ffdb73" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="ia-glowB">
          <stop offset="0%" stopColor="#cc8545" stopOpacity="0.34" />
          <stop offset="100%" stopColor="#cc8545" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="ia-glowC">
          <stop offset="0%" stopColor="#e8b678" stopOpacity="0.4" />
          <stop offset="100%" stopColor="#e8b678" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* ambient warmth */}
      <circle cx="24" cy="26" r="30" fill="url(#ia-glowA)" />
      <circle cx="98" cy="36" r="28" fill="url(#ia-glowB)" />
      <circle cx="66" cy="98" r="32" fill="url(#ia-glowC)" />

      {/* the registry record — a stack of data rows */}
      <g transform="rotate(-5 40 54)">
        <rect
          x="16"
          y="34"
          width="42"
          height="46"
          rx="5"
          fill="#ffffff"
          fillOpacity="0.5"
          stroke="#a95236"
          strokeOpacity="0.3"
          strokeWidth="0.7"
        />
        <rect x="21" y="40" width="24" height="2.4" rx="1.2" fill="#a95236" fillOpacity="0.5" />
        <rect x="21" y="47" width="30" height="2.2" rx="1.1" fill="#cc8545" fillOpacity="0.36" />
        <rect x="21" y="53" width="26" height="2.2" rx="1.1" fill="#cc8545" fillOpacity="0.36" />
        <rect x="21" y="59" width="31" height="2.2" rx="1.1" fill="#cc8545" fillOpacity="0.36" />
        <rect x="21" y="65" width="19" height="2.2" rx="1.1" fill="#cc8545" fillOpacity="0.36" />
        {/* a small field-count tag, hinting at structured data */}
        <rect x="21" y="72" width="12" height="4" rx="2" fill="#a95236" fillOpacity="0.24" />
      </g>

      {/* transfer: dashed arrow with a key glyph, record -> credential */}
      <g stroke="#a95236" strokeOpacity="0.42" strokeWidth="0.8" fill="none">
        <path d="M60 56 h11" strokeDasharray="2 2.2" />
        <path d="M69 53.4 l2.8 2.6 l-2.8 2.6" strokeLinecap="round" strokeLinejoin="round" />
      </g>
      <g fill="#a95236" fillOpacity="0.5" transform="translate(63 62)">
        <circle cx="2" cy="2" r="2" fill="none" stroke="#a95236" strokeOpacity="0.5" strokeWidth="0.8" />
        <rect x="3.6" y="1.5" width="5.6" height="1" rx="0.5" />
        <rect x="7.4" y="2.5" width="1" height="1.8" rx="0.5" />
      </g>

      {/* the issued credential — sealed */}
      <g transform="rotate(6 92 52)">
        <rect
          x="72"
          y="30"
          width="38"
          height="44"
          rx="5"
          fill="#ffffff"
          fillOpacity="0.62"
          stroke="#a95236"
          strokeOpacity="0.34"
          strokeWidth="0.7"
        />
        <rect x="77" y="36" width="20" height="2.4" rx="1.2" fill="#a95236" fillOpacity="0.6" />
        <rect x="77" y="43" width="26" height="2.2" rx="1.1" fill="#a95236" fillOpacity="0.42" />
        <rect x="77" y="49" width="22" height="2.2" rx="1.1" fill="#a95236" fillOpacity="0.42" />
        {/* signature squiggle — the credential is signed, the record is not */}
        <path
          d="M77 62 c2.4 -3.4 4.2 1.6 6.4 -1 c2 -2.4 3.4 2.2 5.6 -0.4 c1.8 -2 3.2 1.6 5 -0.6"
          fill="none"
          stroke="#cc8545"
          strokeOpacity="0.6"
          strokeWidth="0.9"
          strokeLinecap="round"
        />
      </g>

      {/* dots joined by dashed connectors — sunbird.org's own hero motif */}
      <g stroke="#cc8545" strokeOpacity="0.5" strokeWidth="0.5" strokeDasharray="2 2.4" fill="none">
        <line x1="16" y1="18" x2="30" y2="32" />
        <line x1="110" y1="20" x2="102" y2="30" />
        <line x1="22" y1="100" x2="38" y2="86" />
      </g>
      <g>
        <circle cx="14" cy="16" r="3.4" fill="#ffdb73" />
        <circle cx="112" cy="18" r="2.6" fill="#cc8545" fillOpacity="0.85" />
        <circle cx="20" cy="102" r="2.8" fill="#cc8545" fillOpacity="0.75" />
        <circle cx="104" cy="108" r="3.4" fill="#ffdb73" />
        <circle cx="10" cy="60" r="2.2" fill="#ffdb73" />
      </g>

      {/* issuer seal, radiating — the authority behind the signature */}
      <g transform="translate(92 88)">
        <circle r="17" fill="none" stroke="#cc8545" strokeOpacity="0.22" strokeWidth="0.6" />
        <circle r="12.5" fill="none" stroke="#cc8545" strokeOpacity="0.3" strokeWidth="0.6" />
        <circle
          r="8.6"
          fill="#a95236"
          fillOpacity="0.92"
          stroke="#ffffff"
          strokeOpacity="0.55"
          strokeWidth="0.8"
        />
        {/* a stamp mark rather than the verifier's tick: this side issues */}
        <path
          d="M-3.4 -1.2 h6.8 M-3.4 1.6 h6.8 M0 -4 v8"
          fill="none"
          stroke="#fffef4"
          strokeOpacity="0.95"
          strokeWidth="1.5"
          strokeLinecap="round"
        />
      </g>
    </svg>
  )
}
