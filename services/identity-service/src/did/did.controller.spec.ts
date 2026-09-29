import { Test, TestingModule } from '@nestjs/testing';
import { DidController } from './did.controller';
import { InternalServerErrorException } from '@nestjs/common';
import { DidService } from './did.service';

describe('DidController', () => {
  let controller: DidController;
  let didService: { generateDID: jest.Mock; resolveDID: jest.Mock; resolveWebDID: jest.Mock };
  let content: any;
  const defaultContent = {
    content: [
      {
        alsoKnownAs: ['C4GT', 'https://www.codeforgovtech.in/'],
        services: [
          {
            id: 'C4GT',
            type: 'IdentityHub',
            serviceEndpoint: {
              '@context': 'schema.c4gt.acknowledgment',
              '@type': 'UserServiceEndpoint',
              instance: ['https://www.codeforgovtech.in'],
            },
          },
        ],
        method: 'C4GT',
      },
    ],
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [DidController],
      providers: [
        {
          provide: DidService,
          useFactory: () => ({
            generateDID: jest.fn().mockImplementation(async (doc: any) => ({
              id: `did:${doc.method || 'rcw'}:stub`,
              verificationMethod: [{ id: 'stub#key-0', type: 'Ed25519VerificationKey2020' }],
            })),
            resolveDID: jest.fn(),
            resolveWebDID: jest.fn(),
          }),
        },
      ],
    }).compile();

    controller = module.get<DidController>(DidController);
    didService = module.get(DidService);
    content = JSON.parse(JSON.stringify(defaultContent));
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('should test DID generation', async () => {
    const dids = await controller.generateDID(content);
    expect(dids).toBeDefined();
    expect(dids).toHaveLength(1);
    expect(dids[0].id).toBeDefined();
    expect(dids[0].verificationMethod).toBeDefined();
  });

  it('should test throw DID generation Exception', async () => {
    didService.generateDID.mockRejectedValue(new Error('generate failed'));
    await expect(controller.generateDID(content)).rejects.toThrow(new InternalServerErrorException('generate failed'));
  });

  it('should test resolveDID', async () => {
    didService.resolveDID.mockResolvedValue({ id: '1234' });
    const result = await controller.resolveDID('1234');
    expect(result.id).toEqual('1234');
  });

  it('should test resolveWebDID', async () => {
    didService.resolveWebDID.mockResolvedValue({ id: '1234' });
    const result = await controller.resolveWebDID('1234');
    expect(result.id).toEqual('1234');
  });

  it('should test bulk DID generation', async () => {
    const toGenerate = content;
    for (let i = 0; i < 10; i++) {
      toGenerate.content.push(content.content[0]);
    }
    const dids = await controller.generateDID(toGenerate);
    expect(dids).toBeDefined();
    expect(dids).toHaveLength(11);
  });
});