export const samples = [
  {
    sample: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
        'https://playground.chapi.io/examples/alumni/alumni-v1.json',
        'https://w3id.org/security/suites/ed25519-2020/v1',
      ],
      type: ['VerifiableCredential', 'AlumniCredential'],
      issuer: {
        id: 'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
      },
      name: 'Alumni Credential',
      description: 'The holder is an alumni of Example University.',
      issuanceDate: '2022-12-19T09:22:23.064Z',
      credentialSubject: {
        id: 'did:example:ebfeb1f712ebc6f1c276e12ec21',
        alumniOf: {
          identifier: 'did:example:c276e12ec21ebfeb1f712ebc6f1',
          name: 'Example University',
        },
      },
      id: 'https://playground.chapi.io/credential/O18Q1zJx0g81EKloPPQwo',
      proof: {
        type: 'Ed25519Signature2020',
        created: '2022-12-19T09:22:23Z',
        verificationMethod:
          'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
        proofPurpose: 'assertionMethod',
        proofValue:
          'z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB',
      },
    },
    isValid: false,
  },
  {
    sample: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: '06e126d1-fa44-4882-a243-1e326fbe21db',
      name: 'Email',
      author: 'did:example:MDP8AsFhHzhwUvGNuYkX7T',
      authored: '2021-01-01T00:00:00+00:00',
    },
    isValid: false,
  },
  {
    sample: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.1',
      id: 'did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db;version=1.1',
      name: 'Email',
      author: 'did:example:MDP8AsFhHzhwUvGNuYkX7T',
      authored: '2018-01-01T00:00:00+00:00',
      schema: {
        $id: 'email-schema-1.1',
        $schema: 'https://json-schema.org/draft/2020-12/schema',
        description: 'Email',
        type: 'object',
        properties: {
          emailAddress: {
            type: 'string',
            format: 'email',
          },
          backupEmailAddress: {
            type: 'string',
            format: 'email',
          },
        },
        required: ['emailAddress'],
        additionalProperties: false,
      },
    },
    isValid: true,
  },
  {
    sample: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.1',
      id: 'did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db;version=1.1',
      name: 'Email',
      author: 'did:example:MDP8AsFhHzhwUvGNuYkX7T',
      authored: '2018-01-01T00:00:00+00:00',
      schema: {
        $id: 'email-schema-1.1',
        $schema: 'https://json-schema.org/draft/2020-12/schema',
        description: 'Email',
        type: 'object',
        properties: {
          emailAddress: {
            type: 'string',
            format: 'email',
          },
          backupEmailAddress: {
            type: 'string',
            format: 'email',
          },
        },
        required: ['emailAddress'],
        additionalProperties: false,
      },
    },
    isValid: true,
  },
  {
    sample: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0',
      name: 'Email',
      author: 'did:example:MDP8AsFhHzhwUvGNuYkX7T',
      authored: '2018-01-01T00:00:00+00:00',
      schema: {
        $id: 'email-schema-1.0',
        $schema: 'https://json-schema.org/draft/2020-12/schema',
        description: 'Email',
        type: 'object',
        properties: {
          emailAddress: {
            type: 'string',
            format: 'email',
          },
        },
        required: ['emailAddress'],
        additionalProperties: false,
      },
    },
    isValid: true,
  },
  {
    sample: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
        'https://playground.chapi.io/examples/alumni/alumni-v1.json',
        'https://w3id.org/security/suites/ed25519-2020/v1',
      ],
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:example:ebfeb1f712ebc6f1c276e12ec21',
      name: 'Alumni Credential',
      author: 'did:example:c276e12ec21ebfeb1f712ebc6f1',
      authored: '2022-12-19T09:22:23.064Z',
      schema: {
        $id: 'Alumni-Credential-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: 'The holder is an alumni of Example University.',
        type: 'object',
        properties: {
          alumniOf: {
            type: 'object',
            properties: {
              identifier: {
                type: 'string',
                format: 'did',
                description: 'The did for the issuing university (issuer).',
              },
              name: {
                type: 'string',
                description: 'Name of the issuing university (issuer).',
              },
            },
            required: ['identifier', 'name'],
          },
        },
        required: ['alumniOf'],
        additionalProperties: false,
      },
      proof: {
        type: 'Ed25519Signature2020',
        created: '2022-12-19T09:22:23Z',
        verificationMethod:
          'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
        proofPurpose: 'assertionMethod',
        proofValue:
          'z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB',
      },
    },
    isValid: true,
  },
  {
    sample: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
        'https://playground.chapi.io/examples/alumni/alumni-v1.json',
        'https://w3id.org/security/suites/ed25519-2020/v1',
      ],
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:example:ebfeb1f712ebc6f1c276e12ec21',
      name: 'Proof of Academic Evaluation Credential',
      author: 'did:example:c276e12ec21ebfeb1f712ebc6f1',
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
        additionalProperties: false,
      },
      proof: {
        type: 'Ed25519Signature2020',
        created: '2022-12-19T09:22:23Z',
        verificationMethod:
          'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
        proofPurpose: 'assertionMethod',
        proofValue:
          'z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB',
      },
    },
    isValid: true,
  },
  {
    sample: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
        'https://playground.chapi.io/examples/alumni/alumni-v1.json',
        'https://w3id.org/security/suites/ed25519-2020/v1',
      ],
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:example:ebfeb1f712ebc6f1c276e12ec21',
      name: 'Proof of Training Credential',
      author: 'did:example:c276e12ec21ebfeb1f712ebc6f1',
      authored: '2022-12-19T09:22:23.064Z',
      schema: {
        $id: 'Proof-of-Training-Credential-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description:
          'The holder has completed training for <SKILL> in <ABC_Institute>.',
        type: 'object',
        properties: {
          skill: {
            type: 'string',
            description: 'The skill for which the holder was trained.',
          },
          certifyingInstitute: {
            type: 'string',
            description:
              'Name of the instute which certified the holder being skilled in the said skill',
          },
          trainingInstitute: {
            type: 'string',
            description: 'Name of the institute which organised the training.',
          },
        },
        required: ['skill', 'releasingInstitute', 'trainingInstitute'],
        additionalProperties: false,
      },
      proof: {
        type: 'Ed25519Signature2020',
        created: '2022-12-19T09:22:23Z',
        verificationMethod:
          'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
        proofPurpose: 'assertionMethod',
        proofValue:
          'z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB',
      },
    },
    isValid: true,
  },
  {
    sample: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
        'https://playground.chapi.io/examples/alumni/alumni-v1.json',
        'https://w3id.org/security/suites/ed25519-2020/v1',
      ],
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:example:ebfeb1f712ebc6f1c276e12ec21',
      name: 'Marks Credential',
      author: 'did:example:c276e12ec21ebfeb1f712ebc6f1',
      authored: '2022-12-19T09:22:23.064Z',
      schema: {
        $id: 'Marks-Credential-1.0',
        $schema: 'https://json-schema.org/draft/2019-09/schema',
        description: 'The holder has scored the <MARKS> in ABC Examination.',
        type: 'object',
        properties: {
          score: {
            type: 'number',
            description: 'Marks scored by the holder in ABC Examination.',
          },
          examinationName: {
            type: 'string',
            description: 'Name of the examination taken',
          },
          releasingInstitution: {
            type: 'string',
            description: 'Name of the instute which released the score',
          },
          organisingInstitute: {
            type: 'string',
            description:
              'Name of the institute which organised the examination.',
          },
        },
        required: [
          'score',
          'examinationName',
          'releasingInstitution',
          'organisingInstitute',
        ],
        additionalProperties: false,
      },
      proof: {
        type: 'Ed25519Signature2020',
        created: '2022-12-19T09:22:23Z',
        verificationMethod:
          'did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN',
        proofPurpose: 'assertionMethod',
        proofValue:
          'z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB',
      },
    },
    isValid: true,
  },
];
