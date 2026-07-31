// Realistic, editable sample prefills for every create form — so an empty
// console (fresh registry, no data) teaches its own use case instead of
// showing blank inputs. Never auto-submitted; always a starting point the
// operator can edit or reload via a "Load sample" button.

// Registry Schema (POST /api/v1/Schema) — modelled on the repo's own
// java/registry/src/test/resources/Student.json. `schema` is sent as a
// STRINGIFIED string (verified against the Postman collection and
// RegistryEntityController's generic create path) — this differs from
// credential-schema's contract, where `schema` is a nested object.
//
// The shape below is load-bearing, not stylistic — verified against
// Definition.java's constructor (java/registry/src/main/java/dev/sunbirdrc/
// registry/util/Definition.java:39-58), which NPEs without it:
//   - top-level `properties.Student` must be a `$ref` to `#/definitions/Student`
//   - the real field definitions live under `definitions.Student.properties`
//     (Definition.java reads `schemaNode.get("definitions").get(title)` —
//     omitting the `definitions` wrapper is exactly the bug that produced
//     `java.lang.NullPointerException at Definition.java:58` when this
//     schema was first tested against the live registry)
//   - `_osConfig` sits at the TOP level of the document (sibling of
//     `definitions`), not nested inside `definitions.Student` — Definition.java
//     reads it via `schemaNode.get("_osConfig")` off the outer node.
export const SAMPLE_STUDENT_SCHEMA_OBJECT = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  title: 'Student',
  type: 'object',
  properties: {
    Student: { $ref: '#/definitions/Student' },
  },
  required: ['Student'],
  definitions: {
    Student: {
      type: 'object',
      title: 'The Student Schema',
      properties: {
        identityDetails: {
          type: 'object',
          title: 'Identity Details',
          properties: {
            fullName: { type: 'string', title: 'Full name' },
            gender: { type: 'string', enum: ['Male', 'Female', 'Other'], title: 'Gender' },
            dob: { type: 'string', format: 'date', title: 'DOB' },
          },
        },
        contactDetails: {
          type: 'object',
          title: 'Contact Details',
          properties: {
            email: { type: 'string', title: 'Email' },
            mobile: { type: 'string', title: 'Mobile' },
          },
        },
      },
    },
  },
  // Field names verified against OSSchemaConfiguration.java (roles,
  // inviteRoles, ownershipAttributes) — this is what makes the type
  // actually usable once published, not decorative metadata.
  _osConfig: {
    privateFields: ['$.identityDetails.dob'],
    internalFields: ['$.contactDetails.email', '$.contactDetails.mobile'],
    indexFields: ['fullName'],
    roles: ['anonymous'],
    inviteRoles: ['anonymous'],
    ownershipAttributes: [],
  },
}

export const SAMPLE_REGISTRY_SCHEMA = {
  name: 'Student',
  description: 'A student enrolled at an institute — sample registry schema, edit freely before creating.',
  schema: JSON.stringify(SAMPLE_STUDENT_SCHEMA_OBJECT, null, 2),
  status: 'DRAFT' as const,
}

// Credential schema (POST /credential-schema) — a DIFFERENT contract from
// the registry Schema entity above, and NOT the flat shape it might look
// like at a glance. Verified live against schema.service.ts's AJV
// validation (the W3C VC-JSON-Schema 2.0 meta-schema at
// services/credential-schema/schema.json): the top-level `schema` field is
// a WRAPPER object requiring `type`/`version`/`name`/`author`/`authored`,
// and the actual JSON Schema goes one level deeper, in `schema.schema`,
// which itself requires `$id`/`description`/`additionalProperties` that
// are easy to omit. Getting this wrong produces a generically-worded
// "Schema validation failed" 400 with no field-level detail — this sample
// exists precisely so a user isn't stuck reverse-engineering that error.
export const SAMPLE_CREDENTIAL_SCHEMA = JSON.stringify(
  {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0.0',
      name: 'ProofOfEnrolment',
      // Any syntactically-valid did:rcw string works — verified live that
      // this field is not resolved/looked up, only format-checked.
      author: 'did:rcw:00000000-0000-0000-0000-000000000000',
      authored: new Date().toISOString(),
      schema: {
        $id: 'Proof-of-Enrolment-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: 'Proof that a student is enrolled in an institute.',
        type: 'object',
        properties: { studentName: { type: 'string' }, institute: { type: 'string' } },
        required: ['studentName', 'institute'],
        additionalProperties: true,
      },
    },
    tags: ['education'],
    status: 'DRAFT',
  },
  null,
  2,
)

// Issue-credential modal — credentialSubject matching the sample schema above.
export const SAMPLE_CREDENTIAL_SUBJECT = JSON.stringify(
  { studentName: 'Asha Menon', institute: 'Govt. HS Kozhikode' },
  null,
  2,
)
export const SAMPLE_SUBJECT_DID = 'did:web:learner.example:asha'

// Attestation policy modal.
export const SAMPLE_POLICY = {
  name: 'teacherVerification',
  attestorEntity: 'Teacher',
  conditions: "(ATTESTOR#$.role#.contains('teacher'))",
}

// Invite modal — matches the design's own placeholder copy.
export const SAMPLE_INVITE = {
  name: 'Fatima Sheikh',
  email: 'fatima.sheikh@example.org',
}

// Synthesises a plausible value for a swagger-derived entity field, keyed
// off its parsed type/format/enum — same idea as
// oid4vc-service/verifier-app/src/api.ts's sampleValue().
export function sampleFieldValue(label: string, type: string, format?: string, enumOptions?: string[]): unknown {
  if (enumOptions && enumOptions.length > 0) return enumOptions[0]
  if (format === 'date') return '1990-01-01'
  if (type === 'number' || type === 'integer') return 1
  if (type === 'boolean') return true
  if (/name/i.test(label)) return 'Asha Devi'
  if (/email/i.test(label)) return 'asha.devi@example.org'
  if (/mobile|phone/i.test(label)) return '9876543210'
  if (/gender/i.test(label)) return 'Female'
  return `Sample ${label}`
}
