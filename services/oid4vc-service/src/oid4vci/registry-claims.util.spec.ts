import { resolveRegistryClaims, type ClaimSource } from './registry-claims.util';

// The claim resolver is what makes wallet self-service issuance safe: it is the
// only thing deciding what a self-issued credential says, from records the holder
// cannot edit. These tests pin the behaviour that matters — that values come from
// the right record, that absent REQUIRED values are reported rather than silently
// dropped, that nothing the caller sends can leak in, and that the resolver holds
// no domain knowledge: a credential type it has never seen, over entities it has
// never heard of, must resolve with no configuration at all.

const subject = {
  farmerId: 'FRM-000123',
  name: 'Ravi Kumar',
  gender: 'Male',
  dateOfBirth: '1979-04-12',
  district: 'Mandya',
};
const parcel = {
  landRecordRef: 'LR-KA-77-2201',
  farmLocation: 'Rampur, Mandya',
  landAreaAcres: 4.5,
  ownershipType: 'Owned',
};
const crop = { cropName: 'Wheat', season: 'Rabi', year: 2026 };

/** The reference deployment's sources, in precedence order. */
const sources = (over: Partial<Record<string, any>> = {}): ClaimSource[] => [
  { entity: 'Farmer', record: 'subject' in over ? over.subject : subject },
  { entity: 'LandParcel', record: 'parcel' in over ? over.parcel : parcel },
  { entity: 'Crop', record: 'crop' in over ? over.crop : crop },
  ...(over.extra ?? []),
];

// The one alias the reference deployment needs: the schema says `primaryCrop`,
// the registry field is `cropName`. Everything else matches by name.
const ALIASES = { primaryCrop: 'cropName' };
const DOB = 'dateOfBirth';

const FARMER_PROPS = [
  'farmerId',
  'name',
  'gender',
  'landRecordRef',
  'farmLocation',
  'landAreaAcres',
  'ownershipType',
  'primaryCrop',
];

