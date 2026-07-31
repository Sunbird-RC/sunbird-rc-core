import { describe, expect, it } from 'vitest'
import { parseEntitySchema } from './parseEntitySchema'

// Captured shape, verified against RegistrySwaggerController.java and the
// repo's own java/registry/src/test/resources/Student.json fixture — see
// the Phase-5 plan notes. Kept inline so this test has no I/O dependency.
const STUDENT_SWAGGER_FIXTURE = {
  swagger: '2.0',
  paths: {
    '/api/v1/Student': {
      get: { responses: { '200': { description: 'OK', schema: { $ref: '#/definitions/' } } } },
      post: {
        parameters: [{ in: 'body', name: 'body', schema: { $ref: '#/definitions/Student' } }],
        responses: { '200': { description: 'OK', schema: { $ref: '#/definitions/Student' } } },
      },
    },
    '/api/v1/Student/{entityId}': {
      get: { responses: { '200': { description: 'OK', schema: { $ref: '#/definitions/Student' } } } },
      put: { responses: { '200': { description: 'OK', schema: { $ref: '#/definitions/Student' } } } },
    },
    '/api/v1/Student/search': {
      post: { responses: { '200': { description: 'OK', schema: { type: 'array', items: { $ref: '#/definitions/Student' } } } } },
    },
    '/api/v1/Student/sign': {
      get: { responses: { '200': { description: 'OK', schema: { $ref: '#/definitions/' } } } },
    },
  },
  definitions: {
    Student: {
      type: 'object',
      title: 'The Student Schema',
      required: [],
      properties: {
        identityDetails: {
          type: 'object',
          title: 'Identity Details',
          required: [],
          properties: {
            fullName: { type: 'string', title: 'Full name' },
            gender: { type: 'string', enum: ['Male', 'Female', 'Other'], title: 'Gender' },
            dob: { type: 'string', format: 'date', title: 'DOB' },
          },
        },
        contactDetails: {
          type: 'object',
          title: 'Contact Details',
          required: ['email'],
          properties: {
            email: { type: 'string', title: 'Email' },
            mobile: { type: 'string', title: 'Mobile' },
          },
        },
      },
    },
    StudentOsConfig: {
      privateFields: ['$.identityDetails.dob'],
      roles: ['anonymous'],
    },
  },
}

describe('parseEntitySchema', () => {
  it('derives entity types from definitions keys, excluding *OsConfig companions', () => {
    const result = parseEntitySchema(STUDENT_SWAGGER_FIXTURE)
    expect(result.map((r) => r.entityType)).toEqual(['Student'])
  })

  it('flattens nested object properties into dot-paths with human labels', () => {
    const [student] = parseEntitySchema(STUDENT_SWAGGER_FIXTURE)
    const paths = student.fields.map((f) => f.path)
    expect(paths).toEqual([
      'identityDetails.fullName',
      'identityDetails.gender',
      'identityDetails.dob',
      'contactDetails.email',
      'contactDetails.mobile',
    ])
  })

  it('detects enum fields and carries their options', () => {
    const [student] = parseEntitySchema(STUDENT_SWAGGER_FIXTURE)
    const gender = student.fields.find((f) => f.path === 'identityDetails.gender')
    expect(gender?.type).toBe('enum')
    expect(gender?.enumOptions).toEqual(['Male', 'Female', 'Other'])
  })

  it('marks required fields from the parent object required array, not the top-level schema', () => {
    const [student] = parseEntitySchema(STUDENT_SWAGGER_FIXTURE)
    const email = student.fields.find((f) => f.path === 'contactDetails.email')
    const mobile = student.fields.find((f) => f.path === 'contactDetails.mobile')
    expect(email?.required).toBe(true)
    expect(mobile?.required).toBe(false)
  })

  it('caps default table columns at 4 leaf fields', () => {
    const [student] = parseEntitySchema(STUDENT_SWAGGER_FIXTURE)
    expect(student.columns).toHaveLength(4)
    expect(student.columns.map((c) => c.path)).toEqual([
      'identityDetails.fullName',
      'identityDetails.gender',
      'identityDetails.dob',
      'contactDetails.email',
    ])
  })

  it('ignores malformed empty $ref entries (ref resolution is not this parser\'s job)', () => {
    // The swagger doc contains literal "#/definitions/" empty refs (from
    // populateSubEntityActions and the /sign path) — parseEntitySchema
    // only ever reads `definitions`, never walks `paths`, so these can't
    // crash it. This test exists to document that invariant.
    expect(() => parseEntitySchema(STUDENT_SWAGGER_FIXTURE)).not.toThrow()
  })
})
