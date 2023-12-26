import { Test, TestingModule } from '@nestjs/testing';
import { DidService } from './did.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { ConfigService } from '@nestjs/config';
import { DIDResolutionResult, Resolver } from 'did-resolver';
import * as ION from '@decentralized-identity/ion-tools';
import { of } from 'rxjs';
import { HttpService } from '@nestjs/axios';
import { InternalServerErrorException, NotFoundException } from '@nestjs/common/exceptions';

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

  const expectedDidResponse: DIDResolutionResult = {
    didDocument: {
      "@context": [
        "https://w3.org/ns/did/v1",
        "https://w3id.org/security/suites/ed25519-2018/v1"
      ],
      "id": "did:web:did.actor:alice",
      "publicKey": [
        {
          "id": "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN",
          "controller": "did:web:did.actor:alice",
          "type": "Ed25519VerificationKey2018",
          "publicKeyBase58": "DK7uJiq9PnPnj7AmNZqVBFoLuwTjT1hFPrk6LSjZ2JRz"
        }
      ],
      "authentication": [
        "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN"
      ],
      "assertionMethod": [
        "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN"
      ],
      "capabilityDelegation": [
        "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN"
      ],
      "capabilityInvocation": [
        "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN"
      ],
      "keyAgreement": [
        {
          "id": "did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h",
          "type": "X25519KeyAgreementKey2019",
          "controller": "did:web:did.actor:alice",
          "publicKeyBase58": "CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y"
        }
      ]
    },
    didResolutionMetadata: undefined,
    didDocumentMetadata: undefined
  }

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [DidService, PrismaService, VaultService, ConfigService],
    }).compile();

    // const app = module.createNestApplication();
    // await app.init();

    service = module.get<DidService>(DidService);
    jest.restoreAllMocks();
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
    const { method, ...docWithoutMethod} = doc;
    const result = await service.generateDID(docWithoutMethod);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyJwk).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('rcw');
  });

  it('should generate a DID with a given id', async () => {
    const result = await service.generateDID({ ...doc, id: 'did:web:api.dev.github.net:.well-known'});
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyJwk).toBeDefined();
    expect(result.id).toEqual('did:web:api.dev.github.net:.well-known');
  });

  it('resolve a DID', async () => {
    const result = await service.generateDID(doc);
    const didToResolve = result.id;
    const resolvedDid = await service.resolveDID(didToResolve, false);
    expect(resolvedDid).toBeDefined();
    expect(resolvedDid.id).toEqual(didToResolve);
    expect(resolvedDid).toEqual(result);
  });

  it('resolve a web DID with web did generation disabled', async () => {
    let expectedDidId = "did:web:did.actor:alice";
    service.onModuleInit();
    let webDidResolver: Resolver = service.webDidResolver;
    jest.spyOn(webDidResolver, 'resolve')
      .mockReturnValue(new Promise((resolve, reject) => {
        return resolve(expectedDidResponse);
      }));
    const resolvedDid = await service.resolveDID(expectedDidId, false);
    expect(resolvedDid.id).toEqual(expectedDidId);
    expect(resolvedDid).toEqual(expectedDidResponse.didDocument);
  });

  it('resolve a web DID with web did generation enabled', async () => {
    let expectedDidId = "did:web:did.actor:alice";
    service.onModuleInit();
    service.enableWebDid = true;
    jest.spyOn(service.webDidResolver, 'resolve')
      .mockImplementation(() => new Promise((resolve, reject) => {
        return resolve(expectedDidResponse);
      }));
    service.enableWebDid = true;
    const resolvedDid = await service.resolveDID(expectedDidId, false);
    service.enableWebDid = false;
    expect(resolvedDid.id).toEqual(expectedDidId);
    expect(resolvedDid).toEqual(expectedDidResponse.didDocument);
  });

  it('resolve a generated web DID with web did generation enabled', async () => {
    service.onModuleInit();
    service.enableWebDid = true;
    service.webDidPrefix = "did:web:did.actor:";
    const generatedDid = await service.generateDID({...doc, method: "web"});
    expect(generatedDid.id).toContain(service.webDidPrefix);
    let resolvedDid = await service.resolveDID(generatedDid.id, false);
    expect(generatedDid).toEqual(resolvedDid);
    resolvedDid = await service.resolveDID(generatedDid.id.split(service.webDidPrefix)[1], true);
    expect(generatedDid).toEqual(resolvedDid);
    service.enableWebDid = false;
  });

  it('Should throw exception on getting error in key pair generation', async () => {
    const spy = jest.spyOn(ION, 'generateKeyPair')
    spy.mockImplementation(() => new Promise((resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
      await expect(service.generateDID(doc)).rejects
      .toThrow('Error generating key pair');
    spy.mockReset();
  });

  it('Should throw exception on getting error in writing private key to vault', async () => {
    const spy = jest.spyOn((service as any).vault, 'writePvtKey');
    spy.mockImplementation(() => new Promise((resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
    await expect(service.generateDID(doc)).rejects
    .toThrow('Error writing private key to vault');
    spy.mockRestore();
  });

  it('Should throw exception on getting error in writing to database', async () => {
    const spy = jest.spyOn((service as any).prisma.identity, 'create');
    spy.mockImplementation(() => new Promise((resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
    await expect(service.generateDID(doc)).rejects
    .toThrow('Error writing DID to database');
    spy.mockRestore();
  })

  it('Should throw exception on getting error in finding unique id', async () => {
    const spy = jest.spyOn((service as any).prisma.identity, 'findUnique');
    spy.mockImplementation(() => new Promise((_resolve, reject) => {
        return reject({ message: "Invalid id" });
      }));
    await expect(service.resolveDID("did:abcd:123", false)).rejects
    .toThrow(`Error fetching DID: did:abcd:123 from db`);
  })

  it('Should throw exception on not able to find did', async () => {
    await expect(service.resolveDID("did:abcd:123", false)).rejects
    .toThrow(`DID: did:abcd:123 not found`);
  })
});
