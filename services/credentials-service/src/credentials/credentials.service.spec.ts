import { HttpModule } from '@nestjs/axios';
import { Test, TestingModule } from '@nestjs/testing';
import { CredentialsService } from './credentials.service';
import Ajv2019 from 'ajv/dist/2019';
import { UnsignedVCValidator, VCValidator } from './types/validators/index';
import { SchemaUtilsSerivce } from './utils/schema.utils.service';
import { IdentityUtilsService } from './utils/identity.utils.service';
import { RenderingUtilsService } from './utils/rendering.utils.service';
import { PrismaClient } from '@prisma/client';

// setup ajv
const ajv = new Ajv2019({ strictTuples: false });
ajv.addFormat('custom-date-time', function (dateTimeString) {
  return typeof dateTimeString === typeof new Date();
});

describe('CredentialsService', () => {
  let service: CredentialsService;
  const sampleCredReqPayload: any = {
    credential: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
      ],
      type: ['VerifiableCredential', 'UniversityDegreeCredential'],
      issuer: 'did:ulp:705a7f13-da2e-4305-a1ca-ac8e750e9ada',
      issuanceDate: '2023-02-06T11:56:27.259Z',
      expirationDate: '2023-02-08T11:56:27.259Z',
      credentialSubject: {
        id: 'did:ulp:b4a191af-d86e-453c-9d0e-dd4771067235',
        grade: '9.23',
        programme: 'B.Tech',
        certifyingInstitute: 'IIIT Sonepat',
        evaluatingInstitute: 'NIT Kurukshetra',
      },
    },
    credentialSchemaId: 'did:ulpschema:c9cc0f03-4f94-4f44-9bcd-b24a86596fa2',
    tags: ['tag1', 'tag2', 'tag3'],
  };

  const issueCredentialReturnTypeSchema = {
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

  const getCredentialByIdSchema = {
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

  const validate = ajv.compile(issueCredentialReturnTypeSchema);
  const getCredReqValidate = ajv.compile(getCredentialByIdSchema);

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [
        CredentialsService,
        PrismaClient,
        RenderingUtilsService,
        SchemaUtilsSerivce,
        IdentityUtilsService,
      ],
    }).compile();

    service = module.get<CredentialsService>(CredentialsService);
  });

  it('service should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should issue a credential', async () => {
    const newCred = await service.issueCredential(sampleCredReqPayload);
    VCValidator.parse(newCred.credential);
    expect(validate(newCred)).toBe(true); // toHaveProperty('credential');
  });

  it('should get a credential', async () => {
    const newCred: any = await service.issueCredential(sampleCredReqPayload);
    const cred = await service.getCredentialById(newCred.credential?.id);
    UnsignedVCValidator.parse(cred);
    expect(getCredReqValidate(cred)).toBe(true);
  });

  it('should throw because no credential is present to be searched by ID', async () => {
    await expect(service.getCredentialById('did:ulp:123')).rejects.toThrow();
  });

  it('should throw because credential not present to be verified', async () => {
    await expect(service.verifyCredential('did:ulp:123')).rejects.toThrow();
  });

  it('should say revoked', async () => {
    const newCred = await service.issueCredential(sampleCredReqPayload);
    expect(
      await service.deleteCredential((newCred.credential as any).id)
    ).toHaveProperty('status', 'REVOKED');
  });

  it('should throw while delete because credential not present', async () => {
    await expect(service.deleteCredential('did:ulp:123')).rejects.toThrow();
  });

  it('should throw', async () => {
    await expect(
      service.getCredentialsBySubjectOrIssuer({
        subject: { id: 'did:ulp:123' },
      })
    ).rejects.toThrow();
  });

  it('should return array of creds based on issuer', async () => {
    try {
      const newCred = await service.issueCredential(sampleCredReqPayload);
      expect(
        await service.getCredentialsBySubjectOrIssuer({
          issuer: {
            id: (newCred.credential as any)?.issuer,
          },
        })
      ).toBeInstanceOf(Array);
    } catch (e) {
      expect(e.message).toBe(
        'No credentials found for the given subject or issuer'
      );
    }
  });
});