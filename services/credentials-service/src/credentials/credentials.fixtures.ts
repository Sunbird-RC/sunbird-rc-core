import { randomUUID } from 'crypto';

export const generateCredentialSchemaTestBody = () => {
  return {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0.0',
      id: `${randomUUID()}`,
      name: 'Proof of Academic Evaluation Credential',
      author: 'did:rcw:82f1ef57-3969-42db-9ef3-a477f5f7b010',
      authored: '2022-12-19T09:22:23.064Z',
      schema: {
        $id: 'Proof-of-Academic-Evaluation-Credential-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description:
          'The holder has secured the <PERCENTAGE/GRADE> in <PROGRAMME> from <ABC_Institute>.',
        type: 'object',
        properties: {
          grade: {
            type: 'string',
            description: 'Grade (%age, GPA, etc.) secured by the holder.',
          },
          programme: {
            type: 'string',
            description: 'Name of the programme pursed by the holder.',
          },
          certifyingInstitute: {
            type: 'string',
            description:
              'Name of the instute which certified the said grade in the said skill',
          },
          evaluatingInstitute: {
            type: 'string',
            description:
              'Name of the institute which ran the programme and evaluated the holder.',
          },
        },
        required: [
          'grade',
          'programme',
          'certifyingInstitute',
          'evaluatingInstitute',
        ],
        additionalProperties: true,
      },
    },
    tags: ['tag1', 'tag2'],
    status: 'DRAFT',
  };
};

export const generateTestDIDBody = () => {
  return {
    content: [
      {
        alsoKnownAs: ['testDID'],
        services: [
          {
            id: 'CredentialSchemaService',
            type: 'CredentialSchema',
          },
        ],
        method: 'schema',
      },
    ],
  };
};

export const generateCredentialRequestPayload = (
  issuerid,
  subjectid,
  schemaid,
  credentialSchemaVersion
) => {
  return {
    credential: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
      ],
      type: ['VerifiableCredential', 'UniversityDegreeCredential'],
      issuer: issuerid,
      issuanceDate: '2023-02-06T11:56:27.259Z',
      expirationDate: '2023-02-08T11:56:27.259Z',
      credentialSubject: {
        id: subjectid,
        grade: '9.23',
        programme: 'B.Tech',
        certifyingInstitute: 'IIIT Sonepat',
        evaluatingInstitute: 'NIT Kurukshetra',
      },
    },
    credentialSchemaId: schemaid,
    credentialSchemaVersion: credentialSchemaVersion,
    tags: ['tag1', 'tag2', 'tag3'],
  };
};

export const issueCredentialReturnTypeSchema = {
  type: 'object',
  properties: {
    credential: {
      type: 'object',
      properties: {
        '@context': {
          type: 'array',
          items: [{ type: 'string' }],
        },
        id: {
          type: 'string',
        },
        type: {
          type: 'array',
          items: [{ type: 'string' }],
        },
        proof: {
          type: 'object',
          properties: {
            type: { type: 'string' },
            created: { type: 'string' },
            proofValue: { type: 'string' },
            proofPurpose: { type: 'string' },
            verificationMethod: { type: 'string' },
          },
        },
        issuer: { type: 'string' },
        issuanceDate: { type: 'string' },
        expirationDate: { type: 'string' },
        credentialSubject: {
          type: 'object',
          properties: {
            id: { type: 'string' },
            grade: { type: 'string' },
            programme: { type: 'string' },
            certifyingInstitute: { type: 'string' },
            evaluatingInstitute: { type: 'string' },
          },
          required: [
            'id',
            'grade',
            'programme',
            'certifyingInstitute',
            'evaluatingInstitute',
          ],
        },
      },
      required: [
        '@context',
        'id',
        'issuer',
        'expirationDate',
        'credentialSubject',
        'issuanceDate',
        'type',
        'proof',
      ],
    },
    credentialSchemaId: { type: 'string' },
    createdAt: { type: 'object', format: 'custom-date-time' },
    updatedAt: { type: 'object', format: 'custom-date-time' },
    createdBy: { type: 'string' },
    updatedBy: { type: 'string' },
    tags: {
      type: 'array',
      items: [{ type: 'string' }],
    },
  },
  required: [
    'credential',
    'credentialSchemaId',
    'tags',
    'createdAt',
    'updatedAt',
    'createdBy',
    'updatedBy',
  ],
  additionalProperties: false,
};

export const getCredentialByIdSchema = {
  type: 'object',
  properties: {
    '@context': {
      type: 'array',
      items: [{ type: 'string' }],
    },
    id: { type: 'string' },
    type: {
      type: 'array',
      items: [{ type: 'string' }],
    },
    issuer: { type: 'string' },
    issuanceDate: { type: 'string' },
    expirationDate: { type: 'string' },
    credentialSubject: {
      type: 'object',
      properties: {
        id: { type: 'string' },
        grade: { type: 'string' },
        programme: { type: 'string' },
        certifyingInstitute: { type: 'string' },
        evaluatingInstitute: { type: 'string' },
      },
      required: [
        'id',
        'grade',
        'programme',
        'certifyingInstitute',
        'evaluatingInstitute',
      ],
    },
  },
  required: [
    '@context',
    'id',
    'issuer',
    'expirationDate',
    'credentialSubject',
    'issuanceDate',
    'type',
  ],
};
