// Backend-for-frontend for the issuer portal.
//
// It exists for one hard reason: the Keycloak client is CONFIDENTIAL, so the
// code-for-token exchange needs a secret that must never reach a browser. Two
// useful consequences follow — the browser holds only an httpOnly session
// cookie and never a token, and every backend call is same-origin, so the CORS
// problem that bit the verifier console (registry and credential-schema send no
// access-control headers) does not arise.
//
// Zero runtime dependencies, deliberately: it is a redirect handler, a JSON
// proxy and a static file server, and a dependency tree would be a larger
// attack surface than the code it replaced.
//
// SCALING NOTE: sessions live in this process's memory, so more than one replica
// requires sticky sessions or a shared store. Single replica is the intended
// deployment; oid4vc-service uses redis for exactly this reason and this would
// follow the same route if it ever needed to scale.

import { createServer } from 'node:http'
import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto'
import { readFile, stat } from 'node:fs/promises'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadClaimSources, pickOne, resolveClaims } from './claim-mapping.mjs'
import * as mock from './mock-store.mjs'

const HERE = fileURLToPath(new URL('.', import.meta.url))
const DIST = join(HERE, '..', 'dist')

const MOCK = process.env.PORTAL_MOCK === '1'
const PORT = Number(process.env.PORT || 4100)
const BASE_PATH = (process.env.BASE_PATH || '/issuer-portal').replace(/\/$/, '')

const KC_PUBLIC = (process.env.KEYCLOAK_PUBLIC_URL || 'http://localhost:8080/auth').replace(/\/$/, '')
const KC_INTERNAL = (process.env.KEYCLOAK_INTERNAL_URL || KC_PUBLIC).replace(/\/$/, '')
const KC_REALM = process.env.KEYCLOAK_REALM || 'sunbird-rc'
const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || 'issuer-portal'
const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || ''
const PUBLIC_BASE = (process.env.PUBLIC_BASE_URL || `http://localhost:${PORT}`).replace(/\/$/, '')
const REDIRECT_URI = `${PUBLIC_BASE}${BASE_PATH}/callback`

const REGISTRY = (process.env.REGISTRY_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')
const OID4VC = (process.env.OID4VC_BASE_URL || 'http://localhost:3400').replace(/\/$/, '')
const SCHEMA = (process.env.SCHEMA_BASE_URL || 'http://localhost:3333').replace(/\/$/, '')

const SESSION_SECRET = process.env.SESSION_SECRET || (MOCK ? 'mock-secret' : '')
if (!MOCK && (!CLIENT_SECRET || !SESSION_SECRET)) {
  console.error('FATAL: KEYCLOAK_CLIENT_SECRET and SESSION_SECRET are required (or set PORTAL_MOCK=1)')
  process.exit(1)
}

const STAFF_ROLE = 'issuer-staff'
const CITIZEN_ROLE = 'citizen'
const ENTITY_KINDS = new Set(['LandParcel', 'Crop', 'SeedDistribution', 'Qualification'])

/** The claim carrying a citizen's registry key; must match oid4vc-service. */
const SUBJECT_CLAIM = process.env.KEYCLOAK_SUBJECT_CLAIM || 'farmerId'

/**
 * Which registry entities a credential's claims come from — the SAME declaration
 * oid4vc-service reads, so the portal's "what will be issued" table matches what
 * issuance actually produces.
 */
const CLAIM_SOURCES = loadClaimSources()

const isStaff = (session) => (session?.roles ?? []).includes(STAFF_ROLE)

/**
 * The signed-in user's registry key from the token, tolerant of naming style.
 *
 * The configured claim is tried first, then claims are matched with separators
 * stripped and case ignored. Realms in the wild spell this `farmer_id` while this
 * service defaults to `farmerId`, and an exact-only lookup shows a properly
 * linked farmer the "No access" screen — which reads as a permissions problem
 * rather than the naming mismatch it actually is.
 *
 * Mirrors findSubjectClaim in oid4vc-service's TokenService; both sides must
 * agree or a holder is linked in one place and unknown in the other.
 */
function subjectFromClaims(claims) {
  const direct = claims?.[SUBJECT_CLAIM]
  if (direct !== undefined && direct !== null && direct !== '') return String(direct)
  const norm = (v) => String(v).toLowerCase().replace(/[^a-z0-9]/g, '')
  const want = norm(SUBJECT_CLAIM)
  for (const [k, v] of Object.entries(claims ?? {})) {
    if (norm(k) === want && v !== undefined && v !== null && v !== '') return String(v)
  }
  return undefined
}


// --- sessions --------------------------------------------------------------

const sessions = new Map()
const pendingStates = new Map()

const sign = (v) => createHmac('sha256', SESSION_SECRET).update(v).digest('base64url')

function issueCookie(res, sid) {
  const value = `${sid}.${sign(sid)}`
  // `secure` is omitted on plain HTTP so local review works; behind the gateway
  // the deployment is HTTPS-only and the flag is added from PUBLIC_BASE's scheme.
  const secure = PUBLIC_BASE.startsWith('https://') ? '; Secure' : ''
  res.setHeader(
    'set-cookie',
    `portal_sid=${value}; Path=${BASE_PATH || '/'}; HttpOnly; SameSite=Lax${secure}`,
  )
}

function clearCookie(res) {
  res.setHeader('set-cookie', `portal_sid=; Path=${BASE_PATH || '/'}; HttpOnly; Max-Age=0`)
}

function readSession(req) {
  // Mock mode deliberately goes through the SAME cookie mechanism as the real
  // flow. Returning a session unconditionally here (the previous behaviour) made
  // sign-out a no-op: /logout cleared the cookie, the next /api/session reported
  // authenticated again, and the UI simply never left the signed-in state. Using
  // one code path means the logout button is exercised in mock mode too.
  const raw = /(?:^|;\s*)portal_sid=([^;]+)/.exec(req.headers.cookie || '')?.[1]
  if (!raw) return undefined
  const [sid, mac] = raw.split('.')
  if (!sid || !mac) return undefined
  // Constant-time compare: a fast-fail string compare on the MAC leaks it a byte
  // at a time to anyone who can measure the response.
  const expected = Buffer.from(sign(sid))
  const given = Buffer.from(mac)
  if (expected.length !== given.length || !timingSafeEqual(expected, given)) return undefined
  const s = sessions.get(sid)
  if (!s) return undefined
  if (s.expiresAt < Date.now()) {
    sessions.delete(sid)
    return undefined
  }
  return s
}

// --- helpers ---------------------------------------------------------------

function sendJson(res, status, body) {
  const payload = JSON.stringify(body)
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(payload),
    // This is an authenticated back-office API; never let a shared cache keep it.
    'cache-control': 'no-store',
  })
  res.end(payload)
}

