import { randomUUID } from 'crypto';
import { CreateCredentialDTO } from './dto/create-credentials.dto';

export const generateCredentialSchemaTestBody = (): CreateCredentialDTO => {
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
