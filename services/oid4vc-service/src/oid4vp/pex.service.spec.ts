import { PexService } from './pex.service';

describe('PexService', () => {
  const pex = new PexService();

  const presented = [
    {
      types: ['VerifiableCredential', 'TeacherCredential'],
      vct: 'TeacherCredential',
      format: 'jwt_vc_json',
      claims: { name: 'Alice', qualification: 'B.Ed', dob: '1990-01-01' },
    },
  ];

  const mdocPresented = [
    {
      types: [],
      docType: 'org.iso.18013.5.1.mDL',
      format: 'mso_mdoc',
      claims: { 'org.iso.18013.5.1': { given_name: 'Alice' } },
    },
  ];

  it('matches by field path and discloses the requested field', () => {
    const definition = {
      input_descriptors: [
        {
          id: 'teacher',
          constraints: { fields: [{ path: ['$.credentialSubject.qualification'] }] },
        },
      ],
    };
    const res = pex.evaluate(definition, presented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.teacher).toEqual({ qualification: 'B.Ed' });
  });

  it('enforces filter.const', () => {
    const definition = {
      input_descriptors: [
        {
          id: 'teacher',
          constraints: {
            fields: [{ path: ['$.credentialSubject.qualification'], filter: { const: 'PhD' } }],
          },
        },
      ],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('enforces filter.enum and filter.pattern', () => {
    const enumDef = {
      input_descriptors: [
        { id: 'teacher', constraints: { fields: [{ path: ['$.credentialSubject.qualification'], filter: { enum: ['B.Ed', 'M.Ed'] } }] } },
      ],
    };
    expect(pex.evaluate(enumDef, presented).satisfied).toBe(true);

    const patternDef = {
      input_descriptors: [
        { id: 'teacher', constraints: { fields: [{ path: ['$.credentialSubject.dob'], filter: { pattern: '^1990' } }] } },
      ],
    };
    expect(pex.evaluate(patternDef, presented).satisfied).toBe(true);
  });

  it('fails closed on an unsupported filter keyword', () => {
    const definition = {
      input_descriptors: [
        { id: 'teacher', constraints: { fields: [{ path: ['$.credentialSubject.qualification'], filter: { minimum: 1 } }] } },
      ],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('rejects when format restriction excludes the candidate', () => {
    const definition = {
      input_descriptors: [{ id: 'teacher', format: { mso_mdoc: {} }, constraints: { fields: [] } }],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('fails when a required field is missing', () => {
    const definition = {
      input_descriptors: [{ id: 'teacher', constraints: { fields: [{ path: ['$.credentialSubject.salary'] }] } }],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('an optional missing field does not fail the descriptor', () => {
    const definition = {
      input_descriptors: [
        {
          id: 'teacher',
          constraints: {
            fields: [
              { path: ['$.credentialSubject.qualification'] },
              { path: ['$.credentialSubject.salary'], optional: true },
            ],
          },
        },
      ],
    };
    const res = pex.evaluate(definition, presented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.teacher).toEqual({ qualification: 'B.Ed' });
  });

  it('resolves bracket-notation paths against mdoc namespace keys', () => {
    const definition = {
      input_descriptors: [
        {
          id: 'mdl',
          format: { mso_mdoc: {} },
          constraints: { fields: [{ path: ["$['org.iso.18013.5.1']['given_name']"] }] },
        },
      ],
    };
    const res = pex.evaluate(definition, mdocPresented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.mdl).toEqual({ "org.iso.18013.5.1.given_name": 'Alice' });
  });

  it('rejects an empty presentation_definition', () => {
    expect(pex.evaluate({ input_descriptors: [] }, presented).satisfied).toBe(false);
  });

  it('without submission_requirements, every descriptor is mandatory', () => {
    const definition = {
      input_descriptors: [
        { id: 'teacher', constraints: { fields: [{ path: ['$.credentialSubject.name'] }] } },
        { id: 'x', format: { mso_mdoc: {} }, constraints: { fields: [] } },
      ],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('submission_requirements rule "pick" allows a subset of a group', () => {
    const definition = {
      input_descriptors: [
        { id: 'teacher', group: ['g'], constraints: { fields: [{ path: ['$.credentialSubject.name'] }] } },
        { id: 'never-presented', group: ['g'], format: { mso_mdoc: {} }, constraints: { fields: [] } },
      ],
      submission_requirements: [{ rule: 'pick', count: 1, from: 'g' }],
    };
    const res = pex.evaluate(definition, presented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.teacher).toEqual({ name: 'Alice' });
    expect(res.matched['never-presented']).toBeUndefined();
  });

  it('submission_requirements rule "all" requires every descriptor in the group', () => {
    const definition = {
      input_descriptors: [
        { id: 'teacher', group: ['g'], constraints: { fields: [{ path: ['$.credentialSubject.name'] }] } },
        { id: 'never-presented', group: ['g'], format: { mso_mdoc: {} }, constraints: { fields: [] } },
      ],
      submission_requirements: [{ rule: 'all', from: 'g' }],
    };
    expect(pex.evaluate(definition, presented).satisfied).toBe(false);
  });

  it('submission_requirements from_nested combines nested group results', () => {
    const definition = {
      input_descriptors: [
        { id: 'teacher', group: ['a'], constraints: { fields: [{ path: ['$.credentialSubject.name'] }] } },
        { id: 'never-presented', group: ['b'], format: { mso_mdoc: {} }, constraints: { fields: [] } },
      ],
      submission_requirements: [
        {
          rule: 'pick',
          count: 1,
          from_nested: [
            { rule: 'all', from: 'a' },
            { rule: 'all', from: 'b' },
          ],
        },
      ],
    };
    const res = pex.evaluate(definition, presented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.teacher).toEqual({ name: 'Alice' });
  });
});