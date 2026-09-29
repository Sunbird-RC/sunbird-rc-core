import * as crypto from 'crypto';
import { NotFoundException, InternalServerErrorException } from '@nestjs/common';
import { MdocService } from './mdoc.service';
import { PrismaService } from '../utils/prisma.service';
import { VaultService } from '../utils/vault.service';

describe('MdocService', () => {
  const signerDID = 'did:rcw:signer-1';
  const vmId = `${signerDID}#key-0`;
  const docType = 'org.iso.18013.5.1.mDL';

  let prisma: { identity: { findUnique: jest.Mock } };
  let vault: { readPvtKey: jest.Mock };
  let service: MdocService;
  let publicKeyJwk: any;
  let privateKeyJwk: any;

  beforeAll(() => {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
    publicKeyJwk = publicKey.export({ format: 'jwk' });
    privateKeyJwk = privateKey.export({ format: 'jwk' });
  });

  beforeEach(() => {
    prisma = { identity: { findUnique: jest.fn() } };
    vault = { readPvtKey: jest.fn() };
    service = new MdocService(prisma as unknown as PrismaService, vault as unknown as VaultService);
  });

  function primeSigner() {
    prisma.identity.findUnique.mockResolvedValue({
      id: signerDID,
      didDoc: JSON.stringify({
        id: signerDID,
        verificationMethod: [{ id: vmId, type: 'JsonWebKey2020', controller: signerDID, publicKeyJwk }],
      }),
    });
    vault.readPvtKey.mockResolvedValue({ [vmId]: { privateKeyJwk } });
  }

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('signMdoc', () => {
    it('signs an mdoc for a DID with a JsonWebKey2020 verification method', async () => {
      primeSigner();
      const mdoc = await service.signMdoc(signerDID, docType, {
        'org.iso.18013.5.1': { given_name: 'Alice', family_name: 'Doe' },
      });
      expect(typeof mdoc).toBe('string');
      expect(mdoc.length).toBeGreaterThan(0);
    });

    it('accepts an optional deviceKeyJwk', async () => {
      primeSigner();
      const { publicKey: deviceKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
      const deviceKeyJwk = deviceKey.export({ format: 'jwk' });
      const mdoc = await service.signMdoc(
        signerDID,
        docType,
        { 'org.iso.18013.5.1': { given_name: 'Alice' } },
        deviceKeyJwk as any,
      );
      expect(typeof mdoc).toBe('string');
      expect(mdoc.length).toBeGreaterThan(0);
    });

    it('throws NotFoundException when the signer DID does not exist', async () => {
      prisma.identity.findUnique.mockResolvedValue(null);
      await expect(service.signMdoc(signerDID, docType, {})).rejects.toThrow(NotFoundException);
    });

    it('throws InternalServerErrorException when the Prisma read fails', async () => {
      prisma.identity.findUnique.mockRejectedValue(new Error('db down'));
      await expect(service.signMdoc(signerDID, docType, {})).rejects.toThrow(InternalServerErrorException);
    });

    it('throws NotFoundException when the DID has no JsonWebKey2020 verification method', async () => {
      prisma.identity.findUnique.mockResolvedValue({
        id: signerDID,
        didDoc: JSON.stringify({
          id: signerDID,
          verificationMethod: [{ id: `${signerDID}#key-0`, type: 'Ed25519VerificationKey2020' }],
        }),
      });
      await expect(service.signMdoc(signerDID, docType, {})).rejects.toThrow(/JsonWebKey2020/);
    });

    it('throws InternalServerErrorException when the private key is missing from Vault', async () => {
      prisma.identity.findUnique.mockResolvedValue({
        id: signerDID,
        didDoc: JSON.stringify({
          id: signerDID,
          verificationMethod: [{ id: vmId, type: 'JsonWebKey2020', publicKeyJwk }],
        }),
      });
      vault.readPvtKey.mockResolvedValue({});
      await expect(service.signMdoc(signerDID, docType, {})).rejects.toThrow(InternalServerErrorException);
    });
  });

  describe('verifyMdoc', () => {
    it('verifies a genuine signed mdoc round-trip', async () => {
      primeSigner();
      const namespaces = { 'org.iso.18013.5.1': { given_name: 'Alice', family_name: 'Doe' } };
      const mdoc = await service.signMdoc(signerDID, docType, namespaces);

      const result = await service.verifyMdoc(mdoc);
      expect(result.verified).toBe(true);
      expect(result.docType).toBe(docType);
      expect(result.claims?.['org.iso.18013.5.1']).toMatchObject({ given_name: 'Alice', family_name: 'Doe' });
    });

    it('returns verified:false for garbage input', async () => {
      const result = await service.verifyMdoc('not-a-valid-mdoc');
      expect(result.verified).toBe(false);
      expect(result.error).toBeDefined();
    });

    it('detects a tampered signature', async () => {
      primeSigner();
      const mdoc = await service.signMdoc(signerDID, docType, {
        'org.iso.18013.5.1': { given_name: 'Alice' },
      });
      const bytes = Buffer.from(mdoc, 'base64url');
      const flipIndex = Math.floor(bytes.length / 2);
      bytes[flipIndex] = bytes[flipIndex] ^ 0xff;
      const tampered = bytes.toString('base64url');

      const result = await service.verifyMdoc(tampered);
      expect(result.verified).toBe(false);
    });
  });
});