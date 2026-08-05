import type {
  Crop,
  CredentialType,
  EntityKind,
  Farmer,
  Issuer,
  LandParcel,
  Offer,
  Qualification,
  Related,
  SeedDistribution,
  Session,
} from './types'

/**
 * Every request is prefixed with the app's base path, NOT sent root-relative.
 *
 * Behind the gateway this app lives at /issuer-portal/, and nginx routes only
 * that prefix to the BFF — a root-relative `/api/session` would hit the gateway
 * root, which points somewhere else entirely. It happens to work on the Vite dev
 * server either way, so this is exactly the kind of bug that only shows up once
 * deployed. Vite substitutes BASE_URL at build time from `base` in vite.config.
 */
const BASE = import.meta.env.BASE_URL.replace(/\/$/, '')

/**
 * Every call goes to the BFF on the same origin. The browser holds no Keycloak
 * token — only an httpOnly session cookie the BFF sets — so `credentials:
 * 'same-origin'` is what makes an authenticated request authenticated.
 */
async function json<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    credentials: 'same-origin',
    headers: { accept: 'application/json', ...(init?.headers ?? {}) },
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) {
    // The BFF forwards the upstream reason where it has one; registry and
    // oid4vc-service both return useful bodies and an opaque "500" would send
    // staff to the service logs for what is usually a validation mistake.
    let detail = (await res.text()).slice(0, 400)
    try {
      const parsed = JSON.parse(detail) as { message?: string; error?: string }
      detail = parsed.message || parsed.error || detail
    } catch {
      /* not JSON — use the raw text */
    }
    throw new Error(`${res.status} — ${detail}`)
  }
  return res.json() as Promise<T>
}

/** Thrown when the session has expired, so the UI can bounce to login. */
export class UnauthorizedError extends Error {
  constructor() {
    super('Session expired')
    this.name = 'UnauthorizedError'
  }
}

const body = (v: unknown): RequestInit => ({
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify(v),
})

// --- session ---------------------------------------------------------------

export const getSession = () => json<Session>('/api/session')

/** Full-page navigation, not fetch: the OAuth redirect must happen in the browser. */
export const loginUrl = () => `${BASE}/login`
export const logoutUrl = () => `${BASE}/logout`

// --- issuers ---------------------------------------------------------------

/** Every issuing authority, with holder counts and any unassigned holders. */
export const listIssuers = () =>
  json<{ issuers: Issuer[]; unassigned: number }>('/api/issuers')

export const getIssuer = (issuerId: string) =>
  json<Issuer>(`/api/issuers/${encodeURIComponent(issuerId)}`)

/** The server allocates `issuerId` from the name when one isn't supplied. */
export const createIssuer = (i: Partial<Issuer>) => json<Issuer>('/api/issuers', body(i))

export const updateIssuer = (issuerId: string, i: Partial<Issuer>) =>
  json<Issuer>(`/api/issuers/${encodeURIComponent(issuerId)}`, {
    ...body(i),
    method: 'PUT',
  })

export const deleteIssuer = (issuerId: string) =>
  json<{ deleted: true }>(`/api/issuers/${encodeURIComponent(issuerId)}`, { method: 'DELETE' })

// --- holders ---------------------------------------------------------------

/** Holders, scoped to one issuer. An empty `issuerId` lists every holder. */
export function listFarmers(
  search: string,
  offset: number,
  limit: number,
  issuerId?: string,
) {
  const q = new URLSearchParams({ offset: String(offset), limit: String(limit) })
  if (search.trim()) q.set('search', search.trim())
  if (issuerId) q.set('issuerId', issuerId)
  return json<{ farmers: Farmer[]; total: number }>(`/api/farmers?${q}`)
}

export const getFarmer = (farmerId: string) =>
  json<{
    farmer: Farmer
    land: LandParcel[]
    crops: Crop[]
    seeds: SeedDistribution[]
    qualifications: Qualification[]
    issuedCount: number
  }>(`/api/farmers/${encodeURIComponent(farmerId)}`)

export const createFarmer = (f: Farmer) => json<Farmer>('/api/farmers', body(f))

/**
 * The next free holder id for an issuer, using its own prefix.
 *
 * A suggestion, not a reservation: the registry's unique index is what guarantees
 * uniqueness, and the create path retries once if two staff are offered the same
 * id at the same moment.
 */
export const nextFarmerId = (issuerId: string) =>
  json<{ farmerId: string }>(
    `/api/farmers/next-id?issuerId=${encodeURIComponent(issuerId)}`,
  )

export const updateFarmer = (farmerId: string, f: Farmer) =>
  json<Farmer>(`/api/farmers/${encodeURIComponent(farmerId)}`, {
    ...body(f),
    method: 'PUT',
  })

export const deleteFarmer = (farmerId: string) =>
  json<{ deleted: true }>(`/api/farmers/${encodeURIComponent(farmerId)}`, { method: 'DELETE' })