const fail = (res, status, message) => sendJson(res, status, { message })

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = []
    let size = 0
    req.on('data', (c) => {
      size += c.length
      // A back-office form is kilobytes; anything larger is a mistake or an abuse.
      if (size > 256 * 1024) {
        reject(Object.assign(new Error('Request body too large'), { status: 413 }))
        req.destroy()
        return
      }
      chunks.push(c)
    })
    req.on('end', () => {
      const text = Buffer.concat(chunks).toString('utf8')
      if (!text) return resolve({})
      try {
        resolve(JSON.parse(text))
      } catch {
        reject(Object.assign(new Error('Body is not valid JSON'), { status: 400 }))
      }
    })
    req.on('error', reject)
  })
}

/** Calls a backend, forwarding the staff member's token so the backend authorises. */
async function upstream(session, base, path, init = {}) {
  const res = await fetch(`${base}${path}`, {
    ...init,
    headers: {
      accept: 'application/json',
      ...(init.body ? { 'content-type': 'application/json' } : {}),
      ...(session?.accessToken ? { authorization: `Bearer ${session.accessToken}` } : {}),
      ...(init.headers || {}),
    },
  })
  const text = await res.text()
  if (!res.ok) {
    // Surface the upstream reason. The registry and oid4vc-service both explain
    // validation failures usefully, and swallowing that turns a fixable mistake
    // into "500, check the logs".
    let message = text.slice(0, 500)
    try {
      const j = JSON.parse(text)
      message = j.message || j.error || j.params?.errmsg || message
    } catch {
      /* keep the raw text */
    }
    throw Object.assign(new Error(message || `${base}${path} -> ${res.status}`), {
      status: res.status,
    })
  }
  return text ? JSON.parse(text) : {}
}

// --- registry access -------------------------------------------------------

const REG_API = '/api/v1'

