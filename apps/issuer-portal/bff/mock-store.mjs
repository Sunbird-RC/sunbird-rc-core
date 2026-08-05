// In-memory stand-in for the registry, Keycloak and oid4vc-service, used ONLY
// when PORTAL_MOCK=1.
//
// It exists so the portal's UI can be reviewed and demoed without standing up
// four backing services, and so the front end has something to develop against
// offline. It is not a test double for correctness work: it does not validate
// like the registry, does not sign anything, and its QR codes are not scannable.
// The banner in the UI says so, deliberately and unmissably.

// `undefined` has to short-circuit: JSON.stringify(undefined) returns the value
// undefined rather than a string, and JSON.parse then stringifies it to the
// literal "undefined" and throws. A holder with no crop records hits this.
const clone = (v) => (v === undefined ? undefined : JSON.parse(JSON.stringify(v)))

let seq = 100

const state = {
  issuers: [
    {
      osid: 'mock-i1',
      issuerId: 'ISS-FARMER',
      name: 'Department of Agriculture',
      description:
        'Issues land-holding credentials to registered farmers, from the land and crop records held in the registry.',
      category: 'Agriculture',
      did: 'did:web:example.test:mock-agriculture',
      credentialConfigId: 'did:schema:mock-farmer',
      credentialName: 'Farmer Land Holding Credential',
      holderLabel: 'Farmer',
      holderIdPrefix: 'FRM',
      recordEntities: 'LandParcel,Crop,SeedDistribution',
      logoUrl: '/issuer-portal/logos/agriculture.svg',
      url: 'https://agricoop.gov.in',
      icon: '🌾',
      accent: '#4a7c59',
      status: 'Active',
    },
    {
      osid: 'mock-i2',
      issuerId: 'ISS-AGE',
      name: 'Civil Registration Authority',
      description:
        'Issues a minimal age credential — the holder proves they are over 18 without revealing a date of birth.',
      category: 'Identity',
      did: 'did:web:example.test:mock-civil',
      credentialConfigId: 'did:schema:mock-age',
      credentialName: 'Mobile Age Credential',
      holderLabel: 'Citizen',
      holderIdPrefix: 'AGE',
      recordEntities: '',
      logoUrl: '/issuer-portal/logos/civil-registration.svg',
      url: 'https://crsorgi.gov.in',
      icon: '🪪',
      accent: '#3d6b8c',
      status: 'Active',
    },
    {
      osid: 'mock-i3',
      issuerId: 'ISS-EDUCATION',
      name: 'State Board of Education',
      description: 'Issues qualification credentials to students from the awards on their record.',
      category: 'Education',
      did: 'did:web:example.test:mock-education',
      credentialConfigId: 'did:schema:mock-education',
      credentialName: 'Education Certificate Credential',
      holderLabel: 'Student',
      holderIdPrefix: 'EDU',
      recordEntities: 'Qualification',
      logoUrl: '/issuer-portal/logos/education.svg',
      url: 'https://education.gov.in',
      icon: '🎓',
      accent: '#8c5a3d',
      status: 'Active',
    },
  ],
  farmers: [
    {
      osid: 'mock-f1',
      issuerId: 'ISS-FARMER',
      farmerId: 'FRM-000123',
      name: 'Ravi Kumar',
      gender: 'Male',
      dateOfBirth: '1979-04-12',
      mobile: '+91 98450 11223',
      district: 'Mandya',
      state: 'Karnataka',
      keycloakSub: 'mock-sub-ravi',
      keycloakUsername: 'farmer.ravi',
    },
    {
      osid: 'mock-f2',
      issuerId: 'ISS-FARMER',
      farmerId: 'FRM-000124',
      name: 'Lakshmi Devi',
      gender: 'Female',
      dateOfBirth: '1986-11-03',
      mobile: '+91 99001 44556',
      district: 'Hassan',
      state: 'Karnataka',
      keycloakSub: 'mock-sub-lakshmi',
      keycloakUsername: 'farmer.lakshmi',
    },
    {
      osid: 'mock-f3',
      issuerId: 'ISS-FARMER',
      farmerId: 'FRM-000125',
      name: 'Anand Patil',
      gender: 'Male',
      dateOfBirth: '1991-07-21',
      district: 'Belagavi',
      state: 'Karnataka',
    },
    {
      osid: 'mock-f4',
      issuerId: 'ISS-FARMER',
      farmerId: 'FRM-000126',
      name: 'Sunita Rao',
      gender: 'Female',
      district: 'Mandya',
      state: 'Karnataka',
    },
    // Age issuer: a citizen record needs nothing but a name and a date of birth,
    // which is the point of that credential.
    {
      osid: 'mock-f5',
      issuerId: 'ISS-AGE',
      farmerId: 'AGE-000001',
      name: 'Meera Nair',
      gender: 'Female',
      dateOfBirth: '2001-03-19',
      district: 'Ernakulam',
      state: 'Kerala',
      keycloakSub: 'mock-sub-meera',
      keycloakUsername: 'citizen.meera',
    },
    // Deliberately a minor: age_over_18 resolves to false, which must still be a
    // claim rather than a missing value.
    {
      osid: 'mock-f6',
      issuerId: 'ISS-AGE',
      farmerId: 'AGE-000002',
      name: 'Arjun Das',
      gender: 'Male',
      dateOfBirth: '2012-08-30',
      district: 'Kozhikode',
      state: 'Kerala',
    },
    {
      osid: 'mock-f7',
      issuerId: 'ISS-EDUCATION',
      farmerId: 'EDU-000001',
      name: 'Priya Sharma',
      gender: 'Female',
      dateOfBirth: '1998-06-05',
      district: 'Bengaluru Urban',
      state: 'Karnataka',
      keycloakSub: 'mock-sub-priya',
      keycloakUsername: 'student.priya',
    },
    // No qualification on purpose: exercises the blocked-issuance path for the
    // Education issuer, the same way FRM-000125 does for land.
    {
      osid: 'mock-f8',
      issuerId: 'ISS-EDUCATION',
      farmerId: 'EDU-000002',
      name: 'Rahul Verma',
      gender: 'Male',
      district: 'Mysuru',
      state: 'Karnataka',
    },
  ],
  LandParcel: [
    {
      osid: 'mock-l1',
      farmerId: 'FRM-000123',
      landRecordRef: 'LR-KA-77-2201',
      farmLocation: 'Rampur, Mandya',
      landAreaAcres: 4.5,
      ownershipType: 'Owned',
      surveyedOn: '2024-06-01',
    },
    {
      osid: 'mock-l2',
      farmerId: 'FRM-000123',
      landRecordRef: 'LR-KA-77-2202',
      farmLocation: 'Rampur, Mandya',
      landAreaAcres: 1.75,
      ownershipType: 'Leased',
    },
    {
      osid: 'mock-l3',
      farmerId: 'FRM-000124',
      landRecordRef: 'LR-KA-88-3302',
      farmLocation: 'Shivpur, Hassan',
      landAreaAcres: 2,
      ownershipType: 'Leased',
    },
    // FRM-000125 deliberately has no parcel: exercises the blocked-issuance path.
  ],
  Crop: [
    {
      osid: 'mock-c1',
      farmerId: 'FRM-000123',
      landRecordRef: 'LR-KA-77-2201',
      cropName: 'Wheat',
      season: 'Rabi',
      year: 2026,
      areaSownAcres: 3.5,
      expectedYieldQuintals: 42,
    },
    {
      osid: 'mock-c2',
      farmerId: 'FRM-000123',
      cropName: 'Paddy',
      season: 'Kharif',
      year: 2025,
      areaSownAcres: 4,
    },
    {
      osid: 'mock-c3',
      farmerId: 'FRM-000124',
      cropName: 'Ragi',
      season: 'Kharif',
      year: 2026,
      areaSownAcres: 2,
    },
  ],
  SeedDistribution: [
    {
      osid: 'mock-s1',
      farmerId: 'FRM-000123',
      seedType: 'Wheat',
      variety: 'HD-2967',
      quantityKg: 40,
      issuedOn: '2026-10-12',
      subsidyScheme: 'NFSM',
      distributionCentre: 'Mandya Taluk Centre',
    },
    {
      osid: 'mock-s2',
      farmerId: 'FRM-000124',
      seedType: 'Ragi',
      quantityKg: 12,
      issuedOn: '2026-06-04',
    },
  ],
  Qualification: [
    {
      osid: 'mock-q1',
      farmerId: 'EDU-000001',
      degree: 'B.Sc. Agriculture',
      institution: 'University of Agricultural Sciences, Bengaluru',
      yearOfPassing: 2020,
      grade: 'First Class',
      enrolmentNumber: 'UAS-2016-4471',
    },
    {
      // An earlier award, so the newest-first sort is observable.
      osid: 'mock-q2',
      farmerId: 'EDU-000001',
      degree: 'Higher Secondary',
      institution: 'Karnataka State Board',
      yearOfPassing: 2016,
      grade: '88%',
    },
  ],
  issued: { 'FRM-000124': 1 },
  offers: new Map(),
}

