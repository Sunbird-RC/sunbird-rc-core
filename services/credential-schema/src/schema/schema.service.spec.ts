import { Test, TestingModule } from '@nestjs/testing';
import { SchemaService } from './schema.service';
import { CreateCredentialDTO } from './dto/create-credentials.dto';
import { UtilsService } from '../utils/utils.service';
import { PrismaService } from '../prisma.service';
import { HttpModule } from '@nestjs/axios';

describe('SchemaService', () => {
  let service: SchemaService;
  const testCredential: CreateCredentialDTO = {
    schema: {
      type: 'https://w3c-ccg.github.io/vc-json-schemas/',
      version: '1.0',
      id: 'did:ulpschema:c9cc0f03-4f94-4f44-9bcd-b24a8696fa2',
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

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [SchemaService, UtilsService, PrismaService],
    }).compile();
    service = module.get<SchemaService>(SchemaService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should create an unsigned schema and get it from DB', async () => {
    const vcCredSchema = await service.createCredentialSchema(testCredential);
    expect(vcCredSchema).toBeDefined();
    expect(vcCredSchema.schema.proof).toBeFalsy();
    const getVCCredSchema = await service.getCredentialSchema({
      id: vcCredSchema.schema.id,
    });
    expect(vcCredSchema).toEqual(getVCCredSchema);
  });
});
