/**
 * Brand-panel artwork: a credential card with only some of its fields lit, a
 * verification seal, and a QR glyph.
 *
 * The half-lit fields are the point — two bright rows (disclosed) against two
 * dimmed, padlocked rows (withheld) is exactly what this app does, where a
 * generic network graphic said nothing about credentials at all. The seal's
 * concentric rings read as the verification result radiating outward.
 *
 * Decorative, so hidden from assistive technology; kept at low opacity so the
 * headline over it stays comfortably above AA.
 */
export function CredentialArt() {
  return (
    <svg
      className="hero-art"
      viewBox="0 0 120 120"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden
    >
      <defs>
        <radialGradient id="ca-glowA">
          <stop offset="0%" stopColor="#ffdb73" stopOpacity="0.55" />
          <stop offset="100%" stopColor="#ffdb73" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="ca-glowB">
          <stop offset="0%" stopColor="#cc8545" stopOpacity="0.34" />
          <stop offset="100%" stopColor="#cc8545" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="ca-glowC">
          <stop offset="0%" stopColor="#e8b678" stopOpacity="0.40" />
          <stop offset="100%" stopColor="#e8b678" stopOpacity="0" />
        </radialGradient>
      </defs>

      {/* ambient warmth, unchanged in spirit from before */}
      <circle cx="26" cy="24" r="30" fill="url(#ca-glowA)" />
      <circle cx="96" cy="34" r="28" fill="url(#ca-glowB)" />
      <circle cx="72" cy="96" r="32" fill="url(#ca-glowC)" />

      {/* the credential */}
      <g transform="rotate(-7 68 52)">
        <rect
          x="34"
          y="28"
          width="68"
          height="44"
          rx="6"
          fill="#ffffff"
          fillOpacity="0.55"
          stroke="#a95236"
          strokeOpacity="0.34"
          strokeWidth="0.7"
        />

        {/* portrait */}
        <circle cx="46" cy="42" r="6" fill="#cc8545" fillOpacity="0.34" />
        <path
          d="M40 56c0-3.6 2.7-6.2 6-6.2s6 2.6 6 6.2z"
          fill="#cc8545"
          fillOpacity="0.28"
        />

        {/* disclosed fields — bright */}
        <rect x="58" y="36" width="34" height="2.6" rx="1.3" fill="#a95236" fillOpacity="0.60" />
        <rect x="58" y="43" width="26" height="2.6" rx="1.3" fill="#a95236" fillOpacity="0.46" />

        {/* withheld fields — dimmed, with a padlock alongside */}
        <rect x="58" y="50" width="30" height="2.6" rx="1.3" fill="#cc8545" fillOpacity="0.20" />
        <rect x="58" y="57" width="20" height="2.6" rx="1.3" fill="#cc8545" fillOpacity="0.20" />
        <g fill="#cc8545" fillOpacity="0.42">
          <rect x="92" y="49.4" width="4" height="3" rx="0.8" />
          <path d="M92.9 49.4v-1a1.1 1.1 0 0 1 2.2 0v1h-.8v-1a.3.3 0 0 0-.6 0v1z" />
        </g>

        {/* QR glyph, bottom-left of the card */}
        <g fill="#a95236" fillOpacity="0.42" transform="translate(38 60)">
          <rect width="3.2" height="3.2" rx="0.5" />
          <rect x="4.4" width="3.2" height="3.2" rx="0.5" />
          <rect y="4.4" width="3.2" height="3.2" rx="0.5" />
          <rect x="5.6" y="5.6" width="2" height="2" rx="0.4" />
        </g>
      </g>

      {/* dots joined by dashed connectors — sunbird.org's own hero motif */}
      <g stroke="#cc8545" strokeOpacity="0.5" strokeWidth="0.5" strokeDasharray="2 2.4" fill="none">
        <line x1="18" y1="16" x2="36" y2="30" />
        <line x1="108" y1="20" x2="96" y2="32" />
        <line x1="24" y1="98" x2="44" y2="82" />
        <line x1="104" y1="108" x2="96" y2="96" />
      </g>
      <g>
        <circle cx="16" cy="14" r="3.4" fill="#ffdb73" />
        <circle cx="110" cy="18" r="2.6" fill="#cc8545" fillOpacity="0.85" />
        <circle cx="22" cy="100" r="2.8" fill="#cc8545" fillOpacity="0.75" />
        <circle cx="106" cy="110" r="3.6" fill="#ffdb73" />
        <circle cx="12" cy="58" r="2.2" fill="#ffdb73" />
      </g>

      {/* verification seal, radiating */}
      <g transform="translate(92 84)">
        <circle r="19" fill="none" stroke="#cc8545" strokeOpacity="0.22" strokeWidth="0.6" />
        <circle r="14" fill="none" stroke="#cc8545" strokeOpacity="0.30" strokeWidth="0.6" />
        <circle r="9.5" fill="#a95236" fillOpacity="0.92" stroke="#ffffff" strokeOpacity="0.55" strokeWidth="0.8" />
        <path
          d="M-4.2 0.4 l3 3 l5.6 -6"
          fill="none"
          stroke="#fffef4"
          strokeOpacity="0.95"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </g>
    </svg>
  )
}