/** Child entities, in the order the generic tables and claim sources use. */
const CHILD_KINDS = ['LandParcel', 'Crop', 'SeedDistribution', 'Qualification']

export const mockCredentialTypes = () => ({
  types: [
    {
      id: 'did:schema:mock-farmer',
      name: 'Farmer Land Holding Credential',
      configId: 'did:schema:mock-farmer',
      vct: 'https://example.test/vct/farmer-land-holding-credential',
      issuer: 'did:web:example.test:mock-agriculture',
      attributes: [
        'farmerId',
        'name',
        'gender',
        'landRecordRef',
        'farmLocation',
        'landAreaAcres',
        'ownershipType',
        'primaryCrop',
      ],
      required: ['farmerId', 'name', 'landAreaAcres'],
      descriptions: {},
      jsonTypes: {},
    },
    {
      id: 'did:schema:mock-age',
      name: 'Mobile Age Credential',
      configId: 'did:schema:mock-age',
      vct: 'https://example.test/vct/mobile-age-credential',
      issuer: 'did:web:example.test:mock-civil',
      attributes: ['name', 'birthdate', 'age_over_18'],
      required: ['name', 'birthdate'],
      descriptions: {},
      jsonTypes: {},
    },
    {
      id: 'did:schema:mock-education',
      name: 'Education Certificate Credential',
      configId: 'did:schema:mock-education',
      vct: 'https://example.test/vct/education-certificate-credential',
      issuer: 'did:web:example.test:mock-education',
      attributes: ['name', 'degree', 'institution', 'yearOfPassing', 'grade'],
      required: ['name', 'degree', 'institution'],
      descriptions: {},
      jsonTypes: {},
    },
  ],
  hidden: 3,
})

