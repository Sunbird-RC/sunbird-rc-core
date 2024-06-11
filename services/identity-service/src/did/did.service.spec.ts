import { Test, TestingModule } from '@nestjs/testing';
import { DidService } from './did.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { GenerateDidDTO } from './dtos/GenerateDidRequest.dto';
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
    expect(
      result.verificationMethod[0].publicKeyMultibase
      ).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('C4GT');
  });

  it('should generate a DID with a default method', async () => {
    const docWithoutMethod = doc;
    delete docWithoutMethod.method;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyMultibase).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('rcw');
  });

  it('resolve a DID', async () => {
    const result = await service.generateDID(doc);
    const didToResolve = result.id;
    const resolvedDid = await service.resolveDID(didToResolve);
    expect(resolvedDid).toBeDefined();
    expect(resolvedDid.id).toEqual(didToResolve);
    expect(resolvedDid).toEqual(result);
  });

  it("generate web did id test", () => {
    service.webDidPrefix = "did:web:example.com:identity:";
    const didId = service.generateDidUri("web");
    expect(didId).toBeDefined();
    expect(didId).toContain("did:web:example.com:identity");
  });
  it("get web did id for id test", () => {
    service.webDidPrefix = "did:web:example.com:identity:";
    const didId = service.getWebDidIdForId("abc");
    expect(didId).toBeDefined();
    expect(didId).toEqual("did:web:example.com:identity:abc");
  });

  it('should generate a DID with a web method', async () => {
    service.webDidPrefix = "did:web:example.com:identity:";
    const result = await service.generateDID({
      alsoKnownAs: [],
      services: [],
      method: "web"
    });
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('web');
    expect(result.id).toContain("did:web:example.com:identity");
  });

  it("throw exception when web did base url is not set", () => {
    service.webDidPrefix = undefined;
    expect(() => service.getWebDidIdForId("abc"))
    .toThrow("Web did base url not found");
  });
});
