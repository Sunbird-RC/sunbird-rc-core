// A holder can reveal more than the query asked for. The matcher used to build
// its result from the requested paths alone, so the surplus was dropped and the
// presentation answered normally — the relying party never saw it and no decision
// could turn on it, but the values had already left the wallet and reached this
// service. "The verifier never receives it" was therefore true of the relying
// party and not of the protocol boundary.
//
// These assertions are about which of those two guarantees the service makes.
import { DcqlService } from './dcql.service';

describe('rejecting disclosures the request did not ask for', () => {
  const dcql = new DcqlService();

  const QUERY = {
    credentials: [
      {
        id: 'school_cred',
        format: 'vc+sd-jwt',
        meta: { vct_values: ['https://issuer.example/school/vct/school-record-credential'] },
        claims: [{ path: ['learnerId'] }, { path: ['completionStatus'] }],
      },
    ],
  };

  /** What extractCredentials() hands the matcher for an SD-JWT presentation. */
  const presented = (disclosedNames: string[]) => [
    {
      types: ['VerifiableCredential'],
      vct: 'https://issuer.example/school/vct/school-record-credential',
      format: 'vc+sd-jwt',
      // The merged view: registered claims from the signed payload plus the
      // disclosed values. Deliberately includes iss/iat/cnf, because comparing
      // THIS against the query would refuse every presentation ever made.
      claims: {
        iss: 'did:web:issuer.example:school',
        iat: 1756700000,
        cnf: { jwk: {} },
        vct: 'https://issuer.example/school/vct/school-record-credential',
        learnerId: 'EDU-L-006733',
        completionStatus: 'COMPLETED',
        percentage: 72,
      },
      disclosedNames,
    },
  ];

  it('accepts a presentation that discloses exactly what was asked for', () => {
    const result = dcql.evaluate(QUERY, presented(['learnerId', 'completionStatus']), {
      rejectUnrequestedDisclosures: true,
    });
    expect(result.satisfied).toBe(true);
    expect(result.matched.school_cred).toEqual({
      learnerId: 'EDU-L-006733',
      completionStatus: 'COMPLETED',
    });
  });

  it('refuses one that discloses a claim the request did not ask for', () => {
    const result = dcql.evaluate(
      QUERY,
      presented(['learnerId', 'completionStatus', 'percentage']),
      { rejectUnrequestedDisclosures: true },
    );
    expect(result.satisfied).toBe(false);
    expect(result.reason).toContain('did not ask for');
    expect(result.reason).toContain('percentage');
  });

  it('names the surplus claims but never their values', () => {
    // The reason reaches the relying party. Echoing the value would disclose the
    // very thing the refusal exists to withhold.
    const result = dcql.evaluate(
      QUERY,
      presented(['learnerId', 'completionStatus', 'percentage']),
      { rejectUnrequestedDisclosures: true },
    );
    expect(result.reason).not.toContain('72');
  });

  it('does not fault the registered claims that ride along in every SD-JWT', () => {
    // iss, iat, cnf and vct are in `claims` but are not disclosures. An earlier
    // shape of this check compared the whole claim object and refused everything.
    const result = dcql.evaluate(QUERY, presented(['learnerId', 'completionStatus']), {
      rejectUnrequestedDisclosures: true,
    });
    expect(result.satisfied).toBe(true);
  });

  it('drops the surplus instead of refusing when the flag is off', () => {
    // The previous behaviour, kept reachable for interop debugging against a
    // wallet that over-discloses.
    const result = dcql.evaluate(
      QUERY,
      presented(['learnerId', 'completionStatus', 'percentage']),
      { rejectUnrequestedDisclosures: false },
    );
    expect(result.satisfied).toBe(true);
    expect(result.matched.school_cred).not.toHaveProperty('percentage');
  });

  it('allows anything when the query names no claims, because nothing can exceed it', () => {
    const wholeCredential = {
      credentials: [
        {
          id: 'school_cred',
          format: 'vc+sd-jwt',
          meta: { vct_values: ['https://issuer.example/school/vct/school-record-credential'] },
        },
      ],
    };
    const result = dcql.evaluate(
      wholeCredential,
      presented(['learnerId', 'completionStatus', 'percentage']),
      { rejectUnrequestedDisclosures: true },
    );
    expect(result.satisfied).toBe(true);
  });

  it('is not applied to a format that has no selective disclosure', () => {
    // An ldp_vc carries all of its claims by construction; the holder chose
    // nothing, so there is nothing to refuse them for.
    const ldpQuery = {
      credentials: [
        {
          id: 'c',
          format: 'ldp_vc',
          meta: { type_values: [['VerifiableCredential']] },
          claims: [{ path: ['credentialSubject', 'learnerId'] }],
        },
      ],
    };
    const result = dcql.evaluate(
      ldpQuery,
      [
        {
          types: ['VerifiableCredential'],
          format: 'ldp_vc',
          claims: { learnerId: 'EDU-L-006733', percentage: 72 },
        },
      ],
      { rejectUnrequestedDisclosures: true },
    );
    expect(result.satisfied).toBe(true);
  });

  it('accepts a nested claim disclosed as its top-level object', () => {
    // A request for ["address","city"] is satisfied by disclosing `address`.
    // Comparing the full dotted path would refuse a correct presentation.
    const nested = {
      credentials: [
        {
          id: 'c',
          format: 'vc+sd-jwt',
          meta: { vct_values: ['v'] },
          claims: [{ path: ['address', 'city'] }],
        },
      ],
    };
    const result = dcql.evaluate(
      nested,
      [
        {
          types: ['VerifiableCredential'],
          vct: 'v',
          format: 'vc+sd-jwt',
          claims: { iss: 'did:web:x', address: { city: 'Bengaluru' } },
          disclosedNames: ['address'],
        },
      ],
      { rejectUnrequestedDisclosures: true },
    );
    expect(result.satisfied).toBe(true);
    expect(result.matched.c).toEqual({ 'address.city': 'Bengaluru' });
  });
});
