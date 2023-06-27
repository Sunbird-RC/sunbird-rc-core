import { Test, TestingModule } from '@nestjs/testing';
import { DidService } from './did.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from './vault.service';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { ConfigService } from '@nestjs/config';

describe('DidService', () => {
  let service: DidService;
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

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [DidService, PrismaService, VaultService, ConfigService],
    }).compile();

    // const app = module.createNestApplication();
    // await app.init();

    service = module.get<DidService>(DidService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should generate a DID with a custom method', async () => {
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyJwk).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('C4GT');
  });

  it('should generate a DID with a default method', async () => {
    const docWithoutMethod = doc;
    delete docWithoutMethod.method;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyJwk).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('C4GT');
  });

  it('resolve a DID', async () => {
    const result = await service.generateDID(doc);
    const didToResolve = result.id;
    const resolvedDid = await service.resolveDID(didToResolve);
    expect(resolvedDid).toBeDefined();
    expect(resolvedDid.id).toEqual(didToResolve);
    expect(resolvedDid).toEqual(result);
  });
});
