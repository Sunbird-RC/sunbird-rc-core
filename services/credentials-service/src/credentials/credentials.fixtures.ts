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

export const generateRenderingTemplatePayload = (schemaId, schemaVersion) => {
  return {
    schemaId: schemaId,
    schemaVersion: schemaVersion,
    template: "<html lang='en'><head><meta charset='UTF-8' /><meta http-equiv='X-UA-Compatible' content='IE=edge' /><meta name='viewport' content='width=device-width, initial-scale=1.0' /><script src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.0/jquery.min.js'></script><link rel='stylesheet' href='style.css' /><title>Certificate</title></head><body><div class='outer-border'><div class='inner-dotted-border'><img class='logo' src='https://www.vid.no/site/assets/files/17244/christ-deemed-to-be-university-vid.png' alt='' /><br /><span class='certification'>CERTIFICATE OF COMPLETION</span><br /><br /><span class='certify'><i>is hereby awarded to</i></span><br /><br /><span class='name'><b>Daniel Vitorrie</b></span><br /><br /><span class='certify'><i>for successfully completing the</i></span><br /><br /><span class='fs-30 diploma'>diploma in Java Developer</span><br /><br /><span class='fs-20 thank'>Thank you for demonstrating the type of character and integrity that inspire others</span><br /><div class='footer'><div class='date'><span class='certify'><i>Awarded: </i></span><br /><span class='fs-20'> xxxxxx</span></div><div class='qr'>{{grade}}, {{programme}}, {{certifyingInstitute}}, {{evaluatingInstitute}}/></div><span class='sign'>Dean, Christ Univeristy</span></div></div></div></body><style>*{margin:1px;padding:0;box-sizing:border-box}.outer-border{width:80vw;padding:10px;text-align:center;border:10px solid #252F50;font-family:'Lato', sans-serif;margin:auto}.inner-dotted-border{padding:10px;text-align:center;border:2px solid #252F50}.logo{width:75px}.certification{font-size:50px;font-weight:bold;color:#252F50;font-family:\"Times New Roman\", Times, serif}.certify{font-size:20px;color:#252F50}.diploma{color:#252F50;font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.name{font-size:30px;color:#252F50;border-bottom:1px solid;font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.thank{color:#252F50}.fs-30{font-size:30px}.fs-20{font-size:20px}.footer{display:flex;justify-content:space-between;margin:0 4rem}.date{display:flex;align-self:flex-end}.date>.fs-20{font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.qr{margin-top:1.25rem;display:flex;justify-content:end;margin-left:0.75rem;margin-right:0.75rem}.sign{border-top:1px solid;align-self:flex-end}</style></html>",
    type: "Handlebar"
  }
}