async function regSearch(session, entity, filters, offset = 0, limit = 200) {
  const body = { offset, limit, filters }
  const out = await upstream(session, REGISTRY, `${REG_API}/${entity}/search`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
  return Array.isArray(out) ? out : (out?.data ?? [])
}

// --- issuers ---------------------------------------------------------------
//
// An Issuer is the authority a credential comes from: its own name, its own
// did:web, and one bound credential type. Holders are attached to it by
// `issuerId`, so "select an issuer, then work on its holders" is a filter over
// one registry entity rather than a separate table per issuer — which is what
// keeps the wallet self-service chain (Keycloak attribute -> token claim ->
// registry lookup) working unchanged for every issuer.

/** Holder counts per issuer, so the issuer list can show real numbers. */
async function issuerHolderCounts(session, issuerIds) {
  const pairs = await Promise.all(
    issuerIds.map(async (id) => {
      // Failure to count must not fail the whole list: an issuer with an
      // uncountable holder set is still an issuer, and a blank count reads
      // better than a broken page.
      const rows = await regSearch(session, 'Farmer', { issuerId: { eq: id } }, 0, 1000).catch(
        () => undefined,
      )
      return [id, rows?.length]
    }),
  )
  return Object.fromEntries(pairs)
}

/** The issuer a holder record belongs to, or undefined if unassigned. */
async function issuerForHolder(session, holder) {
  if (!holder?.issuerId) return undefined
  const [issuer] = await regSearch(session, 'Issuer', { issuerId: { eq: holder.issuerId } }, 0, 1)
  return issuer
}

/**
 * The subset of an issuer a CITIZEN may see.
 *
 * Projected explicitly rather than returned wholesale: the issuer record is a
 * staff-scoped object, and a citizen needs only enough to recognise the authority
 * and see what it will issue them. Anything added to the Issuer schema later is
 * therefore private by default.
 */
function publicIssuer(issuer) {
  if (!issuer) return undefined
  return {
    issuerId: issuer.issuerId,
    name: issuer.name,
    description: issuer.description,
    holderLabel: issuer.holderLabel,
    credentialName: issuer.credentialName,
    recordEntities: issuer.recordEntities,
    // The authority's branding is exactly what a holder SHOULD see: it is how
    // they recognise who is issuing to them before they accept a credential.
    logoUrl: issuer.logoUrl,
    url: issuer.url,
    icon: issuer.icon,
    accent: issuer.accent,
    did: issuer.did,
  }
}

async function issuerByIdOr404(session, issuerId) {
  const [issuer] = await regSearch(session, 'Issuer', { issuerId: { eq: issuerId } }, 0, 1)
  if (!issuer) throw Object.assign(new Error(`No issuer with ID ${issuerId}`), { status: 404 })
  return issuer
}

/**
 * `ISS-` plus a slug of the name, uniquified.
 *
 * Derived from the name rather than a counter so the id stays readable in
 * registry queries and logs (`ISS-EDUCATION-BOARD`, not `ISS-000003`). The
 * registry's unique index is still the guarantee — this only avoids a collision
 * that the operator would otherwise have to resolve by hand.
 */
async function nextIssuerId(session, name) {
  const slug = slugify(name).toUpperCase().slice(0, 24).replace(/-+$/, '')
  const base = slug ? `ISS-${slug}` : 'ISS-NEW'
  const existing = new Set(
    (await regSearch(session, 'Issuer', {}, 0, 1000).catch(() => [])).map((i) => i.issuerId),
  )
  if (!existing.has(base)) return base
  for (let n = 2; n < 100; n++) if (!existing.has(`${base}-${n}`)) return `${base}-${n}`
  throw Object.assign(new Error('Could not allocate an issuer ID'), { status: 409 })
}

/**
 * The next free holder id for an issuer, e.g. FRM-000127.
 *
 * A CONVENIENCE, not the uniqueness guarantee — that remains the registry's
 * unique index on farmerId. Two staff creating a holder in the same second would
 * both be offered the same id, so the create path retries once with a freshly
 * computed one rather than losing a record to a race.
 *
 * Ids that do not match the issuer's own `PREFIX-digits` shape are ignored, so
 * hand-made or migrated values (FARM-0001, FRM-3000-0001) neither break the scan
 * nor drag the series somewhere strange.
 */
async function nextHolderId(session, issuer) {
  const prefix = (issuer?.holderIdPrefix || 'HLD').toUpperCase()
  const rows = MOCK
    ? mock.listFarmers('', 0, 1000).farmers
    : await regSearch(session, 'Farmer', {}, 0, 1000)
  const pattern = new RegExp(`^${prefix.replace(/[^A-Z0-9]/g, '')}-(\\d+)$`)
  let highest = 0
  let width = 6
  for (const r of rows) {
    const m = pattern.exec(String(r.farmerId ?? ''))
    if (!m) continue
    const n = Number(m[1])
    if (Number.isFinite(n) && n > highest) {
      highest = n
      // Follow the existing series' zero-padding rather than imposing six
      // digits on a registry that already numbers differently.
      width = m[1].length
    }
  }
  return `${prefix}-${String(highest + 1).padStart(width, '0')}`
}

async function farmerByIdOr404(session, farmerId) {
  const [farmer] = await regSearch(session, 'Farmer', { farmerId: { eq: farmerId } }, 0, 1)
  if (!farmer) throw Object.assign(new Error(`No farmer with ID ${farmerId}`), { status: 404 })
  return farmer
}

/**
 * One farmer with their child records. Shared by the staff route
 * (`/api/farmers/:id`) and the citizen route (`/api/me`) so the two cannot drift
 * into showing different things.
 */
async function farmerDetail(session, farmerId) {
  const farmer = await farmerByIdOr404(session, farmerId)
  const [land, crops, seeds, qualifications] = await Promise.all([
    regSearch(session, 'LandParcel', { farmerId: { eq: farmerId } }),
    regSearch(session, 'Crop', { farmerId: { eq: farmerId } }),
    regSearch(session, 'SeedDistribution', { farmerId: { eq: farmerId } }),
    // Tolerated as absent: a registry deployed before the Qualification entity
    // existed answers 4xx, and that must not break the holder screen for the
    // issuers that never use it.
    regSearch(session, 'Qualification', { farmerId: { eq: farmerId } }).catch(() => []),
  ])
  return { farmer, land, crops, seeds, qualifications, issuedCount: 0 }
}

/**
 * Creates an offer for one farmer, with claims resolved from the registry.
 *
 * Shared by the staff route (`/api/offers`, which names the farmer) and the
 * citizen route (`/api/me/offer`, where the farmer comes from the token). The
 * caller decides WHOSE record; this function never reads an identity from a
 * request body, so neither path can be talked into issuing someone else's data.
 */
async function buildOffer(
  session,
  farmerId,
  configId,
  landRecordRef,
  requirePin = true,
  grant = 'pre-authorized_code',
) {
  if (MOCK) return mock.createOffer(farmerId, configId, grant)

  const resolved = await resolveForSubject(session, farmerId, configId, landRecordRef)
  if (resolved.missing.length) {
    throw Object.assign(
      new Error(
        `Cannot issue: missing required ${resolved.missing.join(', ')}. ` +
          `The record must be completed first.`,
      ),
      { status: 422 },
    )
  }
  const offer = await upstream(session, OID4VC, '/oid4vc/offer', {
    method: 'POST',
    body: JSON.stringify({
      credential_configuration_id: configId,
      format: 'vc+sd-jwt',
      claims: resolved.claims,
      // A transaction code only applies to the pre-authorized grant; with
      // authorization_code the holder authenticates instead.
      tx_code_required: grant === 'pre-authorized_code' && requirePin !== false,
      grant,
    }),
  })
  return {
    offerId: offer.offer_id,
    qrData: offer.qr_data,
    credentialOfferUri: offer.credential_offer_uri,
    txCode: offer.tx_code,
  }
}

// --- Keycloak admin (used only to link a login) -----------------------------

let adminTokenCache = { token: undefined, expiresAt: 0 }

/**
 * A pseudo-session carrying the portal's own service token, for calls a
 * citizen's token cannot make.
 *
 * Needed because the registry (once `authentication_enabled` is on) refuses a
 * `citizen` token outright, yet a citizen is legitimately allowed to see their
 * OWN record. Authorization is still decided from the citizen's real token by the
 * route that calls this; the service token is only the credential used to fetch
 * the one record already deemed permissible. It is never chosen based on
 * anything in the request.
 */
async function asService() {
  // Mock mode has no Keycloak to mint a token from, and the mock store needs
  // none. Without this the citizen routes throw "fetch failed" in mock mode —
  // which made the one screen a citizen actually uses unreviewable offline.
  if (MOCK) return { roles: [STAFF_ROLE], service: true }
  return { accessToken: await kcAdminToken(), roles: [STAFF_ROLE], service: true }
}

async function kcAdminToken() {
  if (adminTokenCache.token && adminTokenCache.expiresAt > Date.now() + 5000) {
    return adminTokenCache.token
  }
  // The portal's own confidential client, via client_credentials — not an admin
  // username and password. It needs `manage-users` on realm-management, granted
  // by setup-keycloak-issuer.mjs.
  const res = await fetch(`${KC_INTERNAL}/realms/${KC_REALM}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
    }),
  })
  if (!res.ok) {
    throw Object.assign(
      new Error(
        'Could not obtain a Keycloak service token. The issuer-portal client needs ' +
          'serviceAccountsEnabled and the realm-management manage-users role.',
      ),
      { status: 502 },
    )
  }
  const j = await res.json()
  adminTokenCache = { token: j.access_token, expiresAt: Date.now() + (j.expires_in ?? 60) * 1000 }
  return j.access_token
}

/**
 * Like kcAdmin but returns the raw Response instead of throwing, so a caller can
 * distinguish "already exists" (409) from a real failure. Creating a user is the
 * one admin call where that difference matters to the operator.
 */
async function kcAdminRaw(method, path, body) {
  const token = await kcAdminToken()
  return fetch(`${KC_INTERNAL}/admin/realms/${KC_REALM}${path}`, {
    method,
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
}

async function kcAdmin(method, path, body) {
  const token = await kcAdminToken()
  const res = await fetch(`${KC_INTERNAL}/admin/realms/${KC_REALM}${path}`, {
    method,
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    throw Object.assign(new Error(`Keycloak ${method} ${path} -> ${res.status}`), { status: 502 })
  }
  const text = await res.text()
  return text ? JSON.parse(text) : {}
}

// --- credential types ------------------------------------------------------

/** Mirrors oid4vc-service's vct.util.ts slugifyVct(). */
const slugify = (name) =>
  String(name)
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

/**
 * Issuable types. Same two filters the verifier console applies, for the same
 * reason: the format must be vc+sd-jwt, and the authoring DID must be one a
 * WALLET can resolve. Schemas authored by a did:rcw resolve only inside
 * identity-service, and several did:web authors point at hosts that no longer
 * publish a DID document — issuing either produces credentials that verify here
 * and fail everywhere else.
 */
async function credentialTypes(session) {
  const raw = await upstream(session, SCHEMA, '/credential-schema/oid4vci-configs')
  const host = PUBLIC_BASE.replace(/^https?:\/\//, '')
  const resolvablePrefix = `did:web:${host}:`

  const sdJwt = raw.filter((c) => (c.formats ?? []).includes('vc+sd-jwt'))
  const usable = sdJwt.filter((c) => (c.author ?? '').startsWith(resolvablePrefix))

  const types = usable.map((c) => {
    const props = c.schema?.properties ?? {}
    const rawVct = c.vct || c.name
    return {
      id: c.schemaId,
      name: c.name,
      // Multi-format schemas expose <schemaId>_<format>; single-format use the
      // bare id (oid4vci.service.ts:81).
      configId: (c.formats ?? []).length > 1 ? `${c.schemaId}_vc+sd-jwt` : c.schemaId,
      vct: /^https?:\/\//i.test(rawVct) ? rawVct : `${PUBLIC_BASE}/vct/${slugify(rawVct)}`,
      issuer: c.author ?? '',
      attributes: Object.keys(props),
      required: c.schema?.required ?? [],
      descriptions: Object.fromEntries(
        Object.entries(props).map(([k, v]) => [k, v.description ?? '']),
      ),
      jsonTypes: Object.fromEntries(Object.entries(props).map(([k, v]) => [k, v.type ?? 'string'])),
    }
  })

  return { types, hidden: sdJwt.length - usable.length }
}

// --- claim resolution ------------------------------------------------------

/**
 * Which one of a holder's records a credential should describe.
 *
 * Staff may name a specific record (`recordRef`) — a holder with three land
 * parcels gets a credential about the one that was chosen, not the newest. Any
 * field may carry the reference, so this stays generic: the portal does not need
 * to know that a parcel is identified by `landRecordRef` and an award by
 * `enrolmentNumber`. Without a reference, the entity's configured sort decides,
 * exactly as it does for the wallet self-service path.
 */
function chooseRecord(rows, spec, recordRef) {
  if (recordRef) {
    const hit = rows.find((r) =>
      Object.values(r ?? {}).some((v) => v !== null && String(v) === String(recordRef)),
    )
    if (hit) return hit
  }
  return pickOne(rows, spec)
}

async function resolveForSubject(session, subjectId, configId, recordRef) {
  if (MOCK) {
    const detail = mock.getFarmer(subjectId)
    if (!detail) throw Object.assign(new Error('Holder not found'), { status: 404 })
    const { types } = mock.mockCredentialTypes()
    const type = types.find((t) => t.configId === configId)
    if (!type) throw Object.assign(new Error('Unknown credential type'), { status: 400 })
    return resolveClaims({
      attributes: type.attributes,
      required: type.required,
      sources: mock.claimSources(detail, recordRef),
      aliases: CLAIM_SOURCES.claimAliases,
      birthDateField: CLAIM_SOURCES.birthDateField,
    })
  }

  const { types } = await credentialTypes(session)
  const type = types.find((t) => t.configId === configId)
  if (!type) throw Object.assign(new Error('Unknown credential type'), { status: 400 })

  const { subjectEntity, subjectKey, related, claimAliases, birthDateField } = CLAIM_SOURCES
  if (!subjectEntity || !subjectKey) {
    throw Object.assign(
      new Error(
        'Claim sources are not configured: set REGISTRY_SUBJECT_ENTITY and ' +
          'REGISTRY_SUBJECT_KEY on this container (and the same values on ' +
          'oid4vc-service, which must agree).',
      ),
      { status: 500 },
    )
  }

  const [subject] = await regSearch(session, subjectEntity, { [subjectKey]: { eq: subjectId } }, 0, 1)
  if (!subject) {
    throw Object.assign(new Error(`No ${subjectEntity} with ${subjectKey} ${subjectId}`), {
      status: 404,
    })
  }

  const relatedRecords = await Promise.all(
    related.map((spec) =>
      regSearch(session, spec.entity, { [subjectKey]: { eq: subjectId } })
        // An entity this deployment has not created answers 4xx; that must not
        // break the claim preview for credentials which never needed it.
        .catch(() => [])
        .then((rows) => chooseRecord(rows, spec, recordRef)),
    ),
  )

  return resolveClaims({
    attributes: type.attributes,
    required: type.required,
    sources: [
      { entity: subjectEntity, record: subject },
      ...related.map((spec, i) => ({ entity: spec.entity, record: relatedRecords[i] })),
    ],
    aliases: claimAliases,
    birthDateField,
  })
}

// --- mock QR ---------------------------------------------------------------

/**
 * A deterministic pattern that LOOKS like a QR code, for mock mode only. It is
 * not scannable and is labelled as such — the alternative was either a real QR
 * dependency the production path doesn't need, or an empty box that makes the
 * layout impossible to review.
 */
function mockQrSvg(data) {
  const N = 25
  let h = 2166136261
  for (const ch of data) {
    h = (h ^ ch.charCodeAt(0)) * 16777619
    h >>>= 0
  }
  const bits = []
  for (let i = 0; i < N * N; i++) {
    h = (h * 1103515245 + 12345) >>> 0
    bits.push((h >>> 16) & 1)
  }
  // Finder patterns in three corners, so the shape reads as a QR at a glance.
  const finder = (ox, oy) => {
    let out = ''
    for (let y = 0; y < 7; y++) {
      for (let x = 0; x < 7; x++) {
        const edge = x === 0 || y === 0 || x === 6 || y === 6
        const core = x >= 2 && x <= 4 && y >= 2 && y <= 4
        if (edge || core) out += `<rect x="${ox + x}" y="${oy + y}" width="1" height="1"/>`
        bits[(oy + y) * N + ox + x] = 0
      }
    }
    return out
  }
  const corners = finder(0, 0) + finder(N - 7, 0) + finder(0, N - 7)
  let cells = ''
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      if (bits[y * N + x]) cells += `<rect x="${x}" y="${y}" width="1" height="1"/>`
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="-2 -2 ${N + 4} ${N + 7}" width="240" height="264">
<rect x="-2" y="-2" width="${N + 4}" height="${N + 7}" fill="#fff"/>
<g fill="#22333a">${corners}${cells}</g>
<text x="${N / 2}" y="${N + 3.6}" font-family="sans-serif" font-size="2.2" fill="#a4402c" text-anchor="middle" font-weight="bold">MOCK — NOT SCANNABLE</text>
</svg>`
}

// --- static files ----------------------------------------------------------

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
}

async function serveStatic(res, urlPath) {
  // `normalize` then a prefix check: without it, `..%2f..%2fetc/passwd` walks out
  // of the asset directory.
  const rel = normalize(urlPath).replace(/^(\.\.[/\\])+/, '')
  const file = join(DIST, rel)
  if (!file.startsWith(DIST)) return fail(res, 403, 'Forbidden')
  try {
    const s = await stat(file)
    if (!s.isFile()) throw new Error('not a file')
    const body = await readFile(file)
    const ext = extname(file)
    res.writeHead(200, {
      'content-type': MIME[ext] ?? 'application/octet-stream',
      'content-length': body.length,
      // Vite fingerprints assets, so they are immutable; index.html must not be.
      'cache-control': ext === '.html' ? 'no-store' : 'public, max-age=31536000, immutable',
    })
    res.end(body)
  } catch {
    // SPA fallback: any unknown path renders the app, which routes client-side.
    try {
      const html = await readFile(join(DIST, 'index.html'))
      res.writeHead(200, { 'content-type': MIME['.html'], 'cache-control': 'no-store' })
      res.end(html)
    } catch {
      fail(res, 404, 'Not built. Run `npm run build`, or use `npm run dev` for the dev server.')
    }
  }
}

// --- routes ----------------------------------------------------------------

async function handle(req, res, url) {
  const path = url.pathname.startsWith(BASE_PATH)
    ? url.pathname.slice(BASE_PATH.length) || '/'
    : url.pathname
  const method = req.method || 'GET'

  if (path === '/healthz') return sendJson(res, 200, { ok: true, mock: MOCK })

  // --- auth ---
  if (path === '/login') {
    if (MOCK) {
      // Mint a real session so the cookie path, and therefore logout, behaves
      // exactly as it does against Keycloak — just without the redirect.
      // `?as=citizen` / `?as=norole` switch roles, so both UI flows and the
      // no-access screen are reviewable offline.
      const as = url.searchParams.get('as') ?? 'staff'
      const sid = randomBytes(24).toString('base64url')
      sessions.set(sid, { ...mock.mockSession(as), expiresAt: Date.now() + 8 * 3600 * 1000 })
      issueCookie(res, sid)
      res.writeHead(302, { location: `${BASE_PATH}/` })
      return res.end()
    }
    const state = randomBytes(16).toString('base64url')
    pendingStates.set(state, Date.now() + 10 * 60 * 1000)
    const u = new URL(`${KC_PUBLIC}/realms/${KC_REALM}/protocol/openid-connect/auth`)
    u.searchParams.set('client_id', CLIENT_ID)
    u.searchParams.set('response_type', 'code')
    u.searchParams.set('scope', 'openid profile email')
    u.searchParams.set('redirect_uri', REDIRECT_URI)
    u.searchParams.set('state', state)
    res.writeHead(302, { location: u.toString() })
    return res.end()
  }

  if (path === '/callback') {
    const code = url.searchParams.get('code')
    const state = url.searchParams.get('state')
    // The state check is what makes this not a login-CSRF: without it an
    // attacker can complete a flow they started in the victim's browser.
    if (!state || !pendingStates.has(state) || pendingStates.get(state) < Date.now()) {
      return fail(res, 400, 'Login state missing or expired — please start again.')
    }
    pendingStates.delete(state)
    if (!code) return fail(res, 400, url.searchParams.get('error_description') || 'No code returned')

    const tokenRes = await fetch(`${KC_INTERNAL}/realms/${KC_REALM}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        redirect_uri: REDIRECT_URI,
        client_id: CLIENT_ID,
        client_secret: CLIENT_SECRET,
      }),
    })
    if (!tokenRes.ok) return fail(res, 502, `Token exchange failed: ${await tokenRes.text()}`)
    const tok = await tokenRes.json()

    // Identity and roles come from the access token itself, which already
    // carries preferred_username, name and realm_access.roles. A /userinfo call
    // would be a second network round trip that can fail independently — and
    // when it did, the session came back authenticated but nameless, which looks
    // like a login bug rather than a failed side call.
    //
    // These are for DISPLAY and for the no-issuing-role warning only. The
    // registry and oid4vc-service authorise against the same token themselves.
    let claims = {}
    try {
      claims = JSON.parse(Buffer.from(tok.access_token.split('.')[1], 'base64url').toString('utf8'))
    } catch {
      /* a token we cannot read is still usable upstream; fall back below */
    }

    const sid = randomBytes(24).toString('base64url')
    sessions.set(sid, {
      authenticated: true,
      username: claims.preferred_username ?? claims.sub ?? 'signed in',
      fullName: claims.name || claims.preferred_username || 'Signed in',
      roles: claims.realm_access?.roles ?? [],
      // The citizen's registry key, taken from the token claim written by the
      // portal's "link login" action. This is the ONLY source for it — every
      // self-scoped route reads it from here rather than from a request, which
      // is what stops one citizen requesting another's record.
      farmerId: subjectFromClaims(claims),
      accessToken: tok.access_token,
      // Kept solely as the `id_token_hint` for RP-initiated logout. Never sent
      // to the browser — /api/session projects an explicit allowlist.
      idToken: tok.id_token,
      expiresAt: Date.now() + (tok.expires_in ?? 300) * 1000,
    })
    issueCookie(res, sid)
    res.writeHead(302, { location: `${BASE_PATH}/` })
    return res.end()
  }

  if (path === '/logout') {
    const raw = /(?:^|;\s*)portal_sid=([^;]+)/.exec(req.headers.cookie || '')?.[1]
    const sid = raw ? raw.split('.')[0] : undefined
    // Read the id_token BEFORE dropping the session — it is the spec-correct
    // hint for RP-initiated logout on newer Keycloak.
    const idToken = sid ? sessions.get(sid)?.idToken : undefined
    if (sid) sessions.delete(sid)
    clearCookie(res)
    if (MOCK) {
      res.writeHead(302, { location: `${BASE_PATH}/` })
      return res.end()
    }

    const back = `${PUBLIC_BASE}${BASE_PATH}/`
    const u = new URL(`${KC_PUBLIC}/realms/${KC_REALM}/protocol/openid-connect/logout`)
    // Keycloak changed this parameter mid-life and the two forms are NOT
    // interchangeable: versions up to 17 accept `redirect_uri`, while 18+
    // implement RP-initiated logout and want `post_logout_redirect_uri` plus
    // either `client_id` or an `id_token_hint`. Sending only the newer form made
    // Keycloak 14 answer 400 — the local session was cleared, but the user
    // landed on a Keycloak error page, which reads as "logout is broken".
    //
    // All forms are sent; each version uses what it understands and ignores the
    // rest, so this works across the range without sniffing the version.
    u.searchParams.set('redirect_uri', back)
    u.searchParams.set('post_logout_redirect_uri', back)
    u.searchParams.set('client_id', CLIENT_ID)
    if (idToken) u.searchParams.set('id_token_hint', idToken)
    res.writeHead(302, { location: u.toString() })
    return res.end()
  }

  // --- API (everything below needs a session) ---
  if (path.startsWith('/api/')) {
    const session = readSession(req)
    if (path === '/api/session') {
      // Project explicitly. Returning the session object wholesale would ship
      // `accessToken` to the browser, defeating the entire reason this BFF
      // exists — the token must never leave the server.
      return sendJson(
        res,
        200,
        session
          ? {
              authenticated: true,
              username: session.username,
              fullName: session.fullName,
              roles: session.roles ?? [],
              // The UI needs to know a citizen is linked so it can show their
              // record instead of an "unlinked" notice. The VALUE is only a
              // display hint — every self-scoped route re-reads it from the
              // session server-side and never trusts a client-supplied id.
              ...(session.farmerId ? { farmerId: session.farmerId } : {}),
              ...(session.mock ? { mock: true } : {}),
            }
          : { authenticated: false },
      )
    }
    if (!session) return fail(res, 401, 'Not signed in')

    // Staff-only from here down, except the /api/me/* routes and the three
    // read-only helpers (credential types, offer status, QR) that carry no
    // per-farmer data. Before this gate existed, ANY signed-in account could
    // list every farmer, read another citizen's record, and create farmers.
    const STAFF_ONLY =
      path.startsWith('/api/farmers') ||
      path.startsWith('/api/issuers') ||
      path.startsWith('/api/entities/') ||
      path === '/api/claims/resolve' ||
      path === '/api/offers'
    if (STAFF_ONLY && !isStaff(session)) {
      return fail(
        res,
        403,
        `Requires the ${STAFF_ROLE} role. Your account can only view its own record.`,
      )
    }

    // --- citizen self-service (own record only) ---
    // `farmerId` comes from the session, which took it from the access token's
    // claim. It is never read from the path or body — that is what makes "cannot
    // reach another farmer" structural rather than a filter to be slipped past.
    if (path === '/api/me' && method === 'GET') {
      const farmerId = session.farmerId
      if (!farmerId) {
        return fail(
          res,
          403,
          'Your account is not linked to a farmer record yet. Contact the issuing authority.',
        )
      }
      if (MOCK) {
        const d = mock.getFarmer(farmerId)
        if (!d) return fail(res, 404, 'Your record was not found')
        return sendJson(res, 200, { ...d, issuer: publicIssuer(mock.getIssuer(d.farmer.issuerId)) })
      }
      // Service credential, not the citizen's token — see asService().
      const svc = await asService()
      const detail = await farmerDetail(svc, farmerId)
      return sendJson(res, 200, {
        ...detail,
        issuer: publicIssuer(await issuerForHolder(svc, detail.farmer)),
      })
    }

    if (path === '/api/me/offer' && method === 'POST') {
      const farmerId = session.farmerId
      if (!farmerId) {
        return fail(
          res,
          403,
          'Your account is not linked to a holder record yet. Contact the issuing authority.',
        )
      }
      // The body may name a specific supporting record, but NOT a holder and NOT
      // a credential type. The type comes from the holder's OWN issuer, resolved
      // here: with several issuers on one deployment, honouring a caller-supplied
      // configId would let a citizen of one authority obtain a credential that
      // appears to have been issued by another.
      const { landRecordRef, recordRef } = await readBody(req)
      const svc = await asService()
      const holder = MOCK ? mock.getFarmer(farmerId)?.farmer : await farmerByIdOr404(svc, farmerId)
      const issuer = MOCK
        ? mock.getIssuer(holder?.issuerId)
        : await issuerForHolder(svc, holder)
      if (!issuer) {
        return fail(
          res,
          409,
          'Your record is not assigned to an issuer, so there is nothing to issue. ' +
            'Contact the issuing authority.',
        )
      }
      if (!issuer.credentialConfigId) {
        return fail(
          res,
          409,
          `${issuer.name} has not published a credential type yet. Contact the issuing authority.`,
        )
      }
      // authorization_code, not a PIN. The holder signs into Keycloak from the
      // wallet, which is both stronger and simpler than a transaction code: a
      // PIN exists to substitute for authentication, and here the holder can
      // authenticate for real. It also removes a code they would otherwise have
      // to read off one screen and type into another on the same device.
      return sendJson(
        res,
        201,
        await buildOffer(
          svc,
          farmerId,
          issuer.credentialConfigId,
          recordRef ?? landRecordRef,
          false,
          'authorization_code',
        ),
      )
    }

    // Issuers
    if (path === '/api/issuers' && method === 'GET') {
      if (MOCK) return sendJson(res, 200, mock.listIssuers())
      const issuers = await regSearch(session, 'Issuer', {}, 0, 200)
      const counts = await issuerHolderCounts(
        session,
        issuers.map((i) => i.issuerId).filter(Boolean),
      )
      // Holders whose issuerId is unset or names a deleted issuer would otherwise
      // be invisible: nothing lists them, so nothing can fix them. Reported as a
      // count the UI surfaces as an "Unassigned" bucket.
      const all = await regSearch(session, 'Farmer', {}, 0, 1000).catch(() => [])
      const known = new Set(issuers.map((i) => i.issuerId))
      const unassigned = all.filter((f) => !f.issuerId || !known.has(f.issuerId)).length
      return sendJson(res, 200, {
        issuers: issuers.map((i) => ({ ...i, holderCount: counts[i.issuerId] })),
        unassigned,
      })
    }

    if (path === '/api/issuers' && method === 'POST') {
      const rec = await readBody(req)
      if (!rec.name?.trim()) return fail(res, 400, 'name is required')
      if (!rec.holderLabel?.trim()) return fail(res, 400, 'holderLabel is required')
      if (MOCK) return sendJson(res, 201, mock.createIssuer(rec))
      const issuerId = rec.issuerId?.trim() || (await nextIssuerId(session, rec.name))
      const body = { ...rec, issuerId }
      await upstream(session, REGISTRY, `${REG_API}/Issuer`, {
        method: 'POST',
        body: JSON.stringify(body),
      })
      return sendJson(res, 201, await issuerByIdOr404(session, issuerId))
    }

    const issuerMatch = /^\/api\/issuers\/([^/]+)$/.exec(path)
    if (issuerMatch) {
      const issuerId = decodeURIComponent(issuerMatch[1])

      if (method === 'GET') {
        if (MOCK) {
          const i = mock.getIssuer(issuerId)
          return i ? sendJson(res, 200, i) : fail(res, 404, 'Issuer not found')
        }
        return sendJson(res, 200, await issuerByIdOr404(session, issuerId))
      }

      if (method === 'PUT') {
        const rec = await readBody(req)
        if (MOCK) return sendJson(res, 200, mock.updateIssuer(issuerId, rec))
        const existing = await issuerByIdOr404(session, issuerId)
        await upstream(session, REGISTRY, `${REG_API}/Issuer/${existing.osid}`, {
          method: 'PUT',
          // issuerId stays fixed: it is the join key every holder record points
          // at, exactly as farmerId is for the child entities.
          body: JSON.stringify({ ...existing, ...rec, issuerId }),
        })
        return sendJson(res, 200, await issuerByIdOr404(session, issuerId))
      }

      if (method === 'DELETE') {
        if (MOCK) return sendJson(res, 200, mock.deleteIssuer(issuerId))
        const existing = await issuerByIdOr404(session, issuerId)
        // Refuse rather than orphan. Deleting an issuer out from under its
        // holders leaves records nothing lists and credentials whose stated
        // authority no longer exists in the registry.
        const holders = await regSearch(session, 'Farmer', { issuerId: { eq: issuerId } }, 0, 1)
        if (holders.length) {
          return fail(
            res,
            409,
            `${issuerId} still has holders. Move or delete them first — deleting the issuer ` +
              `would leave their records unlisted.`,
          )
        }
        await upstream(session, REGISTRY, `${REG_API}/Issuer/${existing.osid}`, {
          method: 'DELETE',
        })
        return sendJson(res, 200, { deleted: true })
      }
    }

    // Farmers
    if (path === '/api/farmers' && method === 'GET') {
      const search = url.searchParams.get('search') ?? ''
      const issuerId = url.searchParams.get('issuerId') ?? ''
      const offset = Number(url.searchParams.get('offset') ?? 0)
      const limit = Math.min(100, Number(url.searchParams.get('limit') ?? 20))
      if (MOCK) return sendJson(res, 200, mock.listFarmers(search, offset, limit, issuerId))

      // Scoping happens here, server-side. The issuer is a filter over one
      // entity, so a holder can only appear under the issuer their record names.
      const scope = issuerId ? { issuerId: { eq: issuerId } } : {}

      // The registry has no cross-field text search, so a search term is applied
      // to farmerId, name and district and the results merged. Without a term
      // this is a plain paged list.
      if (!search.trim()) {
        const rows = await regSearch(session, 'Farmer', scope, offset, limit)
        const all = await regSearch(session, 'Farmer', scope, 0, 1000)
        return sendJson(res, 200, { farmers: rows, total: all.length })
      }
      const term = search.trim()
      const groups = await Promise.all(
        ['farmerId', 'name', 'district'].map((f) =>
          regSearch(session, 'Farmer', { ...scope, [f]: { startsWith: term } }, 0, 200).catch(
            () => [],
          ),
        ),
      )
      const byId = new Map()
      for (const g of groups) for (const r of g) byId.set(r.farmerId, r)
      const merged = [...byId.values()]
      return sendJson(res, 200, {
        farmers: merged.slice(offset, offset + limit),
        total: merged.length,
      })
    }

    // Must precede the /api/farmers/:id matcher below, which would otherwise
    // read "next-id" as a farmer id and 404.
    if (path === '/api/farmers/next-id' && method === 'GET') {
      const issuerId = url.searchParams.get('issuerId') ?? ''
      const issuer = MOCK
        ? mock.getIssuer(issuerId)
        : issuerId
          ? await issuerByIdOr404(session, issuerId)
          : undefined
      return sendJson(res, 200, { farmerId: await nextHolderId(session, issuer) })
    }

    if (path === '/api/farmers' && method === 'POST') {
      const rec = await readBody(req)
      if (MOCK) return sendJson(res, 201, mock.createFarmer(rec))

      // Retry once on a duplicate id. The suggested id is computed by reading the
      // series, so two staff creating a holder at the same moment are offered the
      // same one; without this retry the second submission is simply rejected and
      // a filled-in form is lost to a race that had nothing to do with the
      // operator.
      const create = (body) =>
        upstream(session, REGISTRY, `${REG_API}/Farmer`, {
          method: 'POST',
          body: JSON.stringify(body),
        })
      try {
        await create(rec)
      } catch (e) {
        const duplicate = e.status === 400 || e.status === 409
        if (!duplicate || !rec.issuerId) throw e
        const issuer = await issuerByIdOr404(session, rec.issuerId).catch(() => undefined)
        const retryId = await nextHolderId(session, issuer)
        if (retryId === rec.farmerId) throw e
        rec.farmerId = retryId
        await create(rec)
      }
      return sendJson(res, 201, await farmerByIdOr404(session, rec.farmerId))
    }

    const farmerMatch = /^\/api\/farmers\/([^/]+)(\/link)?$/.exec(path)
    if (farmerMatch) {
      const farmerId = decodeURIComponent(farmerMatch[1])
      const isLink = Boolean(farmerMatch[2])

      if (!isLink && method === 'GET') {
        if (MOCK) {
          const d = mock.getFarmer(farmerId)
          return d ? sendJson(res, 200, d) : fail(res, 404, 'Farmer not found')
        }
        return sendJson(res, 200, await farmerDetail(session, farmerId))
      }

      if (!isLink && method === 'PUT') {
        const rec = await readBody(req)
        if (MOCK) return sendJson(res, 200, mock.updateFarmer(farmerId, rec))
        const existing = await farmerByIdOr404(session, farmerId)
        await upstream(session, REGISTRY, `${REG_API}/Farmer/${existing.osid}`, {
          method: 'PUT',
          body: JSON.stringify({ ...rec, farmerId }),
        })
        return sendJson(res, 200, await farmerByIdOr404(session, farmerId))
      }

      if (!isLink && method === 'DELETE') {
        if (MOCK) return sendJson(res, 200, mock.deleteFarmer(farmerId))
        const existing = await farmerByIdOr404(session, farmerId)
        await upstream(session, REGISTRY, `${REG_API}/Farmer/${existing.osid}`, { method: 'DELETE' })
        return sendJson(res, 200, { deleted: true })
      }

      if (isLink && method === 'POST') {
        const { username, create, password } = await readBody(req)
        if (!username) return fail(res, 400, 'username is required')
        if (MOCK) return sendJson(res, 200, mock.linkLogin(farmerId, username))

        let users = await kcAdmin(
          'GET',
          `/users?username=${encodeURIComponent(username)}&exact=true`,
        )

        // Create the account on request. Without this, linking is a dead end for
        // any farmer who has no login yet: staff get a bare 404 and no way
        // forward, because account creation lives in the Keycloak admin console
        // they may not have access to.
        if (!users.length && create) {
          if (!password) return fail(res, 400, 'password is required to create a login')
          const created = await kcAdminRaw('POST', '/users', {
            username,
            enabled: true,
            // Not emailVerified and no email: this is a wallet login, not a
            // contactable identity, and inventing an address would be worse.
            attributes: { [SUBJECT_CLAIM]: [farmerId] },
            credentials: [{ type: 'password', value: password, temporary: true }],
          })
          if (created.status === 409) {
            return fail(res, 409, `A Keycloak user named ${username} already exists`)
          }
          if (!created.ok) {
            return fail(res, 502, `Could not create the login (Keycloak ${created.status})`)
          }
          users = await kcAdmin('GET', `/users?username=${encodeURIComponent(username)}&exact=true`)
          if (!users.length) return fail(res, 502, 'Login created but could not be read back')

          // The citizen role is what lets them see their own record in this
          // portal; without it they land on the no-access screen.
          try {
            const role = await kcAdmin('GET', `/roles/${encodeURIComponent(CITIZEN_ROLE)}`)
            await kcAdminRaw('POST', `/users/${users[0].id}/role-mappings/realm`, [role])
          } catch {
            // Non-fatal: issuance depends on the farmerId claim, not the role.
            console.warn(`Could not grant ${CITIZEN_ROLE} to ${username}`)
          }
        }

        if (!users.length) {
          return fail(
            res,
            404,
            `No Keycloak user named ${username}. Tick "create this login" to make one.`,
          )
        }
        const user = users[0]

        // Refuse to steal an account that already belongs to a different farmer.
        // Without this, linking an existing username silently repoints its
        // farmerId attribute: two farmer records then claim the same login, and
        // the holder would be issued the OTHER person's credential.
        const alreadyLinkedTo = user.attributes?.[SUBJECT_CLAIM]?.[0]
        if (alreadyLinkedTo && alreadyLinkedTo !== farmerId) {
          return fail(
            res,
            409,
            `${username} is already linked to ${alreadyLinkedTo}. Unlink it there first, ` +
              `or use a different account.`,
          )
        }

        // Writing farmerId onto the Keycloak user is what makes wallet
        // self-service work: it travels in the access token, so oid4vc-service
        // resolves the record without an admin lookup per request.
        await kcAdmin('PUT', `/users/${user.id}`, {
          ...user,
          attributes: { ...(user.attributes ?? {}), [SUBJECT_CLAIM]: [farmerId] },
        })
        const existing = await farmerByIdOr404(session, farmerId)
        await upstream(session, REGISTRY, `${REG_API}/Farmer/${existing.osid}`, {
          method: 'PUT',
          body: JSON.stringify({
            ...existing,
            keycloakSub: user.id,
            keycloakUsername: username,
          }),
        })
        return sendJson(res, 200, { keycloakSub: user.id, keycloakUsername: username })
      }

      if (isLink && method === 'DELETE') {
        if (MOCK) return sendJson(res, 200, mock.unlinkLogin(farmerId))
        const existing = await farmerByIdOr404(session, farmerId)
        if (existing.keycloakSub) {
          const user = await kcAdmin('GET', `/users/${existing.keycloakSub}`)
          const attributes = { ...(user.attributes ?? {}) }
          delete attributes.farmerId
          await kcAdmin('PUT', `/users/${user.id}`, { ...user, attributes })
        }
        const cleared = { ...existing }
        delete cleared.keycloakSub
        delete cleared.keycloakUsername
        await upstream(session, REGISTRY, `${REG_API}/Farmer/${existing.osid}`, {
          method: 'PUT',
          body: JSON.stringify(cleared),
        })
        return sendJson(res, 200, { unlinked: true })
      }
    }

    // Child entities
    const entityMatch = /^\/api\/entities\/([A-Za-z]+)(?:\/([^/]+))?$/.exec(path)
    if (entityMatch) {
      const kind = entityMatch[1]
      const osid = entityMatch[2] ? decodeURIComponent(entityMatch[2]) : undefined
      // Allow-list, not pass-through: the kind lands in an upstream URL path.
      if (!ENTITY_KINDS.has(kind)) return fail(res, 404, `Unknown entity ${kind}`)

      if (method === 'POST') {
        const rec = await readBody(req)
        if (MOCK) return sendJson(res, 201, mock.createRelated(kind, rec))
        await upstream(session, REGISTRY, `${REG_API}/${kind}`, {
          method: 'POST',
          body: JSON.stringify(rec),
        })
        return sendJson(res, 201, rec)
      }
      if (method === 'PUT' && osid) {
        const rec = await readBody(req)
        if (MOCK) return sendJson(res, 200, mock.updateRelated(kind, osid, rec))
        await upstream(session, REGISTRY, `${REG_API}/${kind}/${encodeURIComponent(osid)}`, {
          method: 'PUT',
          body: JSON.stringify(rec),
        })
        return sendJson(res, 200, { ...rec, osid })
      }
      if (method === 'DELETE' && osid) {
        if (MOCK) return sendJson(res, 200, mock.deleteRelated(kind, osid))
        await upstream(session, REGISTRY, `${REG_API}/${kind}/${encodeURIComponent(osid)}`, {
          method: 'DELETE',
        })
        return sendJson(res, 200, { deleted: true })
      }
    }

    // Keycloak user lookup, so staff can pick an existing login instead of
    // guessing a username and getting a 404. Returns usernames only — no
    // emails, ids or attributes, since the UI needs nothing else and this is a
    // list of real people.
    if (path === '/api/keycloak/users' && method === 'GET') {
      if (!isStaff(session)) return fail(res, 403, `Requires the ${STAFF_ROLE} role`)
      if (MOCK) {
        return sendJson(res, 200, {
          users: ['farmer.ravi', 'farmer.lakshmi', 'farmer.norecord', 'issuer.staff'],
        })
      }
      const q = (url.searchParams.get('search') ?? '').trim()
      const list = await kcAdmin(
        'GET',
        `/users?max=20${q ? `&search=${encodeURIComponent(q)}` : ''}`,
      )
      return sendJson(res, 200, { users: list.map((u) => u.username).filter(Boolean) })
    }

    // Credential types
    if (path === '/api/credential-types' && method === 'GET') {
      return sendJson(res, 200, MOCK ? mock.mockCredentialTypes() : await credentialTypes(session))
    }

    // Claim resolution
    if (path === '/api/claims/resolve' && method === 'POST') {
      const { farmerId, configId, landRecordRef } = await readBody(req)
      if (!farmerId || !configId) return fail(res, 400, 'farmerId and configId are required')
      return sendJson(res, 200, await resolveForSubject(session, farmerId, configId, landRecordRef))
    }

    // Offers
    if (path === '/api/offers' && method === 'POST') {
      // Staff-only (gated above), so naming the farmer here is legitimate.
      const { farmerId, configId, landRecordRef, requirePin, grant } = await readBody(req)
      if (!farmerId || !configId) return fail(res, 400, 'farmerId and configId are required')
      if (grant && grant !== 'pre-authorized_code' && grant !== 'authorization_code') {
        return fail(res, 400, `unknown grant '${grant}'`)
      }
      return sendJson(
        res,
        201,
        await buildOffer(session, farmerId, configId, landRecordRef, requirePin, grant),
      )
    }

    const statusMatch = /^\/api\/offers\/([^/]+)\/status$/.exec(path)
    if (statusMatch && method === 'GET') {
      const offerId = decodeURIComponent(statusMatch[1])
      if (MOCK) return sendJson(res, 200, mock.offerStatus(offerId))
      try {
        // A live offer still dereferences; once collected or expired it 404s.
        await upstream(session, OID4VC, `/oid4vc/offer/${encodeURIComponent(offerId)}`)
        return sendJson(res, 200, { status: 'pending' })
      } catch (e) {
        // Collected and expired are indistinguishable from here: the offer
        // session is gone either way. The portal's own countdown decides which
        // it shows, so this stays honest rather than guessing.
        return sendJson(res, 200, { status: e.status === 404 ? 'collected' : 'pending' })
      }
    }

    // QR
    if (path === '/api/qr' && method === 'GET') {
      const data = url.searchParams.get('data') ?? ''
      if (!data) return fail(res, 400, 'data is required')
      if (MOCK) {
        const svg = mockQrSvg(data)
        res.writeHead(200, { 'content-type': 'image/svg+xml', 'cache-control': 'no-store' })
        return res.end(svg)
      }
      const upstreamRes = await fetch(`${OID4VC}/qr?data=${encodeURIComponent(data)}`)
      const buf = Buffer.from(await upstreamRes.arrayBuffer())
      res.writeHead(upstreamRes.status, {
        'content-type': upstreamRes.headers.get('content-type') ?? 'image/png',
        'content-length': buf.length,
        'cache-control': 'no-store',
      })
      return res.end(buf)
    }

    return fail(res, 404, `No such endpoint: ${method} ${path}`)
  }

  // --- static ---
  if (method !== 'GET' && method !== 'HEAD') return fail(res, 405, 'Method not allowed')
  return serveStatic(res, path === '/' ? 'index.html' : path)
}

const server = createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://${req.headers.host ?? 'localhost'}`)
  handle(req, res, url).catch((e) => {
    const status = e.status && e.status >= 400 && e.status < 600 ? e.status : 500
    if (status >= 500) console.error(`${req.method} ${url.pathname} ->`, e.message)
    if (!res.headersSent) fail(res, status, e.message || 'Internal error')
    else res.end()
  })
})

// Expire stale sessions and login states rather than leaking them for the
// lifetime of the process.
setInterval(() => {
  const now = Date.now()
  for (const [sid, s] of sessions) if (s.expiresAt < now) sessions.delete(sid)
  for (const [st, exp] of pendingStates) if (exp < now) pendingStates.delete(st)
}, 60_000).unref()

server.listen(PORT, '0.0.0.0', () => {
  console.log(`issuer-portal BFF on http://localhost:${PORT}${BASE_PATH}/`)
  if (MOCK) {
    console.log('  PORTAL_MOCK=1 — sample data, no Keycloak/registry/oid4vc-service, QR not scannable')
  } else {
    console.log(`  keycloak: ${KC_PUBLIC} (realm ${KC_REALM}, client ${CLIENT_ID})`)
    console.log(`  registry: ${REGISTRY}  oid4vc: ${OID4VC}  schema: ${SCHEMA}`)
    console.log(`  redirect_uri: ${REDIRECT_URI}`)
  }
})
