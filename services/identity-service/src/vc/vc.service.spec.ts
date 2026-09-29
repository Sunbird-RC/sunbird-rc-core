import { NotFoundException, InternalServerErrorException } from '@nestjs/common';
import VcService from './vc.service';
import { PrismaService } from '../utils/prisma.service';
import { DidService } from '../did/did.service';
import { VaultService } from '../utils/vault.service';
import { VerificationKeyType } from '../did/dtos/GenerateDidRequest.dto';
import { createPrismaServiceOrMock, createVaultServiceOrMock } from '../utils/test-infra.util';

describe('VcService', () => {
  let service: VcService;
  let prisma: PrismaService;
  let vault: VaultService;
  let didService: DidService;
  let didDoc: any;
  let ed2018Doc: any;
  let rsaDoc: any;

  const signPayload = {
    '@context': {
      name: 'http://schema.org/name',
    },
    name: 'Hello!',
  };

  beforeAll(async () => {
    prisma = await createPrismaServiceOrMock();
    vault = await createVaultServiceOrMock();
    didService = new DidService(prisma, vault);
    didService.signingAlgorithm = 'Ed25519Signature2020';

    didDoc = await didService.generateDID({ alsoKnownAs: [], services: [], method: 'test' });
    ed2018Doc = await didService.generateDID({
      alsoKnownAs: [],
      services: [],
      method: 'test',
      keyPairType: VerificationKeyType.Ed25519VerificationKey2018,
    });
    rsaDoc = await didService.generateDID({
      alsoKnownAs: [],
      services: [],
      method: 'test',
      keyPairType: VerificationKeyType.RsaVerificationKey2018,
    });
  });

  beforeEach(async () => {
    service = new VcService(prisma, didService, vault);
    await service.init();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should sign a payload', async () => {
    const signedPayload = await service.sign(didDoc.id, signPayload);
    expect(signedPayload).toBeDefined();
    expect(signedPayload.proof).toBeDefined();
  });

  it('should verify a signed payload successfully', async () => {
    const signedPayload = await service.sign(didDoc.id, signPayload);
    const verified = await service.verify(didDoc.id, signedPayload);
    expect(verified).toBeDefined();
    expect(verified).toBeTruthy();
    expect(verified).toEqual(true);
  });

  it('should fail to verify a tampered payload', async () => {
    const signedPayload = await service.sign(didDoc.id, signPayload);
    signedPayload.name = 'Hello changed';
    const verified = await service.verify(didDoc.id, signedPayload);
    expect(verified).toBeDefined();
    expect(verified).toBeFalsy();
    expect(verified).toEqual(false);
  });

  describe('additional signature suites', () => {
    it('signs and verifies with RsaSignature2018 (RsaVerificationKey2018)', async () => {
      const signed = await service.sign(rsaDoc.id, signPayload);
      expect(signed.proof).toBeDefined();
      const verified = await service.verify(rsaDoc.id, signed);
      expect(verified).toEqual(true);
    });

    it('signs and verifies with Ed25519Signature2018 (Ed25519VerificationKey2018)', async () => {
      const signed = await service.sign(ed2018Doc.id, signPayload);
      expect(signed.proof).toBeDefined();
      const verified = await service.verify(ed2018Doc.id, signed);
      expect(verified).toEqual(true);
    });
  });

  describe('getSuite', () => {
    it('throws NotFoundException for an unsupported signature type', async () => {
      await expect(
        service.getSuite({ type: 'Ed25519VerificationKey2020' }, 'NoSuchSignature2099'),
      ).rejects.toThrow(NotFoundException);
    });

    it('throws NotFoundException when the verification method type is not supported by the signature type', async () => {
      await expect(
        service.getSuite({ type: 'RsaVerificationKey2018' }, 'Ed25519Signature2020'),
      ).rejects.toThrow(NotFoundException);
    });
  });

  describe('error paths', () => {
    it('throws InternalServerErrorException when the Prisma read fails during sign', async () => {
      jest.spyOn(prisma.identity, 'findUnique').mockRejectedValueOnce(new Error('db down'));
      await expect(service.sign(didDoc.id, signPayload)).rejects.toThrow(InternalServerErrorException);
    });

    it('throws NotFoundException when the signer DID does not exist', async () => {
      await expect(service.sign('did:does-not-exist', signPayload)).rejects.toThrow(NotFoundException);
    });

    it('throws InternalServerErrorException when resolving the signer DID fails during verify', async () => {
      jest.spyOn(didService, 'resolveDID').mockRejectedValueOnce(new Error('resolve failed'));
      await expect(service.verify(didDoc.id, {})).rejects.toThrow(InternalServerErrorException);
    });
  });
});