describe('resolveRegistryClaims', () => {
  it('pulls each attribute from the correct record', () => {
    const { claims, missing } = resolveRegistryClaims({
      properties: FARMER_PROPS,
      required: ['farmerId', 'name', 'landAreaAcres'],
      sources: sources(),
      aliases: ALIASES,
      birthDateField: DOB,
    });

    expect(missing).toEqual([]);
    expect(claims).toEqual({
      farmerId: 'FRM-000123',
      name: 'Ravi Kumar',
      gender: 'Male',
      landRecordRef: 'LR-KA-77-2201',
      farmLocation: 'Rampur, Mandya',
      landAreaAcres: 4.5,
      ownershipType: 'Owned',
      primaryCrop: 'Wheat',
    });
  });

  it('reports the entity each claim came from', () => {
    // Provenance is what the portal shows staff, and what makes an unexpected
    // value traceable to a record rather than a guess.
    const { sources: provenance } = resolveRegistryClaims({
      properties: ['name', 'landAreaAcres', 'primaryCrop'],
      sources: sources(),
      aliases: ALIASES,
    });
    expect(provenance.name).toBe('Farmer.name');
    expect(provenance.landAreaAcres).toBe('LandParcel.landAreaAcres');
    expect(provenance.primaryCrop).toBe('Crop.cropName');
  });

  it('reports required attributes it cannot fill, and omits optional ones', () => {
    // A holder with no land parcel — the case that must be caught before
    // issuance, since credentials-service reports it only as an opaque 500.
    const { claims, missing } = resolveRegistryClaims({
      properties: FARMER_PROPS,
      required: ['farmerId', 'name', 'landAreaAcres'],
      sources: sources({ parcel: undefined, crop: undefined }),
      aliases: ALIASES,
    });

    expect(missing).toEqual(['landAreaAcres']);
    // Optional parcel/crop fields are absent rather than present-and-empty: an
    // SD-JWT disclosure of "" is a claim, and asserting a blank land reference
    // would be worse than asserting nothing.
    expect(Object.keys(claims).sort()).toEqual(['farmerId', 'gender', 'name']);
    expect(claims).not.toHaveProperty('landRecordRef');
  });

  it('never emits an attribute that matches no field on any record', () => {
    const { claims, missing } = resolveRegistryClaims({
      properties: ['name', 'somethingNobodyMapped'],
      required: ['somethingNobodyMapped'],
      sources: sources(),
    });
    expect(claims).toEqual({ name: 'Ravi Kumar' });
    expect(missing).toEqual(['somethingNobodyMapped']);
  });

  it('matches attribute names regardless of case and separators', () => {
    // Schemas in a registry are authored at different times: the same concept
    // appears as land_area_acres, landAreaAcres and LandAreaAcres.
    for (const attr of ['land_area_acres', 'landAreaAcres', 'LAND-AREA-ACRES']) {
      const { claims } = resolveRegistryClaims({ properties: [attr], sources: sources() });
      expect(claims[attr]).toBe(4.5);
    }
  });

  // --- genericity ----------------------------------------------------------

  it('resolves a credential from an entirely unrelated domain, with no config', () => {
    // The point of the resolver: a domain it has never seen, entity names it has
    // never heard of, no aliases. If this needs a code change, the service is not
    // generic.
    const { claims, missing } = resolveRegistryClaims({
      properties: ['patientId', 'name', 'vaccine', 'doseNumber', 'administeredOn'],
      required: ['patientId', 'vaccine', 'doseNumber'],
      sources: [
        { entity: 'Patient', record: { patientId: 'PAT-9', name: 'Asha Menon' } },
        {
          entity: 'Immunisation',
          record: { vaccine: 'MMR', doseNumber: 2, administeredOn: '2026-02-11' },
        },
      ],
    });
    expect(missing).toEqual([]);
    expect(claims).toEqual({
      patientId: 'PAT-9',
      name: 'Asha Menon',
      vaccine: 'MMR',
      doseNumber: 2,
      administeredOn: '2026-02-11',
    });
  });

  it('resolves the subject record before a related one when both carry a field', () => {
    // Precedence must be the configured order, not whichever search returned
    // first — otherwise the same holder can be issued different values run to run.
    const { claims, sources: provenance } = resolveRegistryClaims({
      properties: ['name'],
      sources: [
        { entity: 'Student', record: { name: 'Real Name' } },
        { entity: 'Enrolment', record: { name: 'Course Name' } },
      ],
    });
    expect(claims.name).toBe('Real Name');
    expect(provenance.name).toBe('Student.name');
  });

  it('honours an entity-qualified alias instead of the first name match', () => {
    // `Enrolment.name` pins the source when the same field name means different
    // things on two records.
    const { claims } = resolveRegistryClaims({
      properties: ['courseName'],
      sources: [
        { entity: 'Student', record: { name: 'Real Name' } },
        { entity: 'Enrolment', record: { name: 'B.Sc. Agriculture' } },
      ],
      aliases: { courseName: 'Enrolment.name' },
    });
    expect(claims.courseName).toBe('B.Sc. Agriculture');
  });

  it('reports an entity-qualified alias as missing rather than falling back', () => {
    // Falling back to another entity's same-named field would silently put the
    // wrong value in a signed credential.
    const { claims, missing } = resolveRegistryClaims({
      properties: ['courseName'],
      required: ['courseName'],
      sources: [{ entity: 'Student', record: { name: 'Real Name' } }],
      aliases: { courseName: 'Enrolment.name' },
    });
    expect(claims).toEqual({});
    expect(missing).toEqual(['courseName']);
  });

  // --- derived age claims --------------------------------------------------

  it('derives age_over_18 from the configured date-of-birth field', () => {
    const { claims } = resolveRegistryClaims({
      properties: ['name', 'birthdate', 'age_over_18'],
      required: ['name', 'birthdate'],
      sources: sources(),
      aliases: { birthdate: DOB },
      birthDateField: DOB,
    });
    expect(claims.birthdate).toBe('1979-04-12');
    expect(claims.age_over_18).toBe(true);
  });

  it('derives any age threshold, not a fixed list', () => {
    // age_over_65 must work without anybody adding it anywhere.
    const born1979 = sources();
    for (const [attr, expected] of [
      ['age_over_21', true],
      ['ageOver40', true],
      ['age_over_65', false],
    ] as const) {
      const { claims } = resolveRegistryClaims({
        properties: [attr],
        sources: born1979,
        birthDateField: DOB,
      });
      expect(claims[attr]).toBe(expected);
    }
  });

  it('derives age_over_18 as false for a minor, without dropping the claim', () => {
    // `false` is a legitimate value; treating it as absent would silently turn a
    // "not over 18" assertion into no assertion at all.
    const { claims, missing } = resolveRegistryClaims({
      properties: ['age_over_18'],
      required: ['age_over_18'],
      sources: sources({ subject: { ...subject, dateOfBirth: '2020-01-01' } }),
      birthDateField: DOB,
    });
    expect(claims.age_over_18).toBe(false);
    expect(missing).toEqual([]);
  });

  it('flags a derived claim as missing when its input is absent', () => {
    const { claims, missing } = resolveRegistryClaims({
      properties: ['age_over_18'],
      required: ['age_over_18'],
      sources: sources({ subject: { farmerId: 'FRM-1', name: 'No DOB' }, parcel: undefined, crop: undefined }),
      birthDateField: DOB,
    });
    expect(claims).not.toHaveProperty('age_over_18');
    expect(missing).toEqual(['age_over_18']);
  });

  it('does not derive an age claim when no birth-date field is configured', () => {
    // Guessing a field name here would be a way to mint an age assertion from
    // whatever date happened to be on the record.
    const { claims, missing } = resolveRegistryClaims({
      properties: ['age_over_18'],
      required: ['age_over_18'],
      sources: sources(),
    });
    expect(claims).not.toHaveProperty('age_over_18');
    expect(missing).toEqual(['age_over_18']);
  });

  it('does not treat an unparseable date of birth as over 18', () => {
    // Failing open here would let a bad record mint a false age assertion.
    const { claims, missing } = resolveRegistryClaims({
      properties: ['age_over_18'],
      required: ['age_over_18'],
      sources: sources({ subject: { ...subject, dateOfBirth: 'not-a-date' } }),
      birthDateField: DOB,
    });
    expect(claims).not.toHaveProperty('age_over_18');
    expect(missing).toEqual(['age_over_18']);
  });

  // --- value fidelity ------------------------------------------------------

  it('keeps a zero-valued number, which is falsy but meaningful', () => {
    const { claims, missing } = resolveRegistryClaims({
      properties: ['landAreaAcres'],
      required: ['landAreaAcres'],
      sources: sources({ parcel: { ...parcel, landAreaAcres: 0 } }),
    });
    expect(claims.landAreaAcres).toBe(0);
    expect(missing).toEqual([]);
  });

  it('resolves against the record it is given, not the first one on the registry', () => {
    // Staff choose which parcel a credential describes; the resolver must honour
    // that choice rather than reaching for a default.
    const { claims } = resolveRegistryClaims({
      properties: ['landRecordRef', 'landAreaAcres', 'ownershipType'],
      sources: sources({
        parcel: { landRecordRef: 'LR-KA-77-2202', landAreaAcres: 1.75, ownershipType: 'Leased' },
      }),
    });
    expect(claims).toEqual({
      landRecordRef: 'LR-KA-77-2202',
      landAreaAcres: 1.75,
      ownershipType: 'Leased',
    });
  });
});
