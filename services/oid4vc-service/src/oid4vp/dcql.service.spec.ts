import { DcqlService } from './dcql.service';

describe('DcqlService', () => {
  const dcql = new DcqlService();

  const presented = [
    {
      types: ['VerifiableCredential', 'TeacherCredential'],
      vct: 'TeacherCredential',
      format: 'jwt_vc_json',
      claims: { name: 'Alice', qualification: 'B.Ed', dob: '1990-01-01' },
    },
  ];

  it('matches by type and returns requested claims', () => {
    const query = {
      credentials: [
        {
          id: 'teacher',
          meta: { type_values: [['TeacherCredential']] },
          claims: [{ path: ['qualification'] }],
        },
      ],
    };
    const res = dcql.evaluate(query, presented);
    expect(res.satisfied).toBe(true);
    expect(res.matched.teacher).toEqual({ qualification: 'B.Ed' });
  });

  it('fails when no credential matches the requested type', () => {
    const query = {
      credentials: [{ id: 'x', meta: { type_values: [['DriverLicense']] }, claims: [] }],
    };
    const res = dcql.evaluate(query, presented);
    expect(res.satisfied).toBe(false);
  });

  it('fails when a requested claim is missing', () => {
    const query = {
      credentials: [
        { id: 'teacher', meta: { type_values: [['TeacherCredential']] }, claims: [{ path: ['salary'] }] },
      ],
    };
    const res = dcql.evaluate(query, presented);
    expect(res.satisfied).toBe(false);
  });

  it('enforces claim value constraints', () => {
    const query = {
      credentials: [
        {
          id: 'teacher',
          meta: { type_values: [['TeacherCredential']] },
          claims: [{ path: ['qualification'], values: ['PhD'] }],
        },
      ],
    };
    expect(dcql.evaluate(query, presented).satisfied).toBe(false);
  });

  it('rejects an empty query', () => {
    expect(dcql.evaluate({ credentials: [] }, presented).satisfied).toBe(false);
  });
});