/**
 * Links a farmer record to a Keycloak login by writing the `farmerId` user
 * attribute. That attribute is what wallet self-service issuance reads off the
 * presented access token, so without this link a citizen can log into a wallet
 * but has no record to be issued from.
 */
export const linkLogin = (
  farmerId: string,
  username: string,
  /** Create the Keycloak account first, with this temporary password. */
  create?: { password: string },
) =>
  json<{ keycloakSub: string; keycloakUsername: string }>(
    `/api/farmers/${encodeURIComponent(farmerId)}/link`,
    body(create ? { username, create: true, password: create.password } : { username }),
  )

/** Existing Keycloak usernames, so staff can pick rather than guess one. */
export const listKeycloakUsers = (search?: string) =>
  json<{ users: string[] }>(
    `/api/keycloak/users${search ? `?search=${encodeURIComponent(search)}` : ''}`,
  )

export const unlinkLogin = (farmerId: string) =>
  json<{ unlinked: true }>(`/api/farmers/${encodeURIComponent(farmerId)}/link`, {
    method: 'DELETE',
  })

// --- related entities ------------------------------------------------------

export const createRelated = (kind: EntityKind, record: Related) =>
  json<Related>(`/api/entities/${kind}`, body(record))

export const updateRelated = (kind: EntityKind, osid: string, record: Related) =>
  json<Related>(`/api/entities/${kind}/${encodeURIComponent(osid)}`, {
    ...body(record),
    method: 'PUT',
  })

export const deleteRelated = (kind: EntityKind, osid: string) =>
  json<{ deleted: true }>(`/api/entities/${kind}/${encodeURIComponent(osid)}`, {
    method: 'DELETE',
  })

// --- citizen self-service --------------------------------------------------

/**
 * The signed-in citizen's own record. Deliberately takes no farmer id: the
 * server resolves it from the session's token claim, so there is no parameter
 * here that could be pointed at somebody else.
 */
export const getMe = () =>
  json<{
    farmer: Farmer
    land: LandParcel[]
    crops: Crop[]
    seeds: SeedDistribution[]
    qualifications: Qualification[]
    issuedCount: number
  }>('/api/me')

/**
 * An offer for the citizen's own credential.
 *
 * Sends neither a holder id NOR a credential type: the server resolves both — the
 * holder from the session's token claim, the type from that holder's own issuer.
 * With several issuers on one deployment, a caller-chosen type would let a holder
 * of one authority obtain a credential attributed to another.
 */
export const createMyOffer = (recordRef?: string) =>
  json<Offer>('/api/me/offer', body({ recordRef }))

// --- credential types ------------------------------------------------------

/**
 * Issuable credential types, discovered live. Two filters, the same ones the
 * verifier console applies for the same reason: the format must be `vc+sd-jwt`,
 * and the author DID must be resolvable by a wallet. Most existing schemas are
 * authored by a `did:rcw` (resolvable only inside identity-service) or by a
 * `did:web` whose host no longer serves a DID document — offering those would
 * produce credentials no wallet can verify.
 */
export const listCredentialTypes = () =>
  json<{ types: CredentialType[]; hidden: number }>('/api/credential-types')

/**
 * Resolves a credential type's claims from the farmer's registry records, on the
 * server. Deliberately not computed in the browser: the same resolution runs
 * again at issuance time, and two implementations would drift.
 */
export const resolveClaims = (farmerId: string, configId: string, landRecordRef?: string) =>
  json<{ claims: Record<string, unknown>; sources: Record<string, string>; missing: string[] }>(
    '/api/claims/resolve',
    body({ farmerId, configId, landRecordRef }),
  )

// --- issuance --------------------------------------------------------------

/**
 * Creates the offer. The portal sends only the farmer and the type — never the
 * claims — so the credential's contents come from the registry and cannot be
 * edited on their way through the browser.
 */
export const createOffer = (
  farmerId: string,
  configId: string,
  landRecordRef?: string,
  /**
   * `authorization_code` sends the holder to Keycloak to sign in;
   * `pre-authorized_code` hands over a QR plus a one-time PIN.
   */
  grant: 'authorization_code' | 'pre-authorized_code' = 'pre-authorized_code',
) =>
  json<Offer>(
    '/api/offers',
    body({
      farmerId,
      configId,
      landRecordRef,
      grant,
      requirePin: grant === 'pre-authorized_code',
    }),
  )

/** Has a wallet collected this offer yet? */
export const getOfferStatus = (offerId: string) =>
  json<{ status: 'pending' | 'collected' | 'expired'; collectedAt?: string }>(
    `/api/offers/${encodeURIComponent(offerId)}/status`,
  )

/** QR image, rendered server-side so the portal carries no QR dependency. */
export const qrSrc = (data: string) => `${BASE}/api/qr?data=${encodeURIComponent(data)}`
