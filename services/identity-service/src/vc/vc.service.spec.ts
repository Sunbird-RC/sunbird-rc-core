import { Test, TestingModule } from '@nestjs/testing';
import VcService from './vc.service';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
import { VaultService } from '../utils/vault.service';
import { setupTestValue } from './test-setup';
import * as ION from '@decentralized-identity/ion-tools';
import { GenerateDidDTO } from 'src/did/dtos/GenerateDid.dto';

describe('DidService', () => {
  let service: VcService;
  let didService: DidService;
  let signingDID: string;

  const doc: GenerateDidDTO =
  {
    "alsoKnownAs": [
      "C4GT",
      "https://www.codeforgovtech.in/"
    ],
    "services": [
      {
        "id": "C4GT",
        "type": "IdentityHub",
        "serviceEndpoint": {
          "@context": "schema.c4gt.acknowledgment",
          "@type": "UserServiceEndpoint",
          "instance": [
            "https://www.codeforgovtech.in"
          ]
        }
      }
    ],
    "method": "C4GT"
  }

  // beforeAll(async () => {
  //   // Seed the test value before running the tests
  //   await setupTestValue();
  // });

  beforeEach(async () => {
    jest.restoreAllMocks();
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

  it('Should throw exception on getting error signing the document', async () => {
    const spy = jest.spyOn(ION, 'signJws')
    spy.mockImplementation(() => new Promise((resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
    const did = await didService.generateDID(doc);
    await expect(service.sign(did.id, "")).rejects
    .toThrow(`Error signign the document`);
    spy.mockRestore();
  })

  it('Should throw exception on getting error resolving singed did', async () => {
    await expect(service.verify("did:abcd:123", "")).rejects
    .toThrow(`Error resolving signed did`);
  })

  it('Should return false on getting error verifying jws', async () => {
    const spy = jest.spyOn(ION, 'verifyJws')
    spy.mockImplementation(() => new Promise((resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
    const did = await didService.generateDID(doc);
    await expect(service.verify(did.id, "")).resolves
    .toBe(false);
    spy.mockRestore();
  })
});
