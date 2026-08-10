// SD-JWT VC `vct` (Verifiable Credential Type) handling.
//
// Per draft-ietf-oauth-sd-jwt-vc, `vct` is a StringOrURI (RFC 7519): an
// arbitrary string is legal *unless* it contains a ':', in which case it MUST
// be a URI. So a bare display name like "National Identity Credential" is
// technically spec-legal — but some wallets resolve EVERY vct as a URL
// regardless, and fail with a URL-parsing error on the embedded space.
//
// Publishing an HTTPS URI instead is both the idiomatic choice (collision-
// resistant per the spec's own guidance) and the interoperable one, since the
// URI can actually be dereferenced for Type Metadata (see vct.controller.ts).
// A schema that already declares an absolute-URI vct is passed through
// untouched — that's the author's deliberate identifier.

// A vct is already a usable identifier if it's an absolute http(s) URI.
export function isAbsoluteHttpUri(value: string): boolean {
  return /^https?:\/\//i.test(value);
}

// "National Identity Credential" -> "national-identity-credential".
// Deliberately conservative: only [a-z0-9-] survives, so the result is always
// safe in a URL path and stable across the metadata/issuance/DCQL paths that
// all have to agree on the exact same string.
export function slugifyVct(name: string): string {
  return name
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '') // strip combining accents
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

// The canonical vct for a schema config. Returns the raw value unchanged when
// it's already an absolute URI; otherwise derives `<publicUrl>/vct/<slug>`.
export function normalizeVct(rawVct: string, publicUrl: string): string {
  if (!rawVct) return rawVct;
  if (isAbsoluteHttpUri(rawVct)) return rawVct;
  const slug = slugifyVct(rawVct);
  if (!slug) return rawVct; // nothing sluggable (e.g. all punctuation) — leave as-is
  return `${publicUrl.replace(/\/+$/, '')}/vct/${slug}`;
}
