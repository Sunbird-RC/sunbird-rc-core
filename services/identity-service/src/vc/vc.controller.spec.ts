import { Test, TestingModule } from '@nestjs/testing';
import { VcController } from './vc.controller';
import { InternalServerErrorException } from '@nestjs/common';
import VcService from './vc.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { ConfigService } from '@nestjs/config';
import { DidService } from '../did/did.service';
import { SignedVC } from './dtos/SignedVC.dto';

describe('VcController', () => {
  let controller: VcController;
  let didService: DidService;
  let vcService: VcService;
  let signingDID: string;
  let signedDoc: SignedVC;
  const content = {
    "content": [
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

    ]
  }

  beforeEach(async () => {
    jest.restoreAllMocks();
    const module: TestingModule = await Test.createTestingModule({
      controllers: [VcController],
      providers: [VcService, PrismaService, DidService, VaultService]
    }).compile();
    controller = module.get<VcController>(VcController);
    vcService = module.get<VcService>(VcService);
    didService = (vcService as any).didService;
    const testDidDoc = await didService.generateDID({
        alsoKnownAs: ['Test DID'],
        services: [],
        method: 'test'
      });
    signingDID = testDidDoc.id;
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('should test sign payload', async () => {
    signedDoc = await controller.sign({ DID: signingDID, payload: "{ \"name\": \"alice\" }" });
    expect(signedDoc).toBeDefined();
    expect(signedDoc).toHaveProperty("publicKey");
    expect(signedDoc).toHaveProperty("signed");
  });

  it('should test verify signed payload', async () => {
    console.log(signingDID, signedDoc);
    const verified = await controller.verify({ DID: signingDID, payload: signedDoc.signed})
    expect(verified).toBeDefined();
  });
});
