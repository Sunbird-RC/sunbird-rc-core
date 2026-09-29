import { DidService } from './did.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';
import { createPrismaServiceOrMock, createVaultServiceOrMock } from '../utils/test-infra.util';
import { GenerateDidDTO, VerificationKeyType } from './dtos/GenerateDidRequest.dto';

describe('DidService', () => {
  let service: DidService;
  let doc: GenerateDidDTO;
  let prisma: PrismaService;
  let vault: VaultService;

  const defaultDoc: GenerateDidDTO = {
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
  };

  beforeEach(async () => {
    doc = JSON.parse(JSON.stringify(defaultDoc));
    prisma = await createPrismaServiceOrMock();
    vault = await createVaultServiceOrMock();
    service = new DidService(prisma, vault);
    service.signingAlgorithm = 'Ed25519Signature2020';
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should generate a DID with a custom method', async () => {
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyMultibase).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('C4GT');

    const stored = await prisma.identity.findUnique({ where: { id: result.id } });
    expect(stored).toBeDefined();
    expect(JSON.parse(stored.didDoc as string).id).toEqual(result.id);
  });

  it('should generate a DID with a default method', async () => {
    delete doc.method;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].publicKeyMultibase).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('rcw');
  });

  it('should generate a web DID with a given base url and verification key', async () => {
    doc.method = 'web';
    doc.webDidBaseUrl = 'https://registry.dev.example.com/identity';
    doc.keyPairType = VerificationKeyType.Ed25519VerificationKey2018;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.id).toMatch(/did:web:registry.dev.example.com:*/i);
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].type).toStrictEqual(VerificationKeyType.Ed25519VerificationKey2018.toString());
  });

  it('should generate a web DID with RsaVerificationKey2018 verification key', async () => {
    doc.method = 'abc';
    doc.keyPairType = VerificationKeyType.RsaVerificationKey2018;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].type).toStrictEqual(VerificationKeyType.RsaVerificationKey2018.toString());
  });

  it('should generate a DID with a given ID', async () => {
    doc.method = 'web';
    doc.id = `did:web:abc.com:given:${Date.now()}`;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.id).toMatch(doc.id);
  });

  it('should generate a DID with an EC/JsonWebKey2020 verification key (mdoc)', async () => {
    doc.method = 'abc';
    doc.keyPairType = VerificationKeyType.JsonWebKey2020;
    const result = await service.generateDID(doc);
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.verificationMethod[0].type).toStrictEqual('JsonWebKey2020');
    expect(result.verificationMethod[0].publicKeyJwk).toBeDefined();
  });

  it('throws InternalServerErrorException when persisting the DID to the database fails', async () => {
    jest.spyOn(prisma.identity, 'create').mockRejectedValueOnce(new Error('db down'));
    await expect(service.generateDID(doc)).rejects.toThrow('Error writing DID to database');
  });

  it('throws InternalServerErrorException when writing the private key to Vault fails', async () => {
    jest.spyOn(vault, 'writePvtKey').mockRejectedValueOnce(new Error('vault down'));
    await expect(service.generateDID(doc)).rejects.toThrow('Error writing private key to vault');
  });

  it('resolve a DID', async () => {
    const generated = await service.generateDID(doc);

    const resolvedDid = await service.resolveDID(generated.id);
    expect(resolvedDid).toBeDefined();
    expect(resolvedDid.id).toEqual(generated.id);
    expect(resolvedDid).toEqual(generated);

    await expect(service.resolveDID('did:abc:efg:hij')).rejects.toThrow('DID: did:abc:efg:hij not found');
  });

  it('throws InternalServerErrorException when the Prisma read fails during resolve', async () => {
    jest.spyOn(prisma.identity, 'findUnique').mockRejectedValueOnce(new Error('db down'));
    await expect(service.resolveDID('did:x')).rejects.toThrow('Error fetching DID: did:x from db');
  });

  it('resolve a web DID for given id', async () => {
    service.webDidPrefix = 'did:web:abc.com:resolveweb:';
    doc.method = 'web';
    const generated = await service.generateDID(doc);

    const resolvedDid = await service.resolveWebDID(generated.id.split(service.webDidPrefix)[1]);
    expect(resolvedDid).toBeDefined();
    expect(resolvedDid.id).toEqual(generated.id);
    expect(resolvedDid).toEqual(generated);
  });

  it('generate web did id test', () => {
    service.webDidPrefix = 'did:web:example.com:identity:';
    const didId = service.generateDidUri('web');
    expect(didId).toBeDefined();
    expect(didId).toContain('did:web:example.com:identity');
  });

  it('get web did id for id test', () => {
    service.webDidPrefix = 'did:web:example.com:identity:';
    const didId = service.getWebDidIdForId('abc');
    expect(didId).toBeDefined();
    expect(didId).toEqual('did:web:example.com:identity:abc');
  });

  it('should generate a DID with a web method', async () => {
    service.webDidPrefix = 'did:web:example.com:identity:';
    const result = await service.generateDID({
      alsoKnownAs: [],
      services: [],
      method: 'web',
    });
    expect(result).toBeDefined();
    expect(result.verificationMethod).toBeDefined();
    expect(result.id.split(':')[1]).toEqual('web');
    expect(result.id).toContain('did:web:example.com:identity');
  });

  it('throw exception when web did base url is not set', () => {
    service.webDidPrefix = undefined;
    expect(() => service.getWebDidIdForId('abc'))
      .toThrow('Web did base url not found');
  });
});
