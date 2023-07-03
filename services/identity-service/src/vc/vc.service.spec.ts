import { Test, TestingModule } from '@nestjs/testing';
import VcService from './vc.service';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
import { VaultService } from '../utils/vault.service';
import { setupTestValue } from './test-setup';

describe('DidService', () => {
  let service: VcService;
  let didService: DidService;
  let signingDID: string;

  // beforeAll(async () => {
  //   // Seed the test value before running the tests
  //   await setupTestValue();
  // });

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [VcService, PrismaService, DidService, VaultService],
    }).compile();

    service = module.get<VcService>(VcService);
    didService = module.get<DidService>(DidService);
    const testDidDoc = await didService.generateDID({
      alsoKnownAs: ['Test DID'],
      services: [],
      method: 'test'
    });
    signingDID = testDidDoc.id;
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should sign a payload', async () => {
    const signedPayload = await service.sign(signingDID, 'Hello!');
    expect(signedPayload).toBeDefined();
    expect(signedPayload.publicKey).toBeDefined();
    expect(signedPayload.signed).toBeDefined();
  });

  it('should verify a signed payload successfully', async () => {
    const signedPayload = await service.sign(signingDID, 'Hello!');
    const verified = await service.verify(signingDID, signedPayload.signed);
    expect(verified).toBeDefined();
    expect(verified).toBeTruthy();
    expect(verified).toEqual(true);
  });
  it('should fail to verify a signed payload', async () => {
    const signedPayload = await service.sign(signingDID, 'Hello!');
    const verified = await service.verify(signingDID, (signedPayload.signed as string).slice(1));
    expect(verified).toBeDefined();
    expect(verified).toBeFalsy();
    expect(verified).toEqual(false);
  });
});