// --- issuers ---------------------------------------------------------------

const holderCount = (issuerId) => state.farmers.filter((f) => f.issuerId === issuerId).length

export function listIssuers() {
  const known = new Set(state.issuers.map((i) => i.issuerId))
  return {
    issuers: clone(
      state.issuers.map((i) => ({ ...i, holderCount: holderCount(i.issuerId) })),
    ),
    unassigned: state.farmers.filter((f) => !f.issuerId || !known.has(f.issuerId)).length,
  }
}

export function getIssuer(issuerId) {
  return clone(state.issuers.find((i) => i.issuerId === issuerId))
}

export function createIssuer(rec) {
  const slug = String(rec.name || 'new')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 24)
  let issuerId = rec.issuerId?.trim() || `ISS-${slug || 'NEW'}`
  if (state.issuers.some((i) => i.issuerId === issuerId)) issuerId = `${issuerId}-${++seq}`
  const created = { osid: `mock-${++seq}`, ...rec, issuerId }
  state.issuers.push(created)
  return clone(created)
}

export function updateIssuer(issuerId, rec) {
  const i = state.issuers.findIndex((x) => x.issuerId === issuerId)
  if (i < 0) throw Object.assign(new Error('Issuer not found'), { status: 404 })
  state.issuers[i] = { ...state.issuers[i], ...rec, issuerId }
  return clone(state.issuers[i])
}

export function deleteIssuer(issuerId) {
  const i = state.issuers.findIndex((x) => x.issuerId === issuerId)
  if (i < 0) throw Object.assign(new Error('Issuer not found'), { status: 404 })
  // Same refusal as the real path: deleting an issuer out from under its holders
  // would leave records nothing lists.
  if (holderCount(issuerId) > 0) {
    throw Object.assign(
      new Error(`${issuerId} still has holders. Move or delete them first.`),
      { status: 409 },
    )
  }
  state.issuers.splice(i, 1)
  return { deleted: true }
}

// --- holders ---------------------------------------------------------------

export function listFarmers(search, offset, limit, issuerId) {
  const q = (search || '').trim().toLowerCase()
  const all = state.farmers
    .filter((f) => !issuerId || f.issuerId === issuerId)
    .filter(
      (f) =>
        !q ||
        f.farmerId.toLowerCase().includes(q) ||
        f.name.toLowerCase().includes(q) ||
        (f.district || '').toLowerCase().includes(q),
    )
    .map((f) => ({
      ...f,
      landCount: state.LandParcel.filter((l) => l.farmerId === f.farmerId).length,
    }))
  return { farmers: clone(all.slice(offset, offset + limit)), total: all.length }
}

export function getFarmer(farmerId) {
  const farmer = state.farmers.find((f) => f.farmerId === farmerId)
  if (!farmer) return undefined
  const by = (k) => state[k].filter((r) => r.farmerId === farmerId)
  return clone({
    farmer,
    land: by('LandParcel'),
    crops: by('Crop'),
    seeds: by('SeedDistribution'),
    qualifications: by('Qualification'),
    issuedCount: state.issued[farmerId] ?? 0,
  })
}

export function createFarmer(rec) {
  if (state.farmers.some((f) => f.farmerId === rec.farmerId)) {
    throw Object.assign(new Error(`ID ${rec.farmerId} already exists`), { status: 409 })
  }
  const created = { osid: `mock-${++seq}`, ...rec }
  state.farmers.push(created)
  return clone(created)
}

export function updateFarmer(farmerId, rec) {
  const i = state.farmers.findIndex((f) => f.farmerId === farmerId)
  if (i < 0) throw Object.assign(new Error('Holder not found'), { status: 404 })
  // farmerId is the join key for every child record, so it stays immutable here
  // exactly as the registry's unique index makes it immutable in real life.
  state.farmers[i] = { ...state.farmers[i], ...rec, farmerId }
  return clone(state.farmers[i])
}

export function deleteFarmer(farmerId) {
  const i = state.farmers.findIndex((f) => f.farmerId === farmerId)
  if (i < 0) throw Object.assign(new Error('Holder not found'), { status: 404 })
  state.farmers.splice(i, 1)
  return { deleted: true }
}

export function linkLogin(farmerId, username) {
  const f = state.farmers.find((x) => x.farmerId === farmerId)
  if (!f) throw Object.assign(new Error('Holder not found'), { status: 404 })
  f.keycloakUsername = username
  f.keycloakSub = `mock-sub-${username}`
  return { keycloakSub: f.keycloakSub, keycloakUsername: username }
}

export function unlinkLogin(farmerId) {
  const f = state.farmers.find((x) => x.farmerId === farmerId)
  if (!f) throw Object.assign(new Error('Holder not found'), { status: 404 })
  delete f.keycloakUsername
  delete f.keycloakSub
  return { unlinked: true }
}

export function createRelated(kind, rec) {
  const created = { osid: `mock-${++seq}`, ...rec }
  state[kind].push(created)
  return clone(created)
}

export function updateRelated(kind, osid, rec) {
  const i = state[kind].findIndex((r) => r.osid === osid)
  if (i < 0) throw Object.assign(new Error(`${kind} not found`), { status: 404 })
  state[kind][i] = { ...state[kind][i], ...rec, osid }
  return clone(state[kind][i])
}

export function deleteRelated(kind, osid) {
  const i = state[kind].findIndex((r) => r.osid === osid)
  if (i < 0) throw Object.assign(new Error(`${kind} not found`), { status: 404 })
  state[kind].splice(i, 1)
  return { deleted: true }
}

/**
 * Claim sources for a holder, in the same shape and precedence order the real
 * path builds — subject record first, then one record per child entity.
 *
 * `recordRef` picks a specific child record (a chosen land parcel, say); without
 * one the newest wins, matching the configured sort in the real path.
 */
export function claimSources(detail, recordRef) {
  const newest = (rows, field) =>
    [...rows].sort((a, b) => (Number(b?.[field]) || 0) - (Number(a?.[field]) || 0))[0]
  const rowsFor = {
    LandParcel: detail.land ?? [],
    Crop: detail.crops ?? [],
    SeedDistribution: detail.seeds ?? [],
    Qualification: detail.qualifications ?? [],
  }
  const chosen = (kind) => {
    const rows = rowsFor[kind]
    if (recordRef) {
      const hit = rows.find((r) =>
        Object.values(r ?? {}).some((v) => v !== null && String(v) === String(recordRef)),
      )
      if (hit) return hit
    }
    if (kind === 'Crop') return newest(rows, 'year')
    if (kind === 'Qualification') return newest(rows, 'yearOfPassing')
    return rows[0]
  }
  return [
    { entity: 'Farmer', record: detail.farmer },
    ...CHILD_KINDS.map((kind) => ({ entity: kind, record: chosen(kind) })),
  ]
}

export function createOffer(farmerId, configId, grant = 'pre-authorized_code') {
  const offerId = `mock-offer-${++seq}`
  // No PIN on the authorization_code path: the holder signs in instead.
  const txCode =
    grant === 'authorization_code' ? undefined : String(Math.floor(100000 + Math.random() * 900000))
  const qrData = `openid-credential-offer://?credential_offer_uri=${encodeURIComponent(
    `https://example.test/oid4vc/offer/${offerId}`,
  )}`
  state.offers.set(offerId, { farmerId, configId, txCode, createdAt: Date.now(), collected: false })
  // Simulate a wallet collecting the offer, so the "Collected" state is
  // reviewable without a wallet present.
  setTimeout(() => {
    const o = state.offers.get(offerId)
    if (o) o.collected = true
  }, 25000)
  return {
    offerId,
    qrData,
    credentialOfferUri: `https://example.test/oid4vc/offer/${offerId}`,
    txCode,
  }
}

export function offerStatus(offerId) {
  const o = state.offers.get(offerId)
  if (!o) return { status: 'expired' }
  if (o.collected) return { status: 'collected', collectedAt: new Date().toISOString() }
  if (Date.now() - o.createdAt > 300_000) return { status: 'expired' }
  return { status: 'pending' }
}

/**
 * Mock sessions for each role, so both UI flows can be reviewed without
 * Keycloak. `/login?as=citizen` picks the second one; the default is staff.
 *
 * The citizen carries a `farmerId` exactly as a real token would, because that
 * claim — not anything in the request — is what scopes the self-service routes.
 */
export const mockSession = (as = 'staff') =>
  as === 'citizen'
    ? {
        authenticated: true,
        username: 'farmer.ravi',
        fullName: 'Ravi Kumar',
        roles: ['citizen'],
        farmerId: 'FRM-000123',
        mock: true,
      }
    : as === 'student'
      ? {
          // The Education issuer's holder, so the citizen view can be reviewed
          // for an issuer whose credential draws on a different entity.
          authenticated: true,
          username: 'student.priya',
          fullName: 'Priya Sharma',
          roles: ['citizen'],
          farmerId: 'EDU-000001',
          mock: true,
        }
      : as === 'norole'
        ? {
            // Signed in with neither role — exercises the no-access screen.
            authenticated: true,
            username: 'nobody',
            fullName: 'No Role',
            roles: [],
            mock: true,
          }
        : {
            authenticated: true,
            username: 'issuer.staff',
            fullName: 'Meera Iyer',
            roles: ['issuer-staff'],
            mock: true,
          }